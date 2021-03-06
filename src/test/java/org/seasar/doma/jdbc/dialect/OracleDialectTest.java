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
package org.seasar.doma.jdbc.dialect;

import org.seasar.doma.expr.ExpressionFunctions;

import junit.framework.TestCase;

/**
 * @author taedium
 * 
 */
public class OracleDialectTest extends TestCase {

    public void testExpressionFunctions_prefix() throws Exception {
        OracleDialect dialect = new OracleDialect();
        ExpressionFunctions functions = dialect.getExpressionFunctions();
        assertEquals("a$$a$%a$_a％a＿%", functions.prefix("a$a%a_a％a＿"));
    }

    public void testExpressionFunctions_prefix_escape() throws Exception {
        OracleDialect dialect = new OracleDialect();
        ExpressionFunctions functions = dialect.getExpressionFunctions();
        assertEquals("a!!a!%a!_a％a＿%", functions.prefix("a!a%a_a％a＿", '!'));
    }

    public void testExpressionFunctions_prefix_escapeWithBackslash()
            throws Exception {
        OracleDialect dialect = new OracleDialect();
        ExpressionFunctions functions = dialect.getExpressionFunctions();
        assertEquals("a\\\\a\\%a\\_a％a＿%",
                functions.prefix("a\\a%a_a％a＿", '\\'));
    }

}
