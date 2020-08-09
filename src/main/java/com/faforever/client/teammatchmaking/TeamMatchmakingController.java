package com.faforever.client.teammatchmaking;

import com.faforever.client.chat.CountryFlagService;
import com.faforever.client.chat.avatar.AvatarService;
import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.ShowLadderMapsEvent;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.teammatchmaking.Party.PartyMember;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.RatingUtil;
import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialog;
import com.jfoenix.controls.JFXListView;
import javafx.beans.Observable;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

import static javafx.beans.binding.Bindings.createBooleanBinding;
import static javafx.beans.binding.Bindings.createObjectBinding;
import static javafx.beans.binding.Bindings.createStringBinding;

@Component
@RequiredArgsConstructor
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class TeamMatchmakingController extends AbstractViewController<Node> {

  private final CountryFlagService countryFlagService;
  private final AvatarService avatarService;
  private final PlayerService playerService;
  private final I18n i18n;
  private final UiService uiService;
  private final TeamMatchmakingService teamMatchmakingService;
  private final EventBus eventBus;
  @FXML
  public JFXButton invitePlayerButton;

  @FXML
  public StackPane teamMatchmakingRoot;
  @FXML
  public JFXButton leavePartyButton;
  @FXML
  public JFXButton readyButton;
  @FXML
  public Label refreshingLabel;
  public ToggleButton uefButton;
  public ToggleButton cybranButton;
  public ToggleButton aeonButton;
  public ToggleButton seraphimButton;
  @FXML
  public ImageView avatarImageView;
  @FXML
  public ImageView countryImageView;
  @FXML
  public Label clanLabel;
  @FXML
  public Label usernameLabel;
  @FXML
  public Label teamRatingLabel;
  @FXML
  public Label gameCountLabel;
  public Label ladderRatingLabel;
  public HBox queueBox;
  public PartyMemberItemController controller;
  public FlowPane partyMemberPane;
  private Player player;

  @Override
  public void initialize() {

    player = playerService.getCurrentPlayer().get();
    countryImageView.imageProperty().bind(createObjectBinding(() -> StringUtils.isEmpty(player.getCountry()) ?
        countryFlagService.loadCountryFlag("").orElse(null) // loads earth flag
        : countryFlagService.loadCountryFlag(player.getCountry()).orElse(null), player.countryProperty()));
    avatarImageView.setImage(avatarService.loadAvatar("https://content.faforever.com/faf/avatars/ICE_Test.png"));
    clanLabel.visibleProperty().bind(player.clanProperty().isNotEmpty().and(player.clanProperty().isNotNull()));
    clanLabel.textProperty().bind(createStringBinding(() ->
        Strings.isNullOrEmpty(player.getClan()) ? "" : String.format("[%s]", player.getClan()), player.clanProperty()));
    usernameLabel.textProperty().bind(player.usernameProperty());
    teamRatingLabel.textProperty().bind(createStringBinding(() -> i18n.get("teammatchmaking.teamRating", RatingUtil.getRoundedGlobalRating(player)), player.globalRatingMeanProperty(), player.globalRatingDeviationProperty()));
    ladderRatingLabel.textProperty().bind(createStringBinding(() -> i18n.get("teammatchmaking.1v1Rating", RatingUtil.getLeaderboardRating(player)), player.leaderboardRatingMeanProperty(), player.leaderboardRatingDeviationProperty()));
    gameCountLabel.textProperty().bind(createStringBinding(() -> i18n.get("teammatchmaking.gameCount", player.getNumberOfGames()), player.numberOfGamesProperty()));

    teamMatchmakingService.getParty().getMembers().addListener((Observable o) -> {
      List<PartyMember> members = teamMatchmakingService.getParty().getMembers();
      partyMemberPane.getChildren().clear();
      members.iterator().forEachRemaining(member -> {
        PartyMemberItemController controller = uiService.loadFxml("theme/play/teammatchmaking/matchmaking_member_card.fxml");
        controller.setMember(member);
        partyMemberPane.getChildren().add(controller.getRoot());
      });
    });

    teamMatchmakingService.getMatchmakingQueues().addListener((Observable o) -> {
      List<MatchmakingQueue> queues = teamMatchmakingService.getMatchmakingQueues();
      queueBox.getChildren().clear();
      queues.iterator().forEachRemaining(queue -> {
        MatchmakingQueueItemController controller = uiService.loadFxml("theme/play/teammatchmaking/matchmaking_queue_card.fxml");
        controller.setQueue(queue);
        queueBox.getChildren().add(controller.getRoot());
      });
    });

    invitePlayerButton.managedProperty().bind(invitePlayerButton.visibleProperty());
    invitePlayerButton.visibleProperty().bind(createBooleanBinding(
        () -> teamMatchmakingService.getParty().getOwner().getId() == playerService.getCurrentPlayer().map(Player::getId).orElse(-1),
        teamMatchmakingService.getParty().ownerProperty(),
        playerService.currentPlayerProperty()
    ));
    leavePartyButton.disableProperty().bind(createBooleanBinding(() -> teamMatchmakingService.getParty().getMembers().size() <= 1, teamMatchmakingService.getParty().getMembers()));

    teamMatchmakingService.getParty().getMembers().addListener((Observable o) -> {
      if (isSelfReady()) {
        readyButton.setText(i18n.get("teammatchmaking.ready"));
      } else {
        readyButton.setText(i18n.get("teammatchmaking.notReady"));
      }

      refreshingLabel.setVisible(false);
    });
  }

  @Override
  public Node getRoot() {
    return teamMatchmakingRoot;
  }

  // TODO: use
  public void showMatchmakingMaps(ActionEvent actionEvent) {
    eventBus.post(new ShowLadderMapsEvent());//TODO show team matchmaking maps and not ladder maps
  }

  public void onInvitePlayerButtonClicked(ActionEvent actionEvent) {
    InvitePlayerController invitePlayerController = uiService.loadFxml("theme/play/teammatchmaking/matchmaking_invite_player.fxml");
    Pane root = invitePlayerController.getRoot();
    JFXDialog dialog = uiService.showInDialog(teamMatchmakingRoot, root, i18n.get("teammatchmaking.invitePlayer"));
  }

  public void onEnterQueueButtonClicked(ActionEvent actionEvent) {
    //TODO
  }

  public void onLeavePartyButtonClicked(ActionEvent actionEvent) {
    teamMatchmakingService.leaveParty();
  }

  public void onLeaveQueueButtonClicked(ActionEvent actionEvent) {
    //TODO
  }

  public void onReadyButtonClicked(ActionEvent actionEvent) {
    if (!isSelfReady()) {
      teamMatchmakingService.readyParty();
    } else {
      teamMatchmakingService.unreadyParty();
    }

    refreshingLabel.setVisible(true);
  }

  public boolean isSelfReady() {
    return teamMatchmakingService.getParty().getMembers().stream()
        .anyMatch(p -> p.getPlayer().getId() == playerService.getCurrentPlayer().map(Player::getId).orElse(-1)
            && p.isReady());
  }

  public void onFactionButtonClicked(ActionEvent actionEvent) {
    boolean[] factions = {
        aeonButton.isSelected(),
        cybranButton.isSelected(),
        uefButton.isSelected(),
        seraphimButton.isSelected()
    };

    teamMatchmakingService.setPartyFactions(factions);

    refreshingLabel.setVisible(true);
  }
}