package com.faforever.client.teammatchmaking;

import com.faforever.client.chat.ChatMessage;
import com.faforever.client.chat.CountryFlagService;
import com.faforever.client.chat.MatchmakingChatController;
import com.faforever.client.chat.avatar.AvatarService;
import com.faforever.client.chat.event.ChatMessageEvent;
import com.faforever.client.game.GameBuilder;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerBuilder;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesBuilder;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.teammatchmaking.PartyBuilder.PartyMemberBuilder;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import com.faforever.commons.api.dto.Faction;
import com.google.common.eventbus.EventBus;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Tab;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.util.List;

import static com.faforever.client.teammatchmaking.PartyMemberItemController.LEADER_PSEUDO_CLASS;
import static com.faforever.client.teammatchmaking.TeamMatchmakingController.CHAT_AT_BOTTOM_PSEUDO_CLASS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TeamMatchmakingControllerTest extends UITest {

  @Mock
  private CountryFlagService countryFlagService;
  @Mock
  private AvatarService avatarService;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private PlayerService playerService;
  @Mock
  private I18n i18n;
  @Mock
  private UiService uiService;
  @Mock
  private TeamMatchmakingService teamMatchmakingService;
  @Mock
  private EventBus eventBus;

  private Preferences preferences;
  private TeamMatchmakingController instance;
  private Player player;
  private Party party;
  private ObservableList<MatchmakingQueue> matchmakingQueues;

  @BeforeEach
  public void setUp() throws Exception {
    party = PartyBuilder.create().defaultValues().get();
    player = party.getOwner();
    matchmakingQueues = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
    ObservableList<Faction> factionList = FXCollections.observableArrayList(Faction.SERAPHIM, Faction.AEON);
    preferences = PreferencesBuilder.create().defaultValues()
        .matchmakerPrefs()
        .factions(factionList)
        .then()
        .get();

    when(teamMatchmakingService.getParty()).thenReturn(party);
    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(i18n.get(anyString(), any(Object.class))).thenReturn("");
    when(teamMatchmakingService.currentlyInQueueProperty()).thenReturn(new SimpleBooleanProperty(false));
    when(teamMatchmakingService.partyMembersNotReadyProperty()).thenReturn(new ReadOnlyBooleanWrapper());
    when(teamMatchmakingService.partyMembersNotReady()).thenReturn(false);
    when(teamMatchmakingService.getMatchmakingQueues()).thenReturn(matchmakingQueues);
    when(playerService.getCurrentPlayer()).thenReturn(player);
    when(i18n.get(anyString())).thenReturn("");
    when(uiService.loadFxml("theme/play/teammatchmaking/matchmaking_chat.fxml")).thenAnswer(invocation -> {
      MatchmakingChatController controller = mock(MatchmakingChatController.class);
      when(controller.getRoot()).thenReturn(new Tab());
      return controller;
    });
    instance = new TeamMatchmakingController(countryFlagService, avatarService, preferencesService, playerService, i18n, uiService,
        teamMatchmakingService, eventBus);
    loadFxml("theme/play/team_matchmaking.fxml", clazz -> instance);
  }

  @Test
  public void testPostConstructSelectsPreviousFactions() {
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Faction>> captor = ArgumentCaptor.forClass(List.class);

    assertThat(instance.aeonButton.isSelected(), is(true));
    assertThat(instance.seraphimButton.isSelected(), is(true));
    assertThat(instance.uefButton.isSelected(), is(false));
    assertThat(instance.cybranButton.isSelected(), is(false));
    verify(teamMatchmakingService).sendFactionSelection(captor.capture());
    assertThat(captor.getValue(), containsInAnyOrder(Faction.SERAPHIM, Faction.AEON));
  }

  @Test
  public void testOnInvitePlayerButtonClicked() {
    when(uiService.loadFxml("theme/play/teammatchmaking/matchmaking_invite_player.fxml")).thenAnswer(invocation -> {
      InvitePlayerController controller = mock(InvitePlayerController.class);
      return controller;
    });

    instance.onInvitePlayerButtonClicked();
    WaitForAsyncUtils.waitForFxEvents();

    verify(uiService).showInDialog(instance.teamMatchmakingRoot, null, "");
  }

  @Test
  public void testOnLeavePartyButtonClicked() {
    instance.onLeavePartyButtonClicked();

    verify(teamMatchmakingService).leaveParty();
  }

  @Test
  public void testOnFactionButtonClicked() {
    instance.uefButton.setSelected(true);
    instance.aeonButton.setSelected(true);
    instance.cybranButton.setSelected(false);
    instance.seraphimButton.setSelected(false);

    instance.onFactionButtonClicked();

    assertThat(preferences.getMatchmaker().getFactions(), containsInAnyOrder(Faction.UEF, Faction.AEON));
    verify(teamMatchmakingService, times(2)).sendFactionSelection(eq(List.of(Faction.UEF, Faction.AEON)));
    verify(preferencesService).storeInBackground();
  }

  @Test
  public void testOnFactionButtonClickedWhileNoFactionsSelected() {
    instance.uefButton.setSelected(false);
    instance.aeonButton.setSelected(false);
    instance.cybranButton.setSelected(false);
    instance.seraphimButton.setSelected(false);

    instance.onFactionButtonClicked();

    verify(preferencesService, never()).storeInBackground();
    assertThat(instance.uefButton.isSelected(), is(true));
    assertThat(instance.aeonButton.isSelected(), is(true));
    assertThat(instance.cybranButton.isSelected(), is(true));
    assertThat(instance.seraphimButton.isSelected(), is(true));
  }

  @Test
  public void testOnPartyChatMessage() {
    ChatMessage message = mock(ChatMessage.class);
    when(message.getSource()).thenReturn("#tester'sParty");
    when(instance.matchmakingChatController.getReceiver()).thenReturn("#tester'sParty");

    instance.onChatMessage(new ChatMessageEvent(message));
    WaitForAsyncUtils.waitForFxEvents();

    verify(instance.matchmakingChatController).onChatMessage(message);
  }

  @Test
  public void testOnOtherChatMessage() {
    ChatMessage message = mock(ChatMessage.class);
    when(message.getSource()).thenReturn("#other'sParty");
    when(instance.matchmakingChatController.getReceiver()).thenReturn("#tester'sParty");

    instance.onChatMessage(new ChatMessageEvent(message));
    WaitForAsyncUtils.waitForFxEvents();

    verify(instance.matchmakingChatController, never()).onChatMessage(message);
  }

  @Test
  public void testQueueHeadingListener() {
    verify(i18n, times(2)).get("teammatchmaking.queueTitle");

    runOnFxThreadAndWait(() -> player.setGame(GameBuilder.create().defaultValues().get()));
    verify(i18n).get("teammatchmaking.queueTitle.inGame");

    when(teamMatchmakingService.partyMembersNotReady()).thenReturn(true);
    runOnFxThreadAndWait(() -> player.setGame(null));
    verify(i18n).get("teammatchmaking.queueTitle.memberInGame");

    runOnFxThreadAndWait(() -> party.setOwner(PlayerBuilder.create("test").defaultValues().id(100).get()));
    verify(i18n).get("teammatchmaking.queueTitle.inParty");

    when(teamMatchmakingService.isCurrentlyInQueue()).thenReturn(true);
    runOnFxThreadAndWait(() -> party.setOwner(player));
    verify(i18n).get("teammatchmaking.queueTitle.inParty");
  }

  @Test
  public void testOnQueuesAdded() {
    when(uiService.loadFxml("theme/play/teammatchmaking/matchmaking_queue_card.fxml")).thenAnswer(invocation -> {
      MatchmakingQueueItemController controller = mock(MatchmakingQueueItemController.class);
      when(controller.getRoot()).thenReturn(new VBox());
      return controller;
    });

    matchmakingQueues.add(MatchmakingQueueBuilder.create().defaultValues().queueId(1).get());
    matchmakingQueues.add(MatchmakingQueueBuilder.create().defaultValues().queueId(2).get());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.queuePane.getChildren().size(), is(2));

    matchmakingQueues.add(MatchmakingQueueBuilder.create().defaultValues().queueId(3).get());
    matchmakingQueues.add(MatchmakingQueueBuilder.create().defaultValues().queueId(4).get());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.queuePane.getChildren().size(), is(4));
  }

  @Test
  public void testOnPartyMembersAdded() {
    when(uiService.loadFxml("theme/play/teammatchmaking/matchmaking_member_card.fxml")).thenAnswer(invocation -> {
      PartyMemberItemController controller = mock(PartyMemberItemController.class);
      when(controller.getRoot()).thenReturn(new AnchorPane());
      return controller;
    });

    assertFalse(instance.playerCard.getPseudoClassStates().contains(LEADER_PSEUDO_CLASS));

    Player player1 = PlayerBuilder.create("m1").defaultValues().id(101).get();
    Player player2 = PlayerBuilder.create("m2").defaultValues().id(102).get();
    party.getMembers().add(PartyMemberBuilder.create(player1).defaultValues().get());
    party.getMembers().add(PartyMemberBuilder.create(player2).defaultValues().get());
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.playerCard.getPseudoClassStates().contains(LEADER_PSEUDO_CLASS));
    assertThat(instance.partyMemberPane.getChildren().size(), is(2));

    Player player3 = PlayerBuilder.create("m3").defaultValues().id(103).get();
    party.getMembers().add(PartyMemberBuilder.create(player3).defaultValues().get());
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.partyMemberPane.getChildren().size(), is(3));

    party.getMembers().remove(3);
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.partyMemberPane.getChildren().size(), is(2));
  }

  @Test
  public void testGetRoot() {
    assertThat(instance.getRoot(), is(instance.teamMatchmakingRoot));
  }

  @Test
  public void testDynamicChatListener() {
    runOnFxThreadAndWait(() -> instance.contentPane.resize(2000, 1000));
    assertFalse(instance.chatTabPane.getPseudoClassStates().contains(CHAT_AT_BOTTOM_PSEUDO_CLASS));

    runOnFxThreadAndWait(() -> instance.contentPane.resize(1000, 1000));
    assertTrue(instance.chatTabPane.getPseudoClassStates().contains(CHAT_AT_BOTTOM_PSEUDO_CLASS));
  }
}
