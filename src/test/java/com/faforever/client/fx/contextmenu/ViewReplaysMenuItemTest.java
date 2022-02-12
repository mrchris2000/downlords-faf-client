package com.faforever.client.fx.contextmenu;

import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.ShowUserReplaysEvent;
import com.faforever.client.player.SocialStatus;
import com.faforever.client.test.UITest;
import com.google.common.eventbus.EventBus;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;

public class ViewReplaysMenuItemTest extends UITest {

  @Mock
  private I18n i18n;
  @Mock
  private EventBus eventBus;

  @InjectMocks
  private ViewReplaysMenuItem instance;

  @Test
  public void testViewReplays() {
    instance.setObject(PlayerBeanBuilder.create().defaultValues().get());
    instance.onClicked();
    verify(eventBus).post(any(ShowUserReplaysEvent.class));
  }

  @Test
  public void testVisibleItem() {
    instance.setObject(PlayerBeanBuilder.create().defaultValues().socialStatus(SocialStatus.OTHER).get());
    assertTrue(instance.isVisible());
  }

  @Test
  public void testInvisibleItemIfNoPlayer() {
    instance.setObject(null);
    assertFalse(instance.isVisible());
  }
}