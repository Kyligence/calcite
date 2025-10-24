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
package org.apache.calcite.util;

import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.sql.SqlNumericLiteral;
import org.apache.calcite.sql.SqlOperatorBinding;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.sql.type.SqlTypeUtil;

public class SqlNodeUtils {

  private SqlNodeUtils() {
  }

  public static boolean isDecimalConstant(SqlNumericLiteral literal) {
    SqlTypeName typeName = literal.getTypeName();

    return typeName == SqlTypeName.DECIMAL;
  }

  public static boolean isNumericLiteral(SqlOperatorBinding binding, int ordinal) {
    return binding.isOperandLiteral(ordinal, false) &&
        SqlTypeUtil.isNumeric(binding.getOperandType(ordinal));
  }

  public static boolean isDecimalConstant(RexLiteral literal) {
    SqlTypeName typeName = literal.getType().getSqlTypeName();

    return typeName == SqlTypeName.DECIMAL
        || typeName == SqlTypeName.DOUBLE
        || typeName == SqlTypeName.REAL
        || typeName == SqlTypeName.FLOAT;
  }
}
