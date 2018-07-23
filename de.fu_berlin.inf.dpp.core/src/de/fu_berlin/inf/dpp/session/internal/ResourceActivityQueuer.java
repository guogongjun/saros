package de.fu_berlin.inf.dpp.session.internal;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.fu_berlin.inf.dpp.activities.IActivity;
import de.fu_berlin.inf.dpp.activities.IResourceActivity;
import de.fu_berlin.inf.dpp.activities.SPath;
import de.fu_berlin.inf.dpp.session.IActivityQueuer;

/**
 * This Resource based Queue gets a {@code Set} of {@link SPath}s, which should
 * be queued and flushes them when the SPath entry was removed by
 * {@link #disableQueuing(SPath)}.
 * <p>
 * To keep it simple, the execution order of queued activities stays unchanged.
 * </p>
 * TODO Discard obsolete Editor Events
 */
public class ResourceActivityQueuer implements IActivityQueuer {

    private Set<SPath> queueRessources;
    private List<IActivity> queueActivities = new LinkedList<IActivity>();

    /**
     * Takes a {@code Set} of {@link SPath}s for Queuing.
     *
     * @param queueRessources
     */
    public ResourceActivityQueuer(Set<SPath> queueRessources) {
        ConcurrentHashMap<SPath, Boolean> map;
        map = new ConcurrentHashMap<SPath, Boolean>(queueRessources.size() * 2);
        this.queueRessources = Collections.newSetFromMap(map);
        this.queueRessources.addAll(queueRessources);
    }

    /**
     * Should be called after Resource is completely written to disk.
     *
     * @param resource
     */
    public void disableQueuing(SPath resource) {
        queueRessources.remove(resource);
    }

    @Override
    public List<IActivity> process(List<IActivity> activities) {
        if (queueRessources.isEmpty())
            return activities;

        final List<IActivity> activitiesToExecute = new LinkedList<IActivity>();

        /* check if queue activities are flushable */
        for (IActivity activity : queueActivities) {
            if (isAvailable(activity))
                activitiesToExecute.add(activity);
            else
                break;
        }

        /* check if new activities should be queued */
        for (IActivity activity : activities) {
            if (isAvailable(activity))
                activitiesToExecute.add(activity);
            else
                queueActivities.add(activity);
        }

        return activitiesToExecute;
    }

    private boolean isAvailable(IActivity activity) {
        if (activity instanceof IResourceActivity) {
            IResourceActivity resourceActivity = (IResourceActivity) activity;
            SPath path = resourceActivity.getPath();

            if (path != null && queueRessources.contains(path))
                return false;
        }
        return true;
    }
}
