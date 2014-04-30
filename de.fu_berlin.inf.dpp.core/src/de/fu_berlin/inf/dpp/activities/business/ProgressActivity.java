package de.fu_berlin.inf.dpp.activities.business;

import org.apache.commons.lang.ObjectUtils;

import de.fu_berlin.inf.dpp.activities.serializable.IActivityDataObject;
import de.fu_berlin.inf.dpp.activities.serializable.ProgressActivityDataObject;
import de.fu_berlin.inf.dpp.session.User;

/**
 * A {@link ProgressActivity} is used for controlling a progress bar at a remote
 * peer.
 * 
 */
public class ProgressActivity extends AbstractActivity implements
    ITargetedActivity {

    protected User target;
    protected String progressID;
    protected int workCurrent;
    protected int workTotal;
    protected String taskName;
    protected ProgressAction action;

    public enum ProgressAction {
        BEGINTASK, SUBTASK, SETTASKNAME, UPDATE, DONE, CANCEL;
    }

    public ProgressActivity(User source, User target, String progressID,
        int workCurrent, int workTotal, String taskName, ProgressAction action) {

        super(source);

        if (target == null)
            throw new IllegalArgumentException("target must not be null");

        this.target = target;
        this.progressID = progressID;
        this.workCurrent = workCurrent;
        this.workTotal = workTotal;
        this.taskName = taskName;
        this.action = action;
    }

    @Override
    public String toString() {
        return "ProgressActivity(source: " + getSource() + ", target: "
            + target + ", id: " + progressID + ", work: " + workCurrent + "/"
            + workTotal + ", task: " + taskName + ", action: " + action + ")";
    }

    @Override
    public IActivityDataObject getActivityDataObject() {

        return new ProgressActivityDataObject(getSource(), target, progressID,
            workCurrent, workTotal, taskName, action);
    }

    @Override
    public void dispatch(IActivityReceiver receiver) {
        receiver.receive(this);
    }

    /**
     * The unique ID by which local users and remote peers can identify the
     * progress process.
     */
    public String getProgressID() {
        return progressID;
    }

    /**
     * The current position the progress bar should be shown at (in relation to
     * {@link #getWorkTotal()}.
     * 
     * For instance if {@link #workCurrent} == 3 and {@link #workTotal} == 10,
     * then the progress bar should be shown at 30% of the total width of the
     * progress dialog.
     */
    public int getWorkCurrent() {
        return workCurrent;
    }

    /**
     * The total amount of work associated with this progress process.
     */
    public int getWorkTotal() {
        return workTotal;
    }

    /**
     * A human readable description of the work currently being done as part of
     * the process about which this ProgressActivity is informing about.
     * 
     * @return a new human readable description of the work currently being done
     *         or null if the task did not change.
     */
    public String getTaskName() {
        return taskName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ObjectUtils.hashCode(action);
        result = prime * result + ObjectUtils.hashCode(progressID);
        result = prime * result + ObjectUtils.hashCode(taskName);
        result = prime * result + ObjectUtils.hashCode(target);
        result = prime * result + workCurrent;
        result = prime * result + workTotal;

        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (!(obj instanceof ProgressActivity))
            return false;

        ProgressActivity other = (ProgressActivity) obj;

        if (this.workCurrent != other.workCurrent)
            return false;
        if (this.workTotal != other.workTotal)
            return false;
        if (this.action != other.action)
            return false;
        if (!ObjectUtils.equals(this.progressID, other.progressID))
            return false;
        if (!ObjectUtils.equals(this.taskName, other.taskName))
            return false;
        if (!ObjectUtils.equals(this.target, other.target))
            return false;

        return true;
    }

    /**
     * The action describes whether the associated progress bar at the remote
     * site should be updated or closed.
     * 
     * Once a remote progress has been closed the peer might discard all
     * updates.
     */
    public ProgressAction getAction() {
        return action;
    }

    @Override
    public User getTarget() {
        return target;
    }
}
