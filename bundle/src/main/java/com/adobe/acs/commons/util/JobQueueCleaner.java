/*
 * Copyright 2017 Adobe.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.adobe.acs.commons.util;

import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.fam.ActionManagerFactory;
import com.adobe.acs.commons.fam.ControlledProcess;
import com.adobe.acs.commons.fam.DeferredActions;
import com.adobe.acs.commons.util.visitors.TreeFilteringItemVisitor;
import java.util.ArrayList;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.Session;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.Queue;

/**
 * Stops all running sling jobs and empties the queue entirely.
 */
public class JobQueueCleaner extends ControlledProcess {

    public static final String JOB_TYPE = "slingevent:Job";
    public static final String POLICY_NODE_NAME = "rep:policy";
    public static final String EVENT_QUEUE_LOCATION = "/var/eventing";
    public static final int MIN_PURGE_FOLDER_LEVEL = 3;
    private final List<String> suspendedQueues = new ArrayList<>();
    private final JobManager jobManager;

    public JobQueueCleaner(ActionManagerFactory amf, JobManager jm, String name) {
        super(amf, name);
        this.jobManager = jm;
    }

    @Override
    public void buildProcess(ResourceResolver rr) throws LoginException {
        defineCriticalAction("Stop job queues", rr, this::stopJobQueues);
        defineAction("Purge jobs", rr, this::purgeJobs);
        defineCriticalAction("Resume job queues", rr, this::resumeJobQueues);
    }

    private void stopJobQueues(ActionManager manager) {
        for (Queue q : jobManager.getQueues()) {
            if (!q.isSuspended() || q.getStatistics().getNumberOfQueuedJobs() > 0) {
                suspendedQueues.add(q.getName());
                manager.deferredWithResolver(rr -> q.suspend());
            }
        }
    }

    private void purgeJobs(ActionManager manager) {
        TreeFilteringItemVisitor visitor = new TreeFilteringItemVisitor();
        visitor.setBreadthFirst(false);
        visitor.onLeaveNode((node, level) -> {
            if (level >= MIN_PURGE_FOLDER_LEVEL) {
                manager.deferredWithResolver(DeferredActions.retry(10, 100, rr -> deleteResource(rr, node.getPath())));
            }
        });
        visitor.onVisitChild((node, level) -> {
            if (!node.getName().equals(POLICY_NODE_NAME)) {
                manager.deferredWithResolver(rr -> deleteResource(rr, node.getPath()));
            }
        });
        manager.deferredWithResolver(rr -> {
            Session session = rr.adaptTo(Session.class);
            Node queue = session.getNode(EVENT_QUEUE_LOCATION);
            queue.accept(visitor);
        });
    }

    private void deleteResource(ResourceResolver rr, String path) throws PersistenceException {
        ActionManager.setCurrentItem(path);
        Resource r = rr.resolve(path);
        if (!r.isResourceType(Resource.RESOURCE_TYPE_NON_EXISTING)) {
            rr.delete(r);
        }
    }

    private void resumeJobQueues(ActionManager manager) {
        for (Queue q : jobManager.getQueues()) {
            if (suspendedQueues.contains(q.getName())) {
                manager.deferredWithResolver(rr -> q.resume());
            }
        }
    }
}