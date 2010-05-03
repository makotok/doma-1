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
package org.seasar.doma.internal.jdbc.query;

import static org.seasar.doma.internal.util.AssertionUtil.*;

import java.util.HashMap;
import java.util.Map;

import org.seasar.doma.internal.expr.ExpressionEvaluator;
import org.seasar.doma.internal.expr.Value;
import org.seasar.doma.internal.jdbc.sql.NodePreparedSqlBuilder;
import org.seasar.doma.internal.jdbc.sql.PreparedSql;
import org.seasar.doma.jdbc.Config;
import org.seasar.doma.jdbc.SqlExecutionSkipCause;
import org.seasar.doma.jdbc.SqlFile;
import org.seasar.doma.jdbc.SqlKind;

/**
 * @author taedium
 * 
 */
public abstract class SqlFileModifyQuery implements ModifyQuery {

    protected final SqlKind kind;

    protected Config config;

    protected String sqlFilePath;

    protected final Map<String, Value> parameters = new HashMap<String, Value>();

    protected String callerClassName;

    protected String callerMethodName;

    protected PreparedSql sql;

    protected int queryTimeout;

    protected SqlFileModifyQuery(SqlKind kind) {
        assertNotNull(kind);
        this.kind = kind;
    }

    protected void prepareOptions() {
        if (queryTimeout <= 0) {
            queryTimeout = config.getQueryTimeout();
        }
    }

    protected void prepareSql() {
        SqlFile sqlFile = config.getSqlFileRepository().getSqlFile(sqlFilePath,
                config.getDialect());
        ExpressionEvaluator evaluator = new ExpressionEvaluator(parameters,
                config.getDialect().getExpressionFunctions());
        NodePreparedSqlBuilder sqlBuilder = new NodePreparedSqlBuilder(config,
                kind, sqlFile.getPath(), evaluator);
        sql = sqlBuilder.build(sqlFile.getSqlNode());
    }

    @Override
    public void complete() {
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public void setSqlFilePath(String sqlFilePath) {
        this.sqlFilePath = sqlFilePath;
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

    public void setQueryTimeout(int queryTimeout) {
        this.queryTimeout = queryTimeout;
    }

    @Override
    public PreparedSql getSql() {
        return sql;
    }

    @Override
    public String getClassName() {
        return callerClassName;
    }

    @Override
    public String getMethodName() {
        return callerMethodName;
    }

    @Override
    public Config getConfig() {
        return config;
    }

    @Override
    public boolean isOptimisticLockCheckRequired() {
        return false;
    }

    @Override
    public boolean isExecutable() {
        return true;
    }

    @Override
    public SqlExecutionSkipCause getSqlExecutionSkipCause() {
        return null;
    }

    public int getQueryTimeout() {
        return queryTimeout;
    }

    @Override
    public boolean isAutoGeneratedKeysSupported() {
        return false;
    }

    @Override
    public String toString() {
        return sql != null ? sql.toString() : null;
    }

}
