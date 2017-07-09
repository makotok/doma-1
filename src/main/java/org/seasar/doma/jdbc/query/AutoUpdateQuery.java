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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.seasar.doma.internal.jdbc.entity.AbstractPostUpdateContext;
import org.seasar.doma.internal.jdbc.entity.AbstractPreUpdateContext;
import org.seasar.doma.internal.jdbc.sql.PreparedSqlBuilder;
import org.seasar.doma.jdbc.Config;
import org.seasar.doma.jdbc.Naming;
import org.seasar.doma.jdbc.SqlKind;
import org.seasar.doma.jdbc.dialect.Dialect;
import org.seasar.doma.jdbc.entity.EntityPropertyDesc;
import org.seasar.doma.jdbc.entity.EntityDesc;
import org.seasar.doma.jdbc.entity.Property;

/**
 * @author taedium
 * @param <ENTITY>
 *            エンティティ
 */
public class AutoUpdateQuery<ENTITY> extends AutoModifyQuery<ENTITY> implements
        UpdateQuery {

    protected boolean nullExcluded;

    protected boolean versionIgnored;

    protected boolean optimisticLockExceptionSuppressed;

    protected boolean unchangedPropertyIncluded;

    protected UpdateQueryHelper<ENTITY> helper;

    public AutoUpdateQuery(EntityDesc<ENTITY> entityType) {
        super(entityType);
    }

    @Override
    public void prepare() {
        super.prepare();
        assertNotNull(method, entityDesc, entity);
        setupHelper();
        preUpdate();
        prepareIdAndVersionPropertyTypes();
        validateIdExistent();
        prepareOptions();
        prepareOptimisticLock();
        prepareTargetPropertyTypes();
        prepareSql();
        assertNotNull(sql);
    }

    protected void setupHelper() {
        helper = new UpdateQueryHelper<ENTITY>(config, entityDesc,
                includedPropertyNames, excludedPropertyNames, nullExcluded,
                versionIgnored, optimisticLockExceptionSuppressed,
                unchangedPropertyIncluded);
    }

    protected void preUpdate() {
        List<EntityPropertyDesc<ENTITY, ?>> targetPropertyTypes = helper
                .getTargetPropertyTypes(entity);
        AutoPreUpdateContext<ENTITY> context = new AutoPreUpdateContext<ENTITY>(
                entityDesc, method, config, targetPropertyTypes);
        entityDesc.preUpdate(entity, context);
        if (context.getNewEntity() != null) {
            entity = context.getNewEntity();
        }
    }

    protected void prepareOptimisticLock() {
        if (!versionIgnored && versionPropertyDesc != null) {
            if (!optimisticLockExceptionSuppressed) {
                optimisticLockCheckRequired = true;
            }
        }
    }

    protected void prepareTargetPropertyTypes() {
        targetPropertyTypes = helper.getTargetPropertyTypes(entity);
        if (!targetPropertyTypes.isEmpty()) {
            executable = true;
            sqlExecutionSkipCause = null;
        }
    }

    protected void prepareSql() {
        Naming naming = config.getNaming();
        Dialect dialect = config.getDialect();
        PreparedSqlBuilder builder = new PreparedSqlBuilder(config,
                SqlKind.UPDATE, sqlLogType);
        builder.appendSql("update ");
        builder.appendSql(entityDesc.getQualifiedTableName(naming::apply,
                dialect::applyQuote));
        builder.appendSql(" set ");
        helper.populateValues(entity, targetPropertyTypes, versionPropertyDesc,
                builder);
        if (idPropertyTypes.size() > 0) {
            builder.appendSql(" where ");
            for (EntityPropertyDesc<ENTITY, ?> propertyType : idPropertyTypes) {
                Property<ENTITY, ?> property = propertyType.createProperty();
                property.load(entity);
                builder.appendSql(propertyType.getColumnName(naming::apply,
                        dialect::applyQuote));
                builder.appendSql(" = ");
                builder.appendParameter(property.asInParameter());
                builder.appendSql(" and ");
            }
            builder.cutBackSql(5);
        }
        if (!versionIgnored && versionPropertyDesc != null) {
            if (idPropertyTypes.size() == 0) {
                builder.appendSql(" where ");
            } else {
                builder.appendSql(" and ");
            }
            Property<ENTITY, ?> property = versionPropertyDesc.createProperty();
            property.load(entity);
            builder.appendSql(versionPropertyDesc.getColumnName(naming::apply,
                    dialect::applyQuote));
            builder.appendSql(" = ");
            builder.appendParameter(property.asInParameter());
        }
        sql = builder.build(this::comment);
    }

    @Override
    public void incrementVersion() {
        if (!versionIgnored && versionPropertyDesc != null) {
            entity = versionPropertyDesc.increment(entityDesc, entity);
        }
    }

    @Override
    public void complete() {
        postUpdate();
    }

    protected void postUpdate() {
        List<EntityPropertyDesc<ENTITY, ?>> targetPropertyTypes = helper
                .getTargetPropertyTypes(entity);
        if (!versionIgnored && versionPropertyDesc != null) {
            targetPropertyTypes.add(versionPropertyDesc);
        }
        AutoPostUpdateContext<ENTITY> context = new AutoPostUpdateContext<ENTITY>(
                entityDesc, method, config, targetPropertyTypes);
        entityDesc.postUpdate(entity, context);
        if (context.getNewEntity() != null) {
            entity = context.getNewEntity();
        }
        entityDesc.saveCurrentStates(entity);
    }

    public void setNullExcluded(boolean nullExcluded) {
        this.nullExcluded = nullExcluded;
    }

    public void setVersionIgnored(boolean versionIgnored) {
        this.versionIgnored |= versionIgnored;
    }

    public void setOptimisticLockExceptionSuppressed(
            boolean optimisticLockExceptionSuppressed) {
        this.optimisticLockExceptionSuppressed = optimisticLockExceptionSuppressed;
    }

    public void setUnchangedPropertyIncluded(Boolean unchangedPropertyIncluded) {
        this.unchangedPropertyIncluded = unchangedPropertyIncluded;
    }

    protected static class AutoPreUpdateContext<E> extends
            AbstractPreUpdateContext<E> {

        protected final Set<String> changedPropertyNames;

        public AutoPreUpdateContext(EntityDesc<E> entityType, Method method,
                Config config,
                List<EntityPropertyDesc<E, ?>> targetPropertyTypes) {
            super(entityType, method, config);
            assertNotNull(targetPropertyTypes);
            changedPropertyNames = new HashSet<String>(
                    targetPropertyTypes.size());
            for (EntityPropertyDesc<E, ?> propertyType : targetPropertyTypes) {
                changedPropertyNames.add(propertyType.getName());
            }
        }

        @Override
        public boolean isEntityChanged() {
            return !changedPropertyNames.isEmpty();
        }

        @Override
        public boolean isPropertyChanged(String propertyName) {
            validatePropertyDefined(propertyName);
            return changedPropertyNames.contains(propertyName);
        }
    }

    protected static class AutoPostUpdateContext<E> extends
            AbstractPostUpdateContext<E> {

        protected final Set<String> changedPropertyNames;

        public AutoPostUpdateContext(EntityDesc<E> entityType, Method method,
                Config config,
                List<EntityPropertyDesc<E, ?>> targetPropertyTypes) {
            super(entityType, method, config);
            assertNotNull(targetPropertyTypes);
            changedPropertyNames = new HashSet<String>(
                    targetPropertyTypes.size());
            for (EntityPropertyDesc<E, ?> propertyType : targetPropertyTypes) {
                changedPropertyNames.add(propertyType.getName());
            }
        }

        @Override
        public boolean isPropertyChanged(String propertyName) {
            validatePropertyDefined(propertyName);
            return changedPropertyNames.contains(propertyName);
        }
    }

}
