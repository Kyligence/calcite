/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.calcite.sql.fun;

import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlSpecialOperator;
import org.apache.calcite.sql.type.InferTypes;
import org.apache.calcite.sql.type.OperandTypes;
import org.apache.calcite.sql.type.ReturnTypes;

/**
 * https://olapio.atlassian.net/browse/KE-12894
 *
 * A special operator for the division of timestamp diff function.
 * The purpose of defining a special operator is that
 * kylin could be able to distinguish normal division and timestamp diff division,
 * so kylin can cast, for example, (/INT(Reinterpret(-($23, $22)), 1000) to bigint,
 * otherwise, spark will be interpreting this expression as double type
 *
 */
public class SqlDateTimeDivisionOperator extends SqlSpecialOperator {
  public SqlDateTimeDivisionOperator() {
    super(
        "/",
        SqlKind.DIVIDE,
        60,
        true,
        ReturnTypes.BIGINT_NULLABLE,
        InferTypes.FIRST_KNOWN,
        OperandTypes.DIVISION_OPERATOR);
  }
}

// End SqlDateTimeDivisionOperator.java
