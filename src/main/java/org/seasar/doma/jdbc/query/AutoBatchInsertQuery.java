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

import static org.seasar.doma.internal.util.AssertionUtil.assertEquals;
import static org.seasar.doma.internal.util.AssertionUtil.assertNotNull;

import java.lang.reflect.Method;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.ListIterator;

import org.seasar.doma.internal.jdbc.entity.AbstractPostInsertContext;
import org.seasar.doma.internal.jdbc.entity.AbstractPreInsertContext;
import org.seasar.doma.internal.jdbc.sql.PreparedSqlBuilder;
import org.seasar.doma.jdbc.Config;
import org.seasar.doma.jdbc.JdbcException;
import org.seasar.doma.jdbc.Naming;
import org.seasar.doma.jdbc.PreparedSql;
import org.seasar.doma.jdbc.SqlKind;
import org.seasar.doma.jdbc.dialect.Dialect;
import org.seasar.doma.jdbc.entity.EntityPropertyDesc;
import org.seasar.doma.jdbc.entity.EntityDesc;
import org.seasar.doma.jdbc.entity.GeneratedIdPropertyDesc;
import org.seasar.doma.jdbc.entity.Property;
import org.seasar.doma.jdbc.id.IdGenerationConfig;
import org.seasar.doma.jdbc.id.ReservedIdProvider;
import org.seasar.doma.message.Message;

/**
 * @author taedium
 * @param <ENTITY>
 *            エンティティ
 */
public class AutoBatchInsertQuery<ENTITY> extends AutoBatchModifyQuery<ENTITY>
        implements BatchInsertQuery {

    protected GeneratedIdPropertyDesc<ENTITY, ?, ?> generatedIdPropertyDesc;

    protected IdGenerationConfig idGenerationConfig;

    protected boolean batchSupported = true;

    public AutoBatchInsertQuery(EntityDesc<ENTITY> entityType) {
        super(entityType);
    }

    @Override
    public void prepare() {
        super.prepare();
        assertNotNull(method, entities, sqls);
        int size = entities.size();
        if (size == 0) {
            return;
        }
        executable = true;
        executionSkipCause = null;
        currentEntity = entities.get(0);
        preInsert();
        prepareIdAndVersionPropertyTypes();
        prepareOptions();
        prepareTargetPropertyTypes();
        prepareIdValue();
        prepareVersionValue();
        prepareSql();
        entities.set(0, currentEntity);
        for (ListIterator<ENTITY> it = entities.listIterator(1); it.hasNext();) {
            currentEntity = it.next();
            preInsert();
            prepareIdValue();
            prepareVersionValue();
            prepareSql();
            it.set(currentEntity);
        }
        currentEntity = null;
        assertEquals(entities.size(), sqls.size());
    }

    protected void preInsert() {
        AutoBatchPreInsertContext<ENTITY> context = new AutoBatchPreInsertContext<ENTITY>(
                entityDesc, method, config);
        entityDesc.preInsert(currentEntity, context);
        if (context.getNewEntity() != null) {
            currentEntity = context.getNewEntity();
        }
    }

    @Override
    protected void prepareIdAndVersionPropertyTypes() {
        super.prepareIdAndVersionPropertyTypes();
        generatedIdPropertyDesc = entityDesc.getGeneratedIdPropertyDesc();
        if (generatedIdPropertyDesc != null) {
            if (idGenerationConfig == null) {
                idGenerationConfig = new IdGenerationConfig(config, entityDesc,
                        new ReservedIdProvider(config, entityDesc,
                                entities.size()));
                generatedIdPropertyDesc
                        .validateGenerationStrategy(idGenerationConfig);
                autoGeneratedKeysSupported = generatedIdPropertyDesc
                        .isAutoGeneratedKeysSupported(idGenerationConfig);
                batchSupported = generatedIdPropertyDesc
                        .isBatchSupported(idGenerationConfig);
            }
        }
    }

    protected void prepareTargetPropertyTypes() {
        targetPropertyTypes = new ArrayList<>(entityDesc
                .getEntityPropertyDescs().size());
        for (EntityPropertyDesc<ENTITY, ?> propertyType : entityDesc
                .getEntityPropertyDescs()) {
            if (!propertyType.isInsertable()) {
                continue;
            }
            if (propertyType.isId()) {
                if (propertyType != generatedIdPropertyDesc
                        || generatedIdPropertyDesc
                                .isIncluded(idGenerationConfig)) {
                    targetPropertyTypes.add(propertyType);
                }
                if (generatedIdPropertyDesc == null) {
                    Property<ENTITY, ?> property = propertyType
                            .createProperty();
                    property.load(currentEntity);
                    if (property.getWrapper().get() == null) {
                        throw new JdbcException(Message.DOMA2020,
                                entityDesc.getName(), propertyType.getName());
                    }
                }
                continue;
            }
            if (!isTargetPropertyName(propertyType.getName())) {
                continue;
            }
            targetPropertyTypes.add(propertyType);
        }
    }

    protected void prepareIdValue() {
        if (generatedIdPropertyDesc != null && idGenerationConfig != null) {
            ENTITY newEntity = generatedIdPropertyDesc.preInsert(entityDesc,
                    currentEntity, idGenerationConfig);
            currentEntity = newEntity;
        }
    }

    protected void prepareVersionValue() {
        if (versionPropertyDesc != null) {
            currentEntity = versionPropertyDesc.setIfNecessary(entityDesc,
                    currentEntity, 1);
        }
    }

    protected void prepareSql() {
        Naming naming = config.getNaming();
        Dialect dialect = config.getDialect();
        PreparedSqlBuilder builder = new PreparedSqlBuilder(config,
                SqlKind.BATCH_INSERT, sqlLogType);
        builder.appendSql("insert into ");
        builder.appendSql(entityDesc.getQualifiedTableName(naming::apply,
                dialect::applyQuote));
        builder.appendSql(" (");
        for (EntityPropertyDesc<ENTITY, ?> p : targetPropertyTypes) {
            builder.appendSql(p.getColumnName(naming::apply,
                    dialect::applyQuote));
            builder.appendSql(", ");
        }
        builder.cutBackSql(2);
        builder.appendSql(") values (");
        for (EntityPropertyDesc<ENTITY, ?> propertyType : targetPropertyTypes) {
            Property<ENTITY, ?> property = propertyType.createProperty();
            property.load(currentEntity);
            builder.appendParameter(property.asInParameter());
            builder.appendSql(", ");
        }
        builder.cutBackSql(2);
        builder.appendSql(")");
        PreparedSql sql = builder.build(this::comment);
        sqls.add(sql);
    }

    @Override
    public boolean isBatchSupported() {
        return batchSupported;
    }

    @Override
    public void generateId(Statement statement, int index) {
        if (generatedIdPropertyDesc != null && idGenerationConfig != null) {
            ENTITY newEntity = generatedIdPropertyDesc.postInsert(entityDesc,
                    entities.get(index), idGenerationConfig, statement);
            entities.set(index, newEntity);
        }
    }

    @Override
    public void complete() {
        for (ListIterator<ENTITY> it = entities.listIterator(); it.hasNext();) {
            currentEntity = it.next();
            postInsert();
            it.set(currentEntity);
        }
    }

    protected void postInsert() {
        AutoBatchPostInsertContext<ENTITY> context = new AutoBatchPostInsertContext<ENTITY>(
                entityDesc, method, config);
        entityDesc.postInsert(currentEntity, context);
        if (context.getNewEntity() != null) {
            currentEntity = context.getNewEntity();
        }
    }

    protected static class AutoBatchPreInsertContext<E> extends
            AbstractPreInsertContext<E> {

        public AutoBatchPreInsertContext(EntityDesc<E> entityType,
                Method method, Config config) {
            super(entityType, method, config);
        }
    }

    protected static class AutoBatchPostInsertContext<E> extends
            AbstractPostInsertContext<E> {

        public AutoBatchPostInsertContext(EntityDesc<E> entityType,
                Method method, Config config) {
            super(entityType, method, config);
        }
    }
}
