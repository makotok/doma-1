/*
 * Copyright 2004-2009 the Seasar Foundation and the Others.
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
package org.seasar.doma.jdbc.entity;

/**
 * {@link EntityMeta} の骨格実装です。
 * 
 * @author taedium
 * 
 */
public abstract class AbstractEntityMeta<E> implements EntityMeta<E> {

    private static final long serialVersionUID = 1L;

    private final String __catalogName;

    private final String __schemaName;

    private final String __tableName;

    /**
     * インスタンスを構築します。
     * 
     * @param catalogName
     *            カタログ名
     * @param schemaName
     *            スキーマ名
     * @param tableName
     *            テーブル名
     */
    public AbstractEntityMeta(String catalogName, String schemaName,
            String tableName) {
        this.__catalogName = catalogName;
        this.__schemaName = schemaName;
        this.__tableName = tableName;
    }

    @Override
    public String getCatalogName() {
        return __catalogName;
    }

    @Override
    public String getSchemaName() {
        return __schemaName;
    }

    @Override
    public String getTableName() {
        return __tableName;
    }

}