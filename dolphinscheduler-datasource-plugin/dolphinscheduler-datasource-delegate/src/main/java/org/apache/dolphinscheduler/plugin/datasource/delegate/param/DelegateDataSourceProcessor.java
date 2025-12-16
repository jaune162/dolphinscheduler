/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dolphinscheduler.plugin.datasource.delegate.param;

import org.apache.dolphinscheduler.common.constants.Constants;
import org.apache.dolphinscheduler.common.constants.DataSourceConstants;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.dao.entity.DataSource;
import org.apache.dolphinscheduler.dao.mapper.DataSourceMapper;
import org.apache.dolphinscheduler.plugin.datasource.api.datasource.AbstractDataSourceProcessor;
import org.apache.dolphinscheduler.plugin.datasource.api.datasource.BaseDataSourceParamDTO;
import org.apache.dolphinscheduler.plugin.datasource.api.datasource.DataSourceProcessor;
import org.apache.dolphinscheduler.plugin.datasource.api.plugin.DataSourceProcessorProvider;
import org.apache.dolphinscheduler.plugin.datasource.api.utils.DataSourceUtils;
import org.apache.dolphinscheduler.plugin.datasource.api.utils.PasswordUtils;
import org.apache.dolphinscheduler.spi.datasource.BaseConnectionParam;
import org.apache.dolphinscheduler.spi.datasource.ConnectionParam;
import org.apache.dolphinscheduler.spi.enums.DbType;

import org.apache.commons.collections4.MapUtils;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import lombok.extern.slf4j.Slf4j;

import com.alibaba.druid.sql.parser.SQLParserUtils;
import com.google.auto.service.AutoService;

@AutoService(DataSourceProcessor.class)
@Slf4j
public class DelegateDataSourceProcessor extends AbstractDataSourceProcessor {

    DataSourceMapper dataSourceMapper;
    BaseConnectionParam realConnectionParam;
    private DataSourceProcessor dataSourceProcessor;

    @Override
    public BaseDataSourceParamDTO castDatasourceParamDTO(String paramJson) {
        return JSONUtils.parseObject(paramJson, DelegateDataSourceParamDTO.class);
    }

    @Override
    public BaseDataSourceParamDTO createDatasourceParamDTO(String connectionJson) {
        DelegateConnectionParam connectionParams = (DelegateConnectionParam) createConnectionParams(connectionJson);
        DelegateDataSourceParamDTO datasourceParamDTO = new DelegateDataSourceParamDTO();

        datasourceParamDTO.setRealDatasource(connectionParams.getRealDatasource());

        return datasourceParamDTO;
    }

    @Override
    public BaseConnectionParam createConnectionParams(BaseDataSourceParamDTO dataSourceParam) {
        DelegateDataSourceParamDTO datasourceParam = (DelegateDataSourceParamDTO) dataSourceParam;
        String realDatasource = datasourceParam.getRealDatasource();
        BaseConnectionParam connectionParam = toRealConnectionParam(realDatasource);
        return connectionParam;
    }

    private BaseConnectionParam toRealConnectionParam(String realDatasource) {
        if (realConnectionParam != null) {
            return realConnectionParam;
        }
        if (realDatasource.matches("^\\$\\{.+\\}")) {
            //resolve var
        }
        List<DataSource> dataSources = dataSourceMapper.queryDataSourceByName(realDatasource);
        if (dataSources == null || dataSources.isEmpty()) {
            throw new RuntimeException("datasource not found:" + realDatasource);
        } else if (dataSources.size() > 1) {
            throw new RuntimeException("need one datasource for:" + realDatasource + ", while got:" + dataSources);
        }
        DataSource dataSource = dataSources.get(0);

        BaseConnectionParam connectionParam = (BaseConnectionParam) DataSourceUtils.buildConnectionParams(dataSource.getType(),
                dataSource.getConnectionParams());
        dataSourceProcessor = DataSourceProcessorProvider.getDataSourceProcessor(dataSource.getType());
        return realConnectionParam = connectionParam;
    }

    @Override
    public ConnectionParam createConnectionParams(String connectionJson) {
        DelegateConnectionParam delegateConnectionParam = JSONUtils.parseObject(connectionJson, DelegateConnectionParam.class);
        return toRealConnectionParam(delegateConnectionParam.getRealDatasource());
    }

    @Override
    public String getDatasourceDriver() {
        return "";
    }

    @Override
    public String getValidationQuery() {
        return null;
    }

    @Override
    public String getJdbcUrl(ConnectionParam connectionParam) {
        DelegateConnectionParam connectionParam1 = (DelegateConnectionParam) connectionParam;
        BaseConnectionParam realConnectionParam = toRealConnectionParam(connectionParam1.getRealDatasource());
//        createConnectionParams()
        return realConnectionParam.getJdbcUrl();
    }

    @Override
    public Connection getConnection(ConnectionParam connectionParam) throws ClassNotFoundException, SQLException, IOException {
        DelegateConnectionParam delegateConnectionParam = (DelegateConnectionParam) connectionParam;
        return dataSourceProcessor.getConnection(toRealConnectionParam(delegateConnectionParam.getRealDatasource()));
    }

    @Override
    public DbType getDbType() {
        return DbType.DELEGATE;
    }

    @Override
    public DataSourceProcessor create() {
        return new DelegateDataSourceProcessor();
    }

    @Override
    public List<String> splitAndRemoveComment(String sql) {
        return dataSourceProcessor.splitAndRemoveComment(sql);
    }

}
