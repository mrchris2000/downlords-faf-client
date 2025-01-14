package com.faforever.client.login;

import com.faforever.client.builders.ClientConfigurationBuilder.OAuthEndpointBuilder;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.login.OAuthValuesReceiver.Values;
import com.faforever.client.test.ServiceTest;
import com.faforever.client.update.ClientConfiguration.OAuthEndpoint;
import com.faforever.client.user.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.verification.Timeout;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OAuthEndpointValuesReceiverTest extends ServiceTest {

  public static final URI REDIRECT_URI = URI.create("http://localhost");

  @InjectMocks
  private OAuthValuesReceiver instance;
  @Mock
  private I18n i18n;
  @Mock
  private PlatformService platformService;
  @Mock
  private UserService userService;
  @Spy
  private ClientProperties clientProperties = new ClientProperties();

  @Test
  void receiveValues() throws Exception {
    String title = "JUnit Login Success";
    String message = "JUnit Login Message";
    when(i18n.get("login.browser.success.title")).thenReturn(title);
    when(i18n.get("login.browser.success.message")).thenReturn(message);

    OAuthEndpoint oAuthEndpoint = OAuthEndpointBuilder.create().defaultValues().get();

    CompletableFuture<Values> future = instance.receiveValues(Optional.of(REDIRECT_URI), Optional.ofNullable(oAuthEndpoint));

    ArgumentCaptor<URI> captor = ArgumentCaptor.forClass(URI.class);

    verify(userService, timeout(2000)).getHydraUrl(captor.capture());

    UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(captor.getValue())
        .queryParam("code", "1234")
        .queryParam("state", "abcd");

    try (InputStream inputStream = uriBuilder.build().toUri().toURL().openStream()) {
      String response = new String(inputStream.readAllBytes());
      assertThat(response, containsString(title));
      assertThat(response, containsString(message));
    }

    Values values = future.get();
    assertThat(values.getCode(), is("1234"));
    assertThat(values.getState(), is("abcd"));
  }

  @Test
  void receiveValuesTwice() throws Exception {
    String title = "JUnit Login Success";
    String message = "JUnit Login Message";
    when(i18n.get("login.browser.success.title")).thenReturn(title);
    when(i18n.get("login.browser.success.message")).thenReturn(message);

    OAuthEndpoint oAuthEndpoint = OAuthEndpointBuilder.create().defaultValues().get();

    CompletableFuture<Values> future1 = instance.receiveValues(Optional.of(REDIRECT_URI), Optional.ofNullable(oAuthEndpoint));

    CompletableFuture<Values> future2 = instance.receiveValues(Optional.of(REDIRECT_URI), Optional.ofNullable(oAuthEndpoint));

    assertEquals(future1, future2);

    ArgumentCaptor<URI> captor = ArgumentCaptor.forClass(URI.class);

    verify(userService, new Timeout(2000, times(2))).getHydraUrl(captor.capture());

    UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(captor.getValue())
        .queryParam("code", "1234")
        .queryParam("state", "abcd");

    try (InputStream inputStream = uriBuilder.build().toUri().toURL().openStream()) {
      String response = new String(inputStream.readAllBytes());
      assertThat(response, containsString(title));
      assertThat(response, containsString(message));
    }

    Values values = future1.get();
    assertThat(values.getCode(), is("1234"));
    assertThat(values.getState(), is("abcd"));
  }

  @Test
  void receiveError() throws Exception {
    String title = "JUnit Login Failure";
    String message = "JUnit Login Message";
    when(i18n.get("login.browser.failed.title")).thenReturn(title);
    when(i18n.get("login.browser.failed.message")).thenReturn(message);
    OAuthEndpoint oAuthEndpoint = OAuthEndpointBuilder.create().defaultValues().get();

    CompletableFuture<Values> future = instance.receiveValues(Optional.of(REDIRECT_URI), Optional.ofNullable(oAuthEndpoint));

    ArgumentCaptor<URI> captor = ArgumentCaptor.forClass(URI.class);

    verify(userService, timeout(2000)).getHydraUrl(captor.capture());

    UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(captor.getValue())
        .queryParam("error", "failed");

    try (InputStream inputStream = uriBuilder.build().toUri().toURL().openStream()) {
      String response = new String(inputStream.readAllBytes());
      assertThat(response, containsString(title));
      assertThat(response, containsString(message));
    }

    Exception throwable = assertThrows(ExecutionException.class, future::get);
    assertTrue(throwable.getCause() instanceof IllegalStateException);
  }

  @Test
  void receiveValuesNoPortsAvailable() throws Exception {
    ServerSocket serverSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
    URI takenPort = URI.create("http://localhost:" + serverSocket.getLocalPort());
    CompletableFuture<Values> future = instance.receiveValues(Optional.of(takenPort), Optional.empty());
    Exception throwable = assertThrows(ExecutionException.class, future::get);
    assertTrue(throwable.getCause() instanceof IllegalStateException);
  }
}