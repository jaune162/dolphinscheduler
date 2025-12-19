package org.apache.dolphinscheduler.server.master.runner.pre;

import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.dao.entity.DataSource;
import org.apache.dolphinscheduler.dao.entity.TaskInstance;
import org.apache.dolphinscheduler.plugin.task.api.parameters.SqlParameters;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

@Component
public class SqlTaskDelegateDataSourcePreTaskCreationHandler extends AbstractDelegateDataSourcePreTaskCreationHandler {

    @Override
    public boolean isSupport(TaskInstance taskInstance) {
        return Objects.equals(taskInstance.getTaskType(), "SQL");
    }

    @Override
    protected String rebuildTaskParams(TaskInstance taskInstance, Map<String, Object> env) {
        SqlParameters taskParams = JSONUtils.parseObject(taskInstance.getTaskParams(), SqlParameters.class);
        if (taskParams == null) {
            return taskInstance.getTaskParams();
        }
        if (Objects.equals(taskParams.getType(), "DELEGATE")) {
            DataSource dataSource = this.getDelegateDatasource(taskParams.getDatasource(), env);
            taskParams.setDatasource(dataSource.getId());
            taskParams.setType(dataSource.getType().name());
        }

        return JSONUtils.toJsonString(taskParams);
    }

}
