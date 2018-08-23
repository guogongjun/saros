package de.fu_berlin.inf.dpp.server.console;

import de.fu_berlin.inf.dpp.net.xmpp.JID;
import de.fu_berlin.inf.dpp.session.ISarosSessionManager;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.log4j.Logger;

public class InviteCommand extends ConsoleCommand {
  private static final Logger LOG = Logger.getLogger(InviteCommand.class);
  private final ISarosSessionManager sessionManager;

  public InviteCommand(ISarosSessionManager sessionManager) {
    this.sessionManager = sessionManager;
  }

  @Override
  public String identifier() {
    return "invite";
  }

  @Override
  public String help() {
    return "invite <JID>... - Invite users to session";
  }

  @Override
  public void execute(String command, PrintStream out) {
    try {
      List<JID> jids =
          Arrays.asList(command.split(" "))
              .stream()
              .skip(1)
              .map(x -> new JID(x))
              .collect(Collectors.toList());
      sessionManager.invite(jids, "Invitation by server command");
      for (JID jid : jids) {
        sessionManager.startSharingProjects(jid);
      }
    } catch (Exception e) {
      LOG.error("Error inviting users", e);
    }
  }
}
