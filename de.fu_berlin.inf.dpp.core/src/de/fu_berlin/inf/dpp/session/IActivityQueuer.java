package de.fu_berlin.inf.dpp.session;

import java.util.List;

import de.fu_berlin.inf.dpp.activities.IActivity;
import de.fu_berlin.inf.dpp.activities.IResourceActivity;

public interface IActivityQueuer {

    /**
     * Processes the incoming {@linkplain IActivity activities} and decides
     * which {@linkplain IResourceActivity resource related activities} should
     * be queued. The method returns all other activities.
     * <p>
     * If flushing of the queue was previously requested, than this method will
     * additionally return activities from the queue.
     * 
     * @param activities
     * @return activities that are not queued
     */
    public List<IActivity> process(final List<IActivity> activities);
}