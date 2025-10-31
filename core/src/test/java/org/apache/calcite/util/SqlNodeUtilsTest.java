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

import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlCharStringLiteral;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNumericLiteral;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for SqlNodeUtils class.
 */
public class SqlNodeUtilsTest {
  // Test data for RexNode tests
  private RexBuilder rexBuilder;

  @BeforeEach
  public void setUp() {
    rexBuilder = new RexBuilder(new org.apache.calcite.jdbc.JavaTypeFactoryImpl());
  }

  @Test
  public void testIsDecimalConstantSqlNodeWithDecimalLiteral() {
    // Test with a decimal literal
    SqlNumericLiteral decimalLiteral = SqlNumericLiteral.createExactNumeric("123.45",
        SqlParserPos.ZERO);
    assertTrue(SqlNodeUtils.isDecimalConstantSqlNode(decimalLiteral));
  }

  @Test
  public void testIsDecimalConstantSqlNodeWithIntegerLiteral() {
    // Test with an integer literal (which is not considered a decimal constant)
    SqlNumericLiteral integerLiteral = SqlNumericLiteral.createExactNumeric("123",
        SqlParserPos.ZERO);
    assertFalse(SqlNodeUtils.isDecimalConstantSqlNode(integerLiteral));
  }

  @Test
  public void testIsDecimalConstantSqlNodeWithNull() {
    // Test with null input
    assertFalse(SqlNodeUtils.isDecimalConstantSqlNode(null));
  }

  @Test
  public void testIsDecimalConstantSqlNodeWithIntegerCall() {
    // Test with an integer literal (which is not considered a decimal constant)
    SqlNumericLiteral decimalLiteral1 = SqlNumericLiteral.createExactNumeric("123",
        SqlParserPos.ZERO);
    SqlNumericLiteral decimalLiteral2 = SqlNumericLiteral.createExactNumeric("678",
        SqlParserPos.ZERO);
    SqlCall call = SqlStdOperatorTable.PLUS.createCall(SqlParserPos.ZERO, decimalLiteral1,
        decimalLiteral2);
    assertFalse(SqlNodeUtils.isDecimalConstantSqlNode(call));
  }

  @Test
  public void testIsDecimalConstantSqlNodeWithSimpleCall() {
    // Test with a simple call containing decimal constants
    SqlNumericLiteral decimalLiteral1 = SqlNumericLiteral.createExactNumeric("123.45",
        SqlParserPos.ZERO);
    SqlNumericLiteral decimalLiteral2 = SqlNumericLiteral.createExactNumeric("678.90",
        SqlParserPos.ZERO);
    SqlCall call = SqlStdOperatorTable.PLUS.createCall(SqlParserPos.ZERO, decimalLiteral1,
        decimalLiteral2);
    assertTrue(SqlNodeUtils.isDecimalConstantSqlNode(call));
  }

  @Test
  public void testIsDecimalConstantSqlNodeWithMixedCall() {
    // Test with a call containing both decimal and integer constants
    SqlNumericLiteral decimalLiteral = SqlNumericLiteral.createExactNumeric("123.45",
        SqlParserPos.ZERO);
    SqlNumericLiteral integerLiteral = SqlNumericLiteral.createExactNumeric("678",
        SqlParserPos.ZERO);
    SqlCall call = SqlStdOperatorTable.PLUS.createCall(SqlParserPos.ZERO, decimalLiteral,
        integerLiteral);
    assertTrue(SqlNodeUtils.isDecimalConstantSqlNode(call));
  }

  @Test
  public void testIsDecimalConstantSqlNodeWithDeeplyNestedCall() {
    // Test with a deeply nested call containing decimal constants
    SqlNumericLiteral decimalLiteral1 = SqlNumericLiteral.createExactNumeric("1.1",
        SqlParserPos.ZERO);
    SqlNumericLiteral decimalLiteral2 = SqlNumericLiteral.createExactNumeric("2.2",
        SqlParserPos.ZERO);
    SqlNumericLiteral decimalLiteral3 = SqlNumericLiteral.createExactNumeric("3.3",
        SqlParserPos.ZERO);
    SqlNumericLiteral decimalLiteral4 = SqlNumericLiteral.createExactNumeric("4.4",
        SqlParserPos.ZERO);

    // Create a deeply nested expression: ((1.1 + 2.2) + 3.3) + 4.4
    SqlCall innerCall1 = SqlStdOperatorTable.PLUS.createCall(SqlParserPos.ZERO, decimalLiteral1,
        decimalLiteral2);
    SqlCall innerCall2 = SqlStdOperatorTable.PLUS.createCall(SqlParserPos.ZERO, innerCall1,
        decimalLiteral3);
    SqlCall outerCall = SqlStdOperatorTable.PLUS.createCall(SqlParserPos.ZERO, innerCall2,
        decimalLiteral4);

    assertTrue(SqlNodeUtils.isDecimalConstantSqlNode(outerCall));
  }

  @Test
  public void testIsDecimalConstantSqlNodeWithNonArithmeticCall() {
    // Test with a non-arithmetic call (CONCAT) containing decimal constants
    SqlNumericLiteral decimalLiteral1 = SqlNumericLiteral.createExactNumeric("123.45",
        SqlParserPos.ZERO);
    SqlNumericLiteral decimalLiteral2 = SqlNumericLiteral.createExactNumeric("678.90",
        SqlParserPos.ZERO);
    SqlCall call = SqlStdOperatorTable.CONCAT.createCall(SqlParserPos.ZERO, decimalLiteral1,
        decimalLiteral2);
    assertFalse(SqlNodeUtils.isDecimalConstantSqlNode(call));
  }


  @Test
  public void testIsDecimalConstantRexNodeWithDecimalLiteral() {
    // Test with a decimal literal
    RexLiteral decimalLiteral = rexBuilder.makeExactLiteral(new BigDecimal("123.45"));
    assertTrue(SqlNodeUtils.isDecimalConstantRexNode(decimalLiteral));
  }

  @Test
  public void testIsDecimalConstantRexNodeWithIntegerLiteral() {
    // Test with an integer literal (which is not considered a decimal constant)
    RexLiteral integerLiteral = rexBuilder.makeExactLiteral(new BigDecimal("123"));
    // Even though it's created with BigDecimal, it's still considered a decimal constant
    assertFalse(SqlNodeUtils.isDecimalConstantRexNode(integerLiteral));
  }

  @Test
  public void testIsDecimalConstantRexNodeWithNull() {
    // Test with null input
    assertFalse(SqlNodeUtils.isDecimalConstantRexNode(null));
  }

  @Test
  public void testIsDecimalConstantRexNodeWithIntegerCall() {
    // Test with a simple call containing decimal constants
    RexLiteral decimalLiteral1 = rexBuilder.makeExactLiteral(new BigDecimal("123"));
    RexLiteral decimalLiteral2 = rexBuilder.makeExactLiteral(new BigDecimal("678"));
    RexCall call = (RexCall) rexBuilder.makeCall(SqlStdOperatorTable.PLUS, decimalLiteral1,
        decimalLiteral2);
    assertFalse(SqlNodeUtils.isDecimalConstantRexNode(call));
  }

  @Test
  public void testIsDecimalConstantRexNodeWithSimpleCall() {
    // Test with a simple call containing decimal constants
    RexLiteral decimalLiteral1 = rexBuilder.makeExactLiteral(new BigDecimal("123.45"));
    RexLiteral decimalLiteral2 = rexBuilder.makeExactLiteral(new BigDecimal("678.90"));
    RexCall call = (RexCall) rexBuilder.makeCall(SqlStdOperatorTable.PLUS, decimalLiteral1,
        decimalLiteral2);
    assertTrue(SqlNodeUtils.isDecimalConstantRexNode(call));
  }

  @Test
  public void testIsDecimalConstantRexNodeWithMixedCall() {
    // Test with a call containing both decimal and integer constants
    RexLiteral decimalLiteral = rexBuilder.makeExactLiteral(new BigDecimal("123.45"));
    RexLiteral integerLiteral = rexBuilder.makeExactLiteral(new BigDecimal("678"));
    RexCall call = (RexCall) rexBuilder.makeCall(SqlStdOperatorTable.PLUS, decimalLiteral,
        integerLiteral);
    assertTrue(SqlNodeUtils.isDecimalConstantRexNode(call));
  }

  @Test
  public void testIsDecimalConstantRexNodeWithDeeplyNestedCall() {
    // Test with a deeply nested call containing decimal constants
    RexLiteral decimalLiteral1 = rexBuilder.makeExactLiteral(new BigDecimal("1.1"));
    RexLiteral decimalLiteral2 = rexBuilder.makeExactLiteral(new BigDecimal("2.2"));
    RexLiteral decimalLiteral3 = rexBuilder.makeExactLiteral(new BigDecimal("3.3"));
    RexLiteral decimalLiteral4 = rexBuilder.makeExactLiteral(new BigDecimal("4.4"));

    // Create a deeply nested expression: ((1.1 + 2.2) + 3.3) + 4.4
    RexCall innerCall1 = (RexCall) rexBuilder.makeCall(SqlStdOperatorTable.PLUS, decimalLiteral1,
        decimalLiteral2);
    RexCall innerCall2 = (RexCall) rexBuilder.makeCall(SqlStdOperatorTable.PLUS, innerCall1,
        decimalLiteral3);
    RexCall outerCall = (RexCall) rexBuilder.makeCall(SqlStdOperatorTable.PLUS, innerCall2,
        decimalLiteral4);

    assertTrue(SqlNodeUtils.isDecimalConstantRexNode(outerCall));
  }

  @Test
  public void testIsDecimalConstantRexNodeWithNonArithmeticCall() {
    // Test with a non-arithmetic call (LIKE) containing decimal constants
    RexLiteral decimalLiteral1 = rexBuilder.makeExactLiteral(new BigDecimal("123.45"));
    RexLiteral decimalLiteral2 = rexBuilder.makeLiteral("678.90");
    RexCall call = (RexCall) rexBuilder.makeCall(SqlStdOperatorTable.LIKE, decimalLiteral1,
        decimalLiteral2);
    assertFalse(SqlNodeUtils.isDecimalConstantRexNode(call));
  }

  @Test
  public void testIsNumericLiteralRexNodeWithNumericLiteral() {
    // Test with a numeric literal
    RexLiteral numericLiteral = rexBuilder.makeExactLiteral(new BigDecimal("123.45"));
    assertTrue(SqlNodeUtils.isNumericLiteralRexNode(numericLiteral));
  }

  @Test
  public void testIsNumericLiteralRexNodeWithNonNumericLiteral() {
    // Test with a non-numeric literal
    RexLiteral stringLiteral = rexBuilder.makeLiteral("hello");
    assertFalse(SqlNodeUtils.isNumericLiteralRexNode(stringLiteral));
  }

  @Test
  public void testIsNumericLiteralRexNodeWithNull() {
    // Test with null input
    assertFalse(SqlNodeUtils.isNumericLiteralRexNode(null));
  }

  @Test
  public void testIsNumericLiteralRexNodeWithArithmeticCall() {
    // Test with an arithmetic call containing numeric literals
    RexLiteral numericLiteral1 = rexBuilder.makeExactLiteral(new BigDecimal("123.45"));
    RexLiteral numericLiteral2 = rexBuilder.makeExactLiteral(new BigDecimal("678"));
    RexCall call = (RexCall) rexBuilder.makeCall(SqlStdOperatorTable.PLUS, numericLiteral1,
        numericLiteral2);
    assertTrue(SqlNodeUtils.isNumericLiteralRexNode(call));
  }

  @Test
  public void testIsNumericLiteralRexNodeWithNonNumericCall() {
    // Test with a call containing non-numeric literals
    RexLiteral stringLiteral1 = rexBuilder.makeLiteral("hello");
    RexLiteral stringLiteral2 = rexBuilder.makeLiteral("world");
    RexCall call = (RexCall) rexBuilder.makeCall(SqlStdOperatorTable.PLUS, stringLiteral1,
        stringLiteral2);
    assertFalse(SqlNodeUtils.isNumericLiteralRexNode(call));
  }

  @Test
  public void testIsNumericLiteralRexNodeWithMixedCall() {
    // Test with a call containing both numeric and non-numeric literals
    RexLiteral numericLiteral = rexBuilder.makeExactLiteral(new BigDecimal("123.45"));
    RexLiteral stringLiteral = rexBuilder.makeLiteral("hello");
    RexCall call = (RexCall) rexBuilder.makeCall(SqlStdOperatorTable.PLUS, numericLiteral,
        stringLiteral);
    assertFalse(SqlNodeUtils.isNumericLiteralRexNode(call));
  }

  @Test
  public void testIsNumericLiteralRexNodeWithDeeplyNestedCall() {
    // Test with a deeply nested arithmetic call containing numeric literals
    RexLiteral numericLiteral1 = rexBuilder.makeExactLiteral(new BigDecimal("1.1"));
    RexLiteral numericLiteral2 = rexBuilder.makeExactLiteral(new BigDecimal("2.2"));
    RexLiteral numericLiteral3 = rexBuilder.makeExactLiteral(new BigDecimal("3.3"));
    RexLiteral numericLiteral4 = rexBuilder.makeExactLiteral(new BigDecimal("4.4"));

    // Create a deeply nested expression: ((1.1 + 2.2) + 3.3) + 4.4
    RexCall innerCall1 = (RexCall) rexBuilder.makeCall(SqlStdOperatorTable.PLUS, numericLiteral1,
        numericLiteral2);
    RexCall innerCall2 = (RexCall) rexBuilder.makeCall(SqlStdOperatorTable.PLUS, innerCall1,
        numericLiteral3);
    RexCall outerCall = (RexCall) rexBuilder.makeCall(SqlStdOperatorTable.PLUS, innerCall2,
        numericLiteral4);

    assertTrue(SqlNodeUtils.isNumericLiteralRexNode(outerCall));
  }

  @Test
  public void testIsNumericLiteralRexNodeWithNonArithmeticCall() {
    // Test with a non-arithmetic call containing numeric literals
    RexLiteral numericLiteral1 = rexBuilder.makeExactLiteral(new BigDecimal("123.45"));
    RexLiteral numericLiteral2 = rexBuilder.makeExactLiteral(new BigDecimal("678.90"));
    RexCall call = (RexCall) rexBuilder.makeCall(SqlStdOperatorTable.LIKE, numericLiteral1,
        numericLiteral2);
    assertFalse(SqlNodeUtils.isNumericLiteralRexNode(call));
  }

  @Test
  public void testIsNumericLiteralRexNodeWithDifferentArithmeticOperators() {
    // Test with different arithmetic operators
    RexLiteral numericLiteral1 = rexBuilder.makeExactLiteral(new BigDecimal("10"));
    RexLiteral numericLiteral2 = rexBuilder.makeExactLiteral(new BigDecimal("5"));

    // Test PLUS
    RexCall plusCall = (RexCall) rexBuilder.makeCall(SqlStdOperatorTable.PLUS, numericLiteral1,
        numericLiteral2);
    assertTrue(SqlNodeUtils.isNumericLiteralRexNode(plusCall));

    // Test MINUS
    RexCall minusCall = (RexCall) rexBuilder.makeCall(SqlStdOperatorTable.MINUS, numericLiteral1,
        numericLiteral2);
    assertTrue(SqlNodeUtils.isNumericLiteralRexNode(minusCall));

    // Test MULTIPLY
    RexCall timesCall = (RexCall) rexBuilder.makeCall(SqlStdOperatorTable.MULTIPLY, numericLiteral1,
        numericLiteral2);
    assertTrue(SqlNodeUtils.isNumericLiteralRexNode(timesCall));

    // Test DIVIDE
    RexCall divideCall = (RexCall) rexBuilder.makeCall(SqlStdOperatorTable.DIVIDE, numericLiteral1,
        numericLiteral2);
    assertTrue(SqlNodeUtils.isNumericLiteralRexNode(divideCall));

    // Test MOD
    RexCall modCall = (RexCall) rexBuilder.makeCall(SqlStdOperatorTable.MOD, numericLiteral1,
        numericLiteral2);
    assertTrue(SqlNodeUtils.isNumericLiteralRexNode(modCall));
  }

  @Test
  public void testIsNumericLiteralRexNodeWithApproximateNumericLiterals() {
    // Test with approximate numeric literals
    RexLiteral approxLiteral1 = rexBuilder.makeApproxLiteral(new BigDecimal("1.23E45"));
    RexLiteral approxLiteral2 = rexBuilder.makeApproxLiteral(new BigDecimal("6.78E90"));
    RexCall call = (RexCall) rexBuilder.makeCall(SqlStdOperatorTable.PLUS, approxLiteral1,
        approxLiteral2);
    assertTrue(SqlNodeUtils.isNumericLiteralRexNode(call));
  }

  @Test
  public void testIsNumericLiteralRexNodeWithMixedArithmeticOperators() {
    // Test with mixed arithmetic operators in nested expressions
    RexLiteral numericLiteral1 = rexBuilder.makeExactLiteral(new BigDecimal("10"));
    RexLiteral numericLiteral2 = rexBuilder.makeExactLiteral(new BigDecimal("5"));
    RexLiteral numericLiteral3 = rexBuilder.makeExactLiteral(new BigDecimal("2"));

    // Create a complex expression: (10 + 5) * 2
    RexCall innerCall = (RexCall) rexBuilder.makeCall(SqlStdOperatorTable.PLUS, numericLiteral1,
        numericLiteral2);
    RexCall outerCall = (RexCall) rexBuilder.makeCall(SqlStdOperatorTable.MULTIPLY, innerCall,
        numericLiteral3);

    assertTrue(SqlNodeUtils.isNumericLiteralRexNode(outerCall));
  }

  @Test
  public void testIsNumericLiteralRexNodeWithUnaryMinus() {
    // Test with unary minus operator
    RexLiteral numericLiteral = rexBuilder.makeExactLiteral(new BigDecimal("10"));
    RexCall call = (RexCall) rexBuilder.makeCall(SqlStdOperatorTable.UNARY_MINUS, numericLiteral);

    assertFalse(SqlNodeUtils.isNumericLiteralRexNode(call));
  }

  @Test
  public void testIsNumericLiteralRexNodeWithUnaryPlus() {
    // Test with unary plus operator
    RexLiteral numericLiteral = rexBuilder.makeExactLiteral(new BigDecimal("10"));
    RexCall call = (RexCall) rexBuilder.makeCall(SqlStdOperatorTable.UNARY_PLUS, numericLiteral);

    assertFalse(SqlNodeUtils.isNumericLiteralRexNode(call));
  }

  @Test
  public void testIsNumericLiteralRexNodeWithUnaryOperatorsAndArithmetic() {
    // Test with unary operators combined with arithmetic operators
    RexLiteral numericLiteral1 = rexBuilder.makeExactLiteral(new BigDecimal("10"));
    RexLiteral numericLiteral2 = rexBuilder.makeExactLiteral(new BigDecimal("5"));

    // Create expression: (-10) + 5
    RexCall unaryMinus = (RexCall) rexBuilder.makeCall(SqlStdOperatorTable.UNARY_MINUS, numericLiteral1);
    RexCall call = (RexCall) rexBuilder.makeCall(SqlStdOperatorTable.PLUS, unaryMinus, numericLiteral2);

    assertFalse(SqlNodeUtils.isNumericLiteralRexNode(call));
  }

  @Test
  public void testIsNumericLiteralSqlNodeWithNumericLiteral() {
    // Test with a numeric literal
    SqlNumericLiteral numericLiteral = SqlNumericLiteral.createExactNumeric("123.45",
        SqlParserPos.ZERO);
    assertTrue(SqlNodeUtils.isNumericLiteralSqlNode(numericLiteral));
  }

  @Test
  public void testIsNumericLiteralSqlNodeWithNonNumericLiteral() {
    // Test with a non-numeric literal (string literal)
    SqlCharStringLiteral stringLiteral = SqlLiteral.createCharString("hello", SqlParserPos.ZERO);
    assertFalse(SqlNodeUtils.isNumericLiteralSqlNode(stringLiteral));
  }

  @Test
  public void testIsNumericLiteralSqlNodeWithNull() {
    // Test with null input
    assertFalse(SqlNodeUtils.isNumericLiteralSqlNode(null));
  }

  @Test
  public void testIsNumericLiteralSqlNodeWithArithmeticCall() {
    // Test with an arithmetic call containing numeric literals
    SqlNumericLiteral numericLiteral1 = SqlNumericLiteral.createExactNumeric("123.45",
        SqlParserPos.ZERO);
    SqlNumericLiteral numericLiteral2 = SqlNumericLiteral.createExactNumeric("678",
        SqlParserPos.ZERO);
    SqlCall call = SqlStdOperatorTable.PLUS.createCall(SqlParserPos.ZERO, numericLiteral1,
        numericLiteral2);
    assertTrue(SqlNodeUtils.isNumericLiteralSqlNode(call));
  }

  @Test
  public void testIsNumericLiteralSqlNodeWithNonNumericCall() {
    // Test with a call containing non-numeric literals
    SqlCharStringLiteral stringLiteral1 = SqlLiteral.createCharString("hello", SqlParserPos.ZERO);
    SqlCharStringLiteral stringLiteral2 = SqlLiteral.createCharString("world", SqlParserPos.ZERO);
    SqlCall call = SqlStdOperatorTable.PLUS.createCall(SqlParserPos.ZERO, stringLiteral1,
        stringLiteral2);
    assertFalse(SqlNodeUtils.isNumericLiteralSqlNode(call));
  }

  @Test
  public void testIsNumericLiteralSqlNodeWithMixedCall() {
    // Test with a call containing both numeric and non-numeric literals
    SqlNumericLiteral numericLiteral = SqlNumericLiteral.createExactNumeric("123.45",
        SqlParserPos.ZERO);
    SqlCharStringLiteral stringLiteral = SqlLiteral.createCharString("hello", SqlParserPos.ZERO);
    SqlCall call = SqlStdOperatorTable.PLUS.createCall(SqlParserPos.ZERO, numericLiteral,
        stringLiteral);
    assertFalse(SqlNodeUtils.isNumericLiteralSqlNode(call));
  }

  @Test
  public void testIsNumericLiteralSqlNodeWithDeeplyNestedCall() {
    // Test with a deeply nested arithmetic call containing numeric literals
    SqlNumericLiteral numericLiteral1 = SqlNumericLiteral.createExactNumeric("1.1",
        SqlParserPos.ZERO);
    SqlNumericLiteral numericLiteral2 = SqlNumericLiteral.createExactNumeric("2.2",
        SqlParserPos.ZERO);
    SqlNumericLiteral numericLiteral3 = SqlNumericLiteral.createExactNumeric("3.3",
        SqlParserPos.ZERO);
    SqlNumericLiteral numericLiteral4 = SqlNumericLiteral.createExactNumeric("4.4",
        SqlParserPos.ZERO);

    // Create a deeply nested expression: ((1.1 + 2.2) + 3.3) + 4.4
    SqlCall innerCall1 = SqlStdOperatorTable.PLUS.createCall(SqlParserPos.ZERO, numericLiteral1,
        numericLiteral2);
    SqlCall innerCall2 = SqlStdOperatorTable.PLUS.createCall(SqlParserPos.ZERO, innerCall1,
        numericLiteral3);
    SqlCall outerCall = SqlStdOperatorTable.PLUS.createCall(SqlParserPos.ZERO, innerCall2,
        numericLiteral4);

    assertTrue(SqlNodeUtils.isNumericLiteralSqlNode(outerCall));
  }

  @Test
  public void testIsNumericLiteralSqlNodeWithNonArithmeticCall() {
    // Test with a non-arithmetic call containing numeric literals
    SqlNumericLiteral numericLiteral1 = SqlNumericLiteral.createExactNumeric("123.45",
        SqlParserPos.ZERO);
    SqlNumericLiteral numericLiteral2 = SqlNumericLiteral.createExactNumeric("678.90",
        SqlParserPos.ZERO);
    SqlCall call = SqlStdOperatorTable.LIKE.createCall(SqlParserPos.ZERO, numericLiteral1,
        numericLiteral2);
    assertFalse(SqlNodeUtils.isNumericLiteralSqlNode(call));
  }

  @Test
  public void testIsNumericLiteralSqlNodeWithDifferentArithmeticOperators() {
    // Test with different arithmetic operators
    SqlNumericLiteral numericLiteral1 = SqlNumericLiteral.createExactNumeric("10", SqlParserPos.ZERO);
    SqlNumericLiteral numericLiteral2 = SqlNumericLiteral.createExactNumeric("5", SqlParserPos.ZERO);

    // Test PLUS
    SqlCall plusCall = SqlStdOperatorTable.PLUS.createCall(SqlParserPos.ZERO, numericLiteral1,
        numericLiteral2);
    assertTrue(SqlNodeUtils.isNumericLiteralSqlNode(plusCall));

    // Test MINUS
    SqlCall minusCall = SqlStdOperatorTable.MINUS.createCall(SqlParserPos.ZERO, numericLiteral1,
        numericLiteral2);
    assertTrue(SqlNodeUtils.isNumericLiteralSqlNode(minusCall));

    // Test MULTIPLY
    SqlCall timesCall = SqlStdOperatorTable.MULTIPLY.createCall(SqlParserPos.ZERO, numericLiteral1,
        numericLiteral2);
    assertTrue(SqlNodeUtils.isNumericLiteralSqlNode(timesCall));

    // Test DIVIDE
    SqlCall divideCall = SqlStdOperatorTable.DIVIDE.createCall(SqlParserPos.ZERO, numericLiteral1,
        numericLiteral2);
    assertTrue(SqlNodeUtils.isNumericLiteralSqlNode(divideCall));

    // Test MOD
    SqlCall modCall = SqlStdOperatorTable.MOD.createCall(SqlParserPos.ZERO, numericLiteral1,
        numericLiteral2);
    assertTrue(SqlNodeUtils.isNumericLiteralSqlNode(modCall));
  }

  @Test
  public void testIsNumericLiteralSqlNodeWithApproximateNumeric() {
    // Test with approximate numeric literals
    SqlNumericLiteral approxLiteral1 = SqlNumericLiteral.createApproxNumeric("1.23E45",
        SqlParserPos.ZERO);
    SqlNumericLiteral approxLiteral2 = SqlNumericLiteral.createApproxNumeric("6.78E90",
        SqlParserPos.ZERO);
    SqlCall call = SqlStdOperatorTable.PLUS.createCall(SqlParserPos.ZERO, approxLiteral1,
        approxLiteral2);
    assertTrue(SqlNodeUtils.isNumericLiteralSqlNode(call));
  }
}
