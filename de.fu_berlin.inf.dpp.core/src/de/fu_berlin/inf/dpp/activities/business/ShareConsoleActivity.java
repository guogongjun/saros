package de.fu_berlin.inf.dpp.activities.business;

import de.fu_berlin.inf.dpp.activities.serializable.IActivityDataObject;
import de.fu_berlin.inf.dpp.activities.serializable.ShareConsoleActivityDataObject;
import de.fu_berlin.inf.dpp.session.User;

/**
 * A ShareConsoleActivity is used to transmit text from a user to all other
 * participants of the running session.
 */
public class ShareConsoleActivity extends AbstractActivity {

    private final String consoleContent;

    public ShareConsoleActivity(User source, String content) {
        super(source);

        this.consoleContent = content;
    }

    @Override
    public void dispatch(IActivityReceiver receiver) {
        receiver.receive(this);
    }

    @Override
    public IActivityDataObject getActivityDataObject() {
        return new ShareConsoleActivityDataObject(getSource(), consoleContent);
    }

    public String getConsoleContent() {
        return consoleContent;
    }

}
