/*
 * Copyright 2004-2010 the Seasar Foundation and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.seasar.doma.jdbc.query;

import static org.seasar.doma.internal.util.AssertionUtil.assertNotNull;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.seasar.doma.FetchType;
import org.seasar.doma.internal.expr.ExpressionEvaluator;
import org.seasar.doma.internal.expr.Value;
import org.seasar.doma.internal.jdbc.command.BasicSingleResultHandler;
import org.seasar.doma.internal.jdbc.sql.NodePreparedSqlBuilder;
import org.seasar.doma.internal.jdbc.sql.PreparedSql;
import org.seasar.doma.internal.jdbc.sql.node.ExpandNode;
import org.seasar.doma.internal.jdbc.sql.node.SqlLocation;
import org.seasar.doma.jdbc.Config;
import org.seasar.doma.jdbc.JdbcException;
import org.seasar.doma.jdbc.SelectOptions;
import org.seasar.doma.jdbc.SelectOptionsAccessor;
import org.seasar.doma.jdbc.SqlKind;
import org.seasar.doma.jdbc.SqlNode;
import org.seasar.doma.jdbc.command.SelectCommand;
import org.seasar.doma.jdbc.dialect.Dialect;
import org.seasar.doma.jdbc.entity.EntityType;
import org.seasar.doma.message.Message;
import org.seasar.doma.wrapper.LongWrapper;

/**
 * @author taedium
 * 
 */
public class SqlSelectQuery implements SelectQuery {

    protected Config config;

    protected final Map<String, Value> parameters = new HashMap<String, Value>();

    protected String callerClassName;

    protected String callerMethodName;

    protected SqlNode sqlNode;

    protected PreparedSql sql;

    protected SelectOptions options = SelectOptions.get();

    protected boolean resultEnsured;

    protected boolean resultMappingEnsured;

    protected FetchType fetchType;

    protected int fetchSize;

    protected int maxRows;

    protected int queryTimeout;

    protected Method method;

    protected EntityType<?> entityType;

    @Override
    public void prepare() {
        assertNotNull(config, sqlNode, callerClassName, callerMethodName);
        prepareOptions();
        prepareSql();
        assertNotNull(sql);
    }

    protected void prepareOptions() {
        if (fetchSize <= 0) {
            fetchSize = config.getFetchSize();
        }
        if (maxRows <= 0) {
            maxRows = config.getMaxRows();
        }
        if (queryTimeout <= 0) {
            queryTimeout = config.getQueryTimeout();
        }
    }

    protected void prepareSql() {
        SqlNode transformedSqlNode = config.getDialect()
                .transformSelectSqlNode(sqlNode, options);
        ExpressionEvaluator evaluator = new ExpressionEvaluator(parameters,
                config.getDialect().getExpressionFunctions(),
                config.getClassHelper());
        NodePreparedSqlBuilder sqlBuilder = new NodePreparedSqlBuilder(config,
                SqlKind.SELECT, null, evaluator, this::expandColumns);
        sql = sqlBuilder.build(transformedSqlNode);
    }

    protected List<String> expandColumns(ExpandNode node) {
        if (entityType == null) {
            SqlLocation location = node.getLocation();
            throw new JdbcException(Message.DOMA2144, location.getSql(),
                    location.getLineNumber(), location.getPosition());
        }
        Dialect dialect = config.getDialect();
        return entityType.getEntityPropertyTypes().stream()
                .map(p -> p.getColumnName(dialect::applyQuote))
                .collect(Collectors.toList());
    }

    @Override
    public int getFetchSize() {
        return fetchSize;
    }

    @Override
    public int getMaxRows() {
        return maxRows;
    }

    @Override
    public SelectOptions getOptions() {
        return options;
    }

    @Override
    public PreparedSql getSql() {
        return sql;
    }

    @Override
    public boolean isResultEnsured() {
        return resultEnsured;
    }

    @Override
    public boolean isResultMappingEnsured() {
        return resultMappingEnsured;
    }

    @Override
    public FetchType getFetchType() {
        return fetchType;
    }

    @Override
    public void complete() {
        if (!SelectOptionsAccessor.isCount(options)) {
            return;
        }
        CountQuery query = new CountQuery();
        query.callerClassName = callerClassName;
        query.callerMethodName = callerMethodName;
        query.method = method;
        query.config = config;
        query.fetchSize = fetchSize;
        query.maxRows = maxRows;
        query.options = options;
        query.parameters = parameters;
        query.queryTimeout = queryTimeout;
        query.sqlNode = sqlNode;
        query.entityType = entityType;
        query.prepare();
        SelectCommand<Long> command = new SelectCommand<Long>(query,
                new BasicSingleResultHandler<Long>(() -> new LongWrapper(),
                        true));
        long count = command.execute();
        query.complete();
        SelectOptionsAccessor.setCountSize(options, count);
    }

    @Override
    public String getClassName() {
        return callerClassName;
    }

    @Override
    public Config getConfig() {
        return config;
    }

    @Override
    public String getMethodName() {
        return callerMethodName;
    }

    @Override
    public int getQueryTimeout() {
        return queryTimeout;
    }

    public void setOptions(SelectOptions options) {
        this.options = options;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public void setSqlNode(SqlNode sqlNode) {
        this.sqlNode = sqlNode;
    }

    public void addParameter(String name, Class<?> type, Object value) {
        assertNotNull(name, type);
        parameters.put(name, new Value(type, value));
    }

    public void setCallerClassName(String callerClassName) {
        this.callerClassName = callerClassName;
    }

    public void setCallerMethodName(String callerMethodName) {
        this.callerMethodName = callerMethodName;
    }

    public void setResultEnsured(boolean resultEnsured) {
        this.resultEnsured = resultEnsured;
    }

    public void setResultMappingEnsured(boolean resultMappingEnsured) {
        this.resultMappingEnsured = resultMappingEnsured;
    }

    public void setFetchType(FetchType fetchType) {
        this.fetchType = fetchType;
    }

    public void setFetchSize(int fetchSize) {
        this.fetchSize = fetchSize;
    }

    public void setMaxRows(int maxRows) {
        this.maxRows = maxRows;
    }

    public void setQueryTimeout(int queryTimeout) {
        this.queryTimeout = queryTimeout;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public void setEntityType(EntityType<?> entityType) {
        this.entityType = entityType;
    }

    @Override
    public String toString() {
        return sql != null ? sql.toString() : null;
    }
}