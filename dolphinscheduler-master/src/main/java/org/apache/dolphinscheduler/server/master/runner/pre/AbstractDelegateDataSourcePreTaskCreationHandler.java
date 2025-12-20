package org.apache.dolphinscheduler.server.master.runner.pre;


import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.dao.entity.DataSource;
import org.apache.dolphinscheduler.dao.entity.TaskInstance;
import org.apache.dolphinscheduler.dao.mapper.DataSourceMapper;
import org.apache.dolphinscheduler.plugin.datasource.delegate.param.DelegateConnectionParam;
import org.apache.dolphinscheduler.plugin.task.api.model.Property;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractDelegateDataSourcePreTaskCreationHandler implements DelegateDataSourcePreTaskCreationHandler {

    private DataSourceMapper dataSourceMapper;

    @Autowired
    public void setDataSourceMapper(DataSourceMapper dataSourceMapper) {
        this.dataSourceMapper = dataSourceMapper;
    }

    @Override
    public void preExecute(TaskInstance taskInstance, Map<String, Property> propertyMap) {
        Map<String, Object> env = getGlobalParameters(propertyMap, taskInstance.getProcessInstance().getGlobalParams());
        String taskParamsStr = rebuildTaskParams(taskInstance, env);
        taskInstance.setTaskParams(taskParamsStr);
        taskInstance.getTaskDefine().setTaskParams(taskParamsStr);
    }

    protected abstract String rebuildTaskParams(TaskInstance taskInstance, Map<String, Object> env);

    protected Map<String, Object> getGlobalParameters(Map<String, Property> prepareParamsMap, String globalParams) {
        Map<String, Object> env = new HashMap<>();

        if (MapUtils.isNotEmpty(prepareParamsMap)) {
            prepareParamsMap.forEach((key, value) -> {
                env.put(key, value.getValue());
            });
        }

        if (StringUtils.isNotBlank(globalParams)) {
            TypeReference<List<Property>> pType = new TypeReference<List<Property>>() {};
            List<Property> globalParameters = JSONUtils.parseObject(globalParams, pType);
            if  (globalParameters != null) {
                for (Property globalParameter : globalParameters) {
                    env.put(globalParameter.getProp(), globalParameter.getValue());
                }
            }
        }

        return env;
    }

    protected DataSource getDelegateDatasource(Integer dsId, Map<String, Object> env) {
        DataSource delegateDataSource = this.dataSourceMapper.selectById(dsId);
        DelegateConnectionParam delegateConnectionParam = JSONUtils.parseObject(delegateDataSource.getConnectionParams(), DelegateConnectionParam.class);
        if (delegateConnectionParam == null) {
            throw new IllegalArgumentException("Datasource " + dsId + " no connection parameters.");
        }
        String dsName = delegateConnectionParam.getRealDatasource();
        if (delegateConnectionParam.getRealDatasource().matches("^\\$\\{.+\\}")) {
            dsName = resolveSpel(delegateConnectionParam.getRealDatasource(), env);
        }
        List<DataSource> dataSources =  this.dataSourceMapper.queryDataSourceByName(dsName);
        if (CollectionUtils.isEmpty(dataSources)) {
            throw new IllegalArgumentException("Delegate datasource " + dsName + " not found.");
        }
        return dataSources.get(0);
    }

    protected String resolveSpel(String expressionTpl, Map<String, Object> env) {
        TemplateParserContext parserContext = new TemplateParserContext("${", "}");

        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext(env);
        context.addPropertyAccessor(new MapAccessor());
        // var context
        Expression expression = parser.parseExpression(expressionTpl, parserContext);
        return expression.getValue(context, String.class);
    }
}
