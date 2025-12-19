package org.apache.dolphinscheduler.server.master.runner.pre;

import org.apache.dolphinscheduler.dao.entity.TaskInstance;
import org.apache.dolphinscheduler.plugin.task.api.model.Property;

import java.util.Map;

public interface PreTaskCreationHandler {

    boolean isSupport(TaskInstance taskInstance);

    void preExecute(TaskInstance taskInstance, Map<String, Property> propertyMap);
}
