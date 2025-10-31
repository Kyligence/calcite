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
package org.apache.calcite.sql.type;

import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.runtime.CalciteException;
import org.apache.calcite.runtime.Resources;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlNumericLiteral;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.validate.SqlValidatorException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Test for {@link org.apache.calcite.sql.type.ReturnTypes}.
 * Tests the DECIMAL_PRODUCT return type inference without using reflection.
 */
class ReturnTypesTest {

  private SqlTypeFixture f;
  private RelDataTypeFactory typeFactory;
  private RexBuilder rexBuilder;

  @BeforeEach
  void setUp() {
    f = new SqlTypeFixture();
    typeFactory = f.typeFactory;
    rexBuilder = new RexBuilder(typeFactory);
  }

  @Test
  void testDecimalProductWithDecimalTypes() {
    // Test case: Both operands are DECIMAL types
    RelDataType decimal1 = typeFactory.createSqlType(SqlTypeName.DECIMAL, 10, 2);
    RelDataType decimal2 = typeFactory.createSqlType(SqlTypeName.DECIMAL, 8, 3);

    SqlNumericLiteral literal1 = SqlNumericLiteral.createExactNumeric("123.45", SqlParserPos.ZERO);
    SqlNumericLiteral literal2 = SqlNumericLiteral.createExactNumeric("67.89", SqlParserPos.ZERO);
    SqlCall call = SqlStdOperatorTable.MULTIPLY.createCall(SqlParserPos.ZERO, literal1, literal2);

    // Create a test operator binding
    TestSqlOperatorBinding binding = new TestSqlOperatorBinding(typeFactory, call, decimal1,
        decimal2);

    // Test the DECIMAL_PRODUCT return type inference
    RelDataType result = ReturnTypes.DECIMAL_PRODUCT.inferReturnType(binding);

    assertNotNull(result);
    assertEquals(SqlTypeName.DECIMAL, result.getSqlTypeName());
  }

  @Test
  void testDecimalProductWithIntegerTypes() {
    // Test case: Both operands are INTEGER types
    RelDataType int1 = typeFactory.createSqlType(SqlTypeName.INTEGER);
    RelDataType int2 = typeFactory.createSqlType(SqlTypeName.INTEGER);

    SqlNumericLiteral literal1 = SqlNumericLiteral.createExactNumeric("123", SqlParserPos.ZERO);
    SqlNumericLiteral literal2 = SqlNumericLiteral.createExactNumeric("456", SqlParserPos.ZERO);
    SqlCall call = SqlStdOperatorTable.MULTIPLY.createCall(SqlParserPos.ZERO, literal1, literal2);

    TestSqlOperatorBinding binding = new TestSqlOperatorBinding(typeFactory, call, int1, int2);

    RelDataType result = ReturnTypes.DECIMAL_PRODUCT.inferReturnType(binding);

    assertNull(result);
  }

  @Test
  void testDecimalProductWithMixedTypes() {
    // Test case: One operand is DECIMAL, other is INTEGER
    RelDataType decimalType = typeFactory.createSqlType(SqlTypeName.DECIMAL, 10, 2);
    RelDataType integerType = typeFactory.createSqlType(SqlTypeName.INTEGER);

    SqlNumericLiteral decimalLiteral = SqlNumericLiteral.createExactNumeric("123.45",
        SqlParserPos.ZERO);
    SqlNumericLiteral intLiteral = SqlNumericLiteral.createExactNumeric("456", SqlParserPos.ZERO);
    SqlCall call = SqlStdOperatorTable.MULTIPLY.createCall(SqlParserPos.ZERO, decimalLiteral,
        intLiteral);

    TestSqlOperatorBinding binding = new TestSqlOperatorBinding(typeFactory, call, decimalType,
        integerType);

    RelDataType result = ReturnTypes.DECIMAL_PRODUCT.inferReturnType(binding);

    assertNotNull(result);
    assertEquals(SqlTypeName.DECIMAL, result.getSqlTypeName());
  }

  @Test
  void testDecimalProductWithBigIntTypes() {
    // Test case: Both operands are BIGINT types
    RelDataType bigint1 = typeFactory.createSqlType(SqlTypeName.BIGINT);
    RelDataType bigint2 = typeFactory.createSqlType(SqlTypeName.BIGINT);

    SqlNumericLiteral literal1 = SqlNumericLiteral.createExactNumeric("123456789",
        SqlParserPos.ZERO);
    SqlNumericLiteral literal2 = SqlNumericLiteral.createExactNumeric("987654321",
        SqlParserPos.ZERO);
    SqlCall call = SqlStdOperatorTable.MULTIPLY.createCall(SqlParserPos.ZERO, literal1, literal2);

    TestSqlOperatorBinding binding = new TestSqlOperatorBinding(typeFactory, call, bigint1,
        bigint2);

    RelDataType result = ReturnTypes.DECIMAL_PRODUCT.inferReturnType(binding);

    assertNull(result);
  }

  @Test
  void testDecimalProductWithRexNodes() {
    // Test using RexBuilder to create RexCall (similar to SqlNodeUtilsTest pattern)
    RexLiteral decimalLiteral1 = rexBuilder.makeExactLiteral(new BigDecimal("123.45"));
    RexLiteral decimalLiteral2 = rexBuilder.makeExactLiteral(new BigDecimal("67.89"));
    RexCall rexCall = (RexCall) rexBuilder.makeCall(SqlStdOperatorTable.MULTIPLY,
        decimalLiteral1, decimalLiteral2);

    // Create a RexCallBinding
    TestRexCallBinding rexBinding = new TestRexCallBinding(typeFactory, rexCall);

    RelDataType result = ReturnTypes.DECIMAL_PRODUCT.inferReturnType(rexBinding);

    assertNotNull(result);
    assertEquals(SqlTypeName.DECIMAL, result.getSqlTypeName());
  }

  @Test
  void testDecimalProductWithIntegerRexNodes() {
    // Test using RexBuilder with integer literals
    RexLiteral intLiteral1 = rexBuilder.makeExactLiteral(new BigDecimal("123"));
    RexLiteral intLiteral2 = rexBuilder.makeExactLiteral(new BigDecimal("456"));
    RexCall rexCall = (RexCall) rexBuilder.makeCall(SqlStdOperatorTable.MULTIPLY,
        intLiteral1, intLiteral2);

    TestRexCallBinding rexBinding = new TestRexCallBinding(typeFactory, rexCall);

    RelDataType result = ReturnTypes.DECIMAL_PRODUCT.inferReturnType(rexBinding);

    assertNull(result);
  }

  @Test
  void testDecimalProductWithMixedRexNodes() {
    // Test using RexBuilder with mixed decimal and integer literals
    RexLiteral decimalLiteral = rexBuilder.makeExactLiteral(new BigDecimal("123.45"));
    RexLiteral intLiteral = rexBuilder.makeExactLiteral(new BigDecimal("456"));
    RexCall rexCall = (RexCall) rexBuilder.makeCall(SqlStdOperatorTable.MULTIPLY,
        decimalLiteral, intLiteral);

    TestRexCallBinding rexBinding = new TestRexCallBinding(typeFactory, rexCall);

    RelDataType result = ReturnTypes.DECIMAL_PRODUCT.inferReturnType(rexBinding);

    assertNotNull(result);
    assertEquals(SqlTypeName.DECIMAL, result.getSqlTypeName());
  }

  /**
   * Simple test implementation of SqlOperatorBinding for testing purposes.
   */
  private static class TestSqlOperatorBinding extends org.apache.calcite.sql.SqlOperatorBinding {
    private final RelDataType[] operandTypes;
    private final SqlCall call;

    TestSqlOperatorBinding(RelDataTypeFactory typeFactory, SqlCall call,
        RelDataType... operandTypes) {
      super(typeFactory, call.getOperator());
      this.operandTypes = operandTypes;
      this.call = call;
    }

    @Override
    public int getOperandCount() {
      return operandTypes.length;
    }

    @Override
    public RelDataType getOperandType(int ordinal) {
      return operandTypes[ordinal];
    }

    @Override
    public CalciteException newError(Resources.ExInst<SqlValidatorException> e) {
      return new CalciteException(e.str(), null);
    }
  }

  /**
   * Test implementation of RexCallBinding for testing RexNode-based scenarios.
   */
  private static class TestRexCallBinding extends org.apache.calcite.rex.RexCallBinding {
    private final RexCall rexCall;

    TestRexCallBinding(RelDataTypeFactory typeFactory, RexCall rexCall) {
      super(typeFactory, rexCall.getOperator(), rexCall.getOperands(),
          java.util.Collections.emptyList());
      this.rexCall = rexCall;
    }
  }
}
