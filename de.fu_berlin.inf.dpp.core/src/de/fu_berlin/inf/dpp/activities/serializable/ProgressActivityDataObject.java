package de.fu_berlin.inf.dpp.activities.serializable;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import de.fu_berlin.inf.dpp.activities.business.IActivity;
import de.fu_berlin.inf.dpp.activities.business.ProgressActivity;
import de.fu_berlin.inf.dpp.activities.business.ProgressActivity.ProgressAction;
import de.fu_berlin.inf.dpp.session.User;

/**
 * A {@link ProgressActivityDataObject} is used for communicating
 * {@link ProgressActivity}s to peers.
 */
@XStreamAlias("progressActivity")
public class ProgressActivityDataObject extends AbstractActivityDataObject {

    @XStreamAsAttribute
    protected String progressID;

    @XStreamAsAttribute
    protected int workCurrent;

    @XStreamAsAttribute
    protected int workTotal;

    protected String taskName;

    @XStreamAsAttribute
    protected ProgressAction action;

    @XStreamAsAttribute
    protected User target;

    public ProgressActivityDataObject(User source, User target,
        String progressID, int workCurrent, int workTotal, String taskName,
        ProgressAction action) {

        super(source);

        this.target = target;
        this.progressID = progressID;
        this.workCurrent = workCurrent;
        this.workTotal = workTotal;
        this.taskName = taskName;
        this.action = action;
    }

    @Override
    public String toString() {
        return "ProgressActivityDO(source: " + getSource() + ", target: "
            + target + ", id: " + progressID + ", work: " + workCurrent + "/"
            + workTotal + ", task: " + taskName + ", action: " + action + ")";
    }

    @Override
    public IActivity getActivity() {
        return new ProgressActivity(getSource(), target, progressID,
            workCurrent, workTotal, taskName, action);
    }
}
