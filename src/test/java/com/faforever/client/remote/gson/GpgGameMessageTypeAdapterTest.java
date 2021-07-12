package com.faforever.client.remote.gson;

import com.faforever.client.fa.relay.GpgClientCommand;
import com.faforever.client.test.ServiceTest;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GpgGameMessageTypeAdapterTest extends ServiceTest {

  private GpgClientMessageTypeAdapter instance;

  @BeforeEach
  public void setUp() throws Exception {
    instance = GpgClientMessageTypeAdapter.INSTANCE;
  }

  @Test
  public void testWrite() throws Exception {
    JsonWriter out = mock(JsonWriter.class);
    instance.write(out, GpgClientCommand.CONNECTED);

    verify(out).value(GpgClientCommand.CONNECTED.getString());
  }

  @Test
  public void testWriteNull() throws Exception {
    JsonWriter out = mock(JsonWriter.class);
    instance.write(out, null);

    verify(out).nullValue();
  }

  @Test
  public void testRead() throws Exception {
    JsonReader in = mock(JsonReader.class);
    when(in.nextString()).thenReturn(GpgClientCommand.CHAT.getString());

    GpgClientCommand gpgClientCommand = instance.read(in);

    assertEquals(GpgClientCommand.CHAT, gpgClientCommand);
  }

  public void testReadNullReturnsNull() throws Exception {
    JsonReader in = mock(JsonReader.class);
    when(in.nextString()).thenReturn(null);

    assertNull(instance.read(in));
  }

  public void testReadGibberishReturnsNull() throws Exception {
    JsonReader in = mock(JsonReader.class);
    when(in.nextString()).thenReturn("gibberish");

    assertNull(instance.read(in));
  }
}
