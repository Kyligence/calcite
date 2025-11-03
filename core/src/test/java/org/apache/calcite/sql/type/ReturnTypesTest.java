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
import org.apache.calcite.rex.RexCallBinding;
import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.runtime.CalciteException;
import org.apache.calcite.runtime.Resources;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlCallBinding;
import org.apache.calcite.sql.SqlNumericLiteral;
import org.apache.calcite.sql.SqlOperatorBinding;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.validate.SqlValidator;
import org.apache.calcite.sql.validate.SqlValidatorException;
import org.apache.calcite.sql.validate.SqlValidatorImpl;
import org.apache.calcite.util.Pair;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Collections;

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

  @Test void testDecimalProductWithDecimalTypes() {
    // Test case: Both operands are DECIMAL types
    RelDataType decimal1 = typeFactory.createSqlType(SqlTypeName.DECIMAL, 10, 2);
    RelDataType decimal2 = typeFactory.createSqlType(SqlTypeName.DECIMAL, 8, 3);

    SqlNumericLiteral literal1 = SqlNumericLiteral.createExactNumeric("123.45", SqlParserPos.ZERO);
    SqlNumericLiteral literal2 = SqlNumericLiteral.createExactNumeric("67.89", SqlParserPos.ZERO);
    SqlCall call = SqlStdOperatorTable.MULTIPLY.createCall(SqlParserPos.ZERO, literal1, literal2);

    SqlValidator validator = Mockito.mock(SqlValidatorImpl.class);
    Mockito.when(validator.getTypeFactory()).thenReturn(typeFactory);
    // Create a test operator binding
    TestSqlOperatorBinding binding = new TestSqlOperatorBinding(validator, call, decimal1,
        decimal2);

    // Test the DECIMAL_PRODUCT return type inference
    RelDataType result = ReturnTypes.DECIMAL_PRODUCT.inferReturnType(binding);

    assertNotNull(result);
    assertEquals(SqlTypeName.DECIMAL, result.getSqlTypeName());
  }

  @Test void testDecimalProductWithIntegerTypes() {
    // Test case: Both operands are INTEGER types
    RelDataType int1 = typeFactory.createSqlType(SqlTypeName.INTEGER);
    RelDataType int2 = typeFactory.createSqlType(SqlTypeName.INTEGER);

    SqlNumericLiteral literal1 = SqlNumericLiteral.createExactNumeric("123", SqlParserPos.ZERO);
    SqlNumericLiteral literal2 = SqlNumericLiteral.createExactNumeric("456", SqlParserPos.ZERO);
    SqlCall call = SqlStdOperatorTable.MULTIPLY.createCall(SqlParserPos.ZERO, literal1, literal2);
    SqlValidator validator = Mockito.mock(SqlValidatorImpl.class);
    Mockito.when(validator.getTypeFactory()).thenReturn(typeFactory);
    // Create a test operator binding
    TestSqlOperatorBinding binding = new TestSqlOperatorBinding(validator, call, int1, int2);

    RelDataType result = ReturnTypes.DECIMAL_PRODUCT.inferReturnType(binding);

    assertNull(result);
  }

  @Test void testDecimalProductWithMixedTypes() {
    // Test case: One operand is DECIMAL, other is INTEGER
    RelDataType decimalType = typeFactory.createSqlType(SqlTypeName.DECIMAL, 10, 2);
    RelDataType integerType = typeFactory.createSqlType(SqlTypeName.INTEGER);

    SqlNumericLiteral decimalLiteral = SqlNumericLiteral.createExactNumeric("123.45",
        SqlParserPos.ZERO);
    SqlNumericLiteral intLiteral = SqlNumericLiteral.createExactNumeric("456", SqlParserPos.ZERO);
    SqlCall call = SqlStdOperatorTable.MULTIPLY.createCall(SqlParserPos.ZERO, decimalLiteral,
        intLiteral);

    SqlValidator validator = Mockito.mock(SqlValidatorImpl.class);
    Mockito.when(validator.getTypeFactory()).thenReturn(typeFactory);
    // Create a test operator binding
    TestSqlOperatorBinding binding = new TestSqlOperatorBinding(validator, call, decimalType,
        integerType);

    RelDataType result = ReturnTypes.DECIMAL_PRODUCT.inferReturnType(binding);

    assertNotNull(result);
    assertEquals(SqlTypeName.DECIMAL, result.getSqlTypeName());
  }

  @Test void testDecimalProductWithBigIntTypes() {
    // Test case: Both operands are BIGINT types
    RelDataType bigint1 = typeFactory.createSqlType(SqlTypeName.BIGINT);
    RelDataType bigint2 = typeFactory.createSqlType(SqlTypeName.BIGINT);

    SqlNumericLiteral literal1 = SqlNumericLiteral.createExactNumeric("123456789",
        SqlParserPos.ZERO);
    SqlNumericLiteral literal2 = SqlNumericLiteral.createExactNumeric("987654321",
        SqlParserPos.ZERO);
    SqlCall call = SqlStdOperatorTable.MULTIPLY.createCall(SqlParserPos.ZERO, literal1, literal2);

    SqlValidator validator = Mockito.mock(SqlValidatorImpl.class);
    Mockito.when(validator.getTypeFactory()).thenReturn(typeFactory);
    // Create a test operator binding
    TestSqlOperatorBinding binding = new TestSqlOperatorBinding(validator, call, bigint1,
        bigint2);

    RelDataType result = ReturnTypes.DECIMAL_PRODUCT.inferReturnType(binding);

    assertNull(result);
  }

  @Test void testDecimalProductSqlNodeWithNegativeValues() {
    // Test case: Both operands are DECIMAL types
    RelDataType decimal1 = typeFactory.createSqlType(SqlTypeName.DECIMAL, 10, 2);
    RelDataType decimal2 = typeFactory.createSqlType(SqlTypeName.DECIMAL, 10, 0);

    SqlNumericLiteral literal1 = SqlNumericLiteral.createExactNumeric("123.45", SqlParserPos.ZERO);
    SqlNumericLiteral literal2 = SqlNumericLiteral.createExactNumeric("-67", SqlParserPos.ZERO);
    SqlCall call = SqlStdOperatorTable.MULTIPLY.createCall(SqlParserPos.ZERO, literal1, literal2);

    SqlValidator validator = Mockito.mock(SqlValidatorImpl.class);
    Mockito.when(validator.getTypeFactory()).thenReturn(typeFactory);
    // Create a test operator binding
    TestSqlOperatorBinding binding = new TestSqlOperatorBinding(validator, call, decimal1,
        decimal2);

    // Test the DECIMAL_PRODUCT return type inference
    RelDataType result = ReturnTypes.DECIMAL_PRODUCT.inferReturnType(binding);

    assertNotNull(result);
    assertEquals(SqlTypeName.DECIMAL, result.getSqlTypeName());
    assertEquals(8, result.getPrecision());
    assertEquals(2, result.getScale());
  }

  @Test void testDecimalProductRexNodeWithNegativeValues() {
    RexLiteral decimalLiteral1 = rexBuilder.makeExactLiteral(new BigDecimal("123.45"));
    RexLiteral decimalLiteral2 = rexBuilder.makeExactLiteral(new BigDecimal("-67"));
    RexCall rexCall = (RexCall) rexBuilder.makeCall(SqlStdOperatorTable.MULTIPLY,
        decimalLiteral1, decimalLiteral2);

    // Create a RexCallBinding
    RexCallBinding rexBinding = RexCallBinding.create(typeFactory, rexCall,
        Collections.emptyList());

    RelDataType result2 = ReturnTypes.DECIMAL_PRODUCT.inferReturnType(rexBinding);

    assertNotNull(result2);
    assertEquals(SqlTypeName.DECIMAL, result2.getSqlTypeName());
    assertEquals(8, result2.getPrecision());
    assertEquals(2, result2.getScale());
  }

  @Test void testDecimalProductWithRexNodes() {
    // Test using RexBuilder to create RexCall (similar to SqlNodeUtilsTest pattern)
    RexLiteral decimalLiteral1 = rexBuilder.makeExactLiteral(new BigDecimal("123.45"));
    RexLiteral decimalLiteral2 = rexBuilder.makeExactLiteral(new BigDecimal("67.89"));
    RexCall rexCall = (RexCall) rexBuilder.makeCall(SqlStdOperatorTable.MULTIPLY,
        decimalLiteral1, decimalLiteral2);

    // Create a RexCallBinding
    RexCallBinding rexBinding = RexCallBinding.create(typeFactory, rexCall,
        Collections.emptyList());

    RelDataType result = ReturnTypes.DECIMAL_PRODUCT.inferReturnType(rexBinding);

    assertNotNull(result);
    assertEquals(SqlTypeName.DECIMAL, result.getSqlTypeName());
  }

  @Test void testDecimalProductWithIntegerRexNodes() {
    // Test using RexBuilder with integer literals
    RexLiteral intLiteral1 = rexBuilder.makeExactLiteral(new BigDecimal("123"));
    RexLiteral intLiteral2 = rexBuilder.makeExactLiteral(new BigDecimal("456"));
    RexCall rexCall = (RexCall) rexBuilder.makeCall(SqlStdOperatorTable.MULTIPLY,
        intLiteral1, intLiteral2);

    RexCallBinding rexBinding = RexCallBinding.create(typeFactory, rexCall,
        Collections.emptyList());

    RelDataType result = ReturnTypes.DECIMAL_PRODUCT.inferReturnType(rexBinding);

    assertNull(result);
  }

  @Test void testDecimalProductWithMixedRexNodes() {
    // Test using RexBuilder with mixed decimal and integer literals
    RexLiteral decimalLiteral = rexBuilder.makeExactLiteral(new BigDecimal("123.45"));
    RexLiteral intLiteral = rexBuilder.makeExactLiteral(new BigDecimal("456"));
    RexCall rexCall = (RexCall) rexBuilder.makeCall(SqlStdOperatorTable.MULTIPLY,
        decimalLiteral, intLiteral);

    RexCallBinding rexBinding = RexCallBinding.create(typeFactory, rexCall,
        Collections.emptyList());

    RelDataType result = ReturnTypes.DECIMAL_PRODUCT.inferReturnType(rexBinding);

    assertNotNull(result);
    assertEquals(SqlTypeName.DECIMAL, result.getSqlTypeName());
  }

  // Comprehensive tests for getDecimalMultiplyBindingType method

  @Test void testGetDecimalMultiplyBindingTypeWithSqlCallBindingDecimalDecimal() throws Exception {
    // Test SqlCallBinding with both operands as DECIMAL literals
    RelDataType decimal1 = typeFactory.createSqlType(SqlTypeName.DECIMAL, 10, 2);
    RelDataType decimal2 = typeFactory.createSqlType(SqlTypeName.DECIMAL, 8, 3);

    SqlNumericLiteral literal1 = SqlNumericLiteral.createExactNumeric("123.45", SqlParserPos.ZERO);
    SqlNumericLiteral literal2 = SqlNumericLiteral.createExactNumeric("67.89", SqlParserPos.ZERO);
    SqlCall call = SqlStdOperatorTable.MULTIPLY.createCall(SqlParserPos.ZERO, literal1, literal2);

    SqlValidator validator = Mockito.mock(SqlValidatorImpl.class);
    Mockito.when(validator.getTypeFactory()).thenReturn(typeFactory);
    // Create a test operator binding
    TestSqlOperatorBinding binding = new TestSqlOperatorBinding(validator, call, decimal1,
        decimal2);

    Pair<RelDataType, RelDataType> result = invokeGetDecimalMultiplyBindingType(binding,
        typeFactory);

    assertEquals(SqlTypeName.DECIMAL, result.left.getSqlTypeName());
    assertEquals(SqlTypeName.DECIMAL, result.right.getSqlTypeName());
  }

  @Test void testGetDecimalMultiplyBindingTypeWithSqlCallBindingDecimalInteger() throws Exception {
    // Test SqlCallBinding with DECIMAL and INTEGER literals
    RelDataType decimalType = typeFactory.createSqlType(SqlTypeName.DECIMAL, 10, 2);
    RelDataType integerType = typeFactory.createSqlType(SqlTypeName.INTEGER);

    SqlNumericLiteral decimalLiteral = SqlNumericLiteral.createExactNumeric("123.45",
        SqlParserPos.ZERO);
    SqlNumericLiteral intLiteral = SqlNumericLiteral.createExactNumeric("456", SqlParserPos.ZERO);
    SqlCall call = SqlStdOperatorTable.MULTIPLY.createCall(SqlParserPos.ZERO, decimalLiteral,
        intLiteral);

    SqlValidator validator = Mockito.mock(SqlValidatorImpl.class);
    Mockito.when(validator.getTypeFactory()).thenReturn(typeFactory);
    // Create a test operator binding
    TestSqlOperatorBinding binding = new TestSqlOperatorBinding(validator, call, decimalType,
        integerType);

    Pair<RelDataType, RelDataType> result = invokeGetDecimalMultiplyBindingType(binding,
        typeFactory);

    assertEquals(SqlTypeName.DECIMAL, result.left.getSqlTypeName());
    assertEquals(SqlTypeName.DECIMAL, result.right.getSqlTypeName());
  }

  @Test void testGetDecimalMultiplyBindingTypeWithSqlCallBindingIntegerInteger() throws Exception {
    // Test SqlCallBinding with both operands as INTEGER literals
    RelDataType int1 = typeFactory.createSqlType(SqlTypeName.INTEGER);
    RelDataType int2 = typeFactory.createSqlType(SqlTypeName.INTEGER);

    SqlNumericLiteral literal1 = SqlNumericLiteral.createExactNumeric("123", SqlParserPos.ZERO);
    SqlNumericLiteral literal2 = SqlNumericLiteral.createExactNumeric("456", SqlParserPos.ZERO);
    SqlCall call = SqlStdOperatorTable.MULTIPLY.createCall(SqlParserPos.ZERO, literal1, literal2);

    SqlValidator validator = Mockito.mock(SqlValidatorImpl.class);
    Mockito.when(validator.getTypeFactory()).thenReturn(typeFactory);
    // Create a test operator binding
    TestSqlOperatorBinding binding = new TestSqlOperatorBinding(validator, call, int1, int2);

    Pair<RelDataType, RelDataType> result = invokeGetDecimalMultiplyBindingType(binding,
        typeFactory);

    assertEquals(SqlTypeName.INTEGER, result.left.getSqlTypeName());
    assertEquals(SqlTypeName.INTEGER, result.right.getSqlTypeName());
  }

  @Test void testGetDecimalMultiplyBindingTypeWithRexCallBindingDecimalDecimal() throws Exception {
    // Test RexCallBinding with both operands as DECIMAL literals
    RexLiteral decimalLiteral1 = rexBuilder.makeExactLiteral(new BigDecimal("123.45"));
    RexLiteral decimalLiteral2 = rexBuilder.makeExactLiteral(new BigDecimal("67.89"));
    RexCall rexCall = (RexCall) rexBuilder.makeCall(SqlStdOperatorTable.MULTIPLY,
        decimalLiteral1, decimalLiteral2);

    RexCallBinding rexBinding = RexCallBinding.create(typeFactory, rexCall,
        Collections.emptyList());

    Pair<RelDataType, RelDataType> result = invokeGetDecimalMultiplyBindingType(rexBinding,
        typeFactory);

    assertEquals(SqlTypeName.DECIMAL, result.left.getSqlTypeName());
    assertEquals(SqlTypeName.DECIMAL, result.right.getSqlTypeName());
  }

  @Test void testGetDecimalMultiplyBindingTypeWithRexCallBindingDecimalInteger() throws Exception {
    // Test RexCallBinding with DECIMAL and INTEGER literals
    RexLiteral decimalLiteral = rexBuilder.makeExactLiteral(new BigDecimal("123.45"));
    RexLiteral intLiteral = rexBuilder.makeExactLiteral(new BigDecimal("456"));
    RexCall rexCall = (RexCall) rexBuilder.makeCall(SqlStdOperatorTable.MULTIPLY,
        decimalLiteral, intLiteral);

    RexCallBinding rexBinding = RexCallBinding.create(typeFactory, rexCall,
        Collections.emptyList());

    Pair<RelDataType, RelDataType> result = invokeGetDecimalMultiplyBindingType(rexBinding,
        typeFactory);

    assertEquals(SqlTypeName.DECIMAL, result.left.getSqlTypeName());
    assertEquals(SqlTypeName.DECIMAL, result.right.getSqlTypeName());
  }

  @Test void testGetDecimalMultiplyBindingTypeWithRexCallBindingIntegerInteger() throws Exception {
    // Test RexCallBinding with both operands as INTEGER literals
    RexLiteral intLiteral1 = rexBuilder.makeExactLiteral(new BigDecimal("123"));
    RexLiteral intLiteral2 = rexBuilder.makeExactLiteral(new BigDecimal("456"));
    RexCall rexCall = (RexCall) rexBuilder.makeCall(SqlStdOperatorTable.MULTIPLY,
        intLiteral1, intLiteral2);

    RexCallBinding rexBinding = RexCallBinding.create(typeFactory, rexCall,
        Collections.emptyList());

    Pair<RelDataType, RelDataType> result = invokeGetDecimalMultiplyBindingType(rexBinding,
        typeFactory);

    assertEquals(SqlTypeName.INTEGER, result.left.getSqlTypeName());
    assertEquals(SqlTypeName.INTEGER, result.right.getSqlTypeName());
  }

  @Test void testGetDecimalMultiplyBindingTypeWithRexCallBindingIntegerConversion()
      throws Exception {
    // Test integer to decimal conversion in RexCallBinding
    RexLiteral intLiteral1 = rexBuilder.makeExactLiteral(new BigDecimal("1231234567890"));
    RexLiteral intLiteral2 = rexBuilder.makeExactLiteral(new BigDecimal("12345678904567"));
    RexCall rexCall = (RexCall) rexBuilder.makeCall(SqlStdOperatorTable.MULTIPLY,
        intLiteral1, intLiteral2);

    RexCallBinding rexBinding = RexCallBinding.create(typeFactory, rexCall,
        Collections.emptyList());

    Pair<RelDataType, RelDataType> result = invokeGetDecimalMultiplyBindingType(rexBinding,
        typeFactory);

    // Check precision calculation for integer conversion
    assertEquals(SqlTypeName.BIGINT, result.left.getSqlTypeName());

    assertEquals(SqlTypeName.BIGINT, result.right.getSqlTypeName());
  }

  @Test void testGetDecimalMultiplyBindingTypeWithFallbackBinding() throws Exception {
    // Test fallback to original types for non-SqlCallBinding/RexCallBinding
    RelDataType int1 = typeFactory.createSqlType(SqlTypeName.INTEGER);
    RelDataType int2 = typeFactory.createSqlType(SqlTypeName.INTEGER);

    TestOperatorBinding fallbackBinding = new TestOperatorBinding(typeFactory, int1, int2);

    Pair<RelDataType, RelDataType> result = invokeGetDecimalMultiplyBindingType(fallbackBinding,
        typeFactory);

    // Should return original types
    assertEquals(SqlTypeName.INTEGER, result.left.getSqlTypeName());
    assertEquals(SqlTypeName.INTEGER, result.right.getSqlTypeName());
  }

  @Test void testGetDecimalMultiplyBindingTypeWithFallbackBinding2() throws Exception {
    // Test fallback to original types for non-SqlCallBinding/RexCallBinding
    RelDataType int1 = typeFactory.createSqlType(SqlTypeName.DECIMAL);
    RelDataType int2 = typeFactory.createSqlType(SqlTypeName.DECIMAL);

    TestOperatorBinding fallbackBinding = new TestOperatorBinding(typeFactory, int1, int2);

    Pair<RelDataType, RelDataType> result = invokeGetDecimalMultiplyBindingType(fallbackBinding,
        typeFactory);

    // Should return original types
    assertEquals(SqlTypeName.DECIMAL, result.left.getSqlTypeName());
    assertEquals(SqlTypeName.DECIMAL, result.right.getSqlTypeName());
  }

  @Test void testGetDecimalMultiplyBindingTypeWithFallbackBinding3() throws Exception {
    // Test fallback to original types for non-SqlCallBinding/RexCallBinding
    RelDataType int1 = typeFactory.createSqlType(SqlTypeName.DECIMAL);
    RelDataType int2 = typeFactory.createSqlType(SqlTypeName.INTEGER);

    TestOperatorBinding fallbackBinding = new TestOperatorBinding(typeFactory, int1, int2);

    Pair<RelDataType, RelDataType> result = invokeGetDecimalMultiplyBindingType(fallbackBinding,
        typeFactory);

    // Should return original types
    assertEquals(SqlTypeName.DECIMAL, result.left.getSqlTypeName());
    assertEquals(SqlTypeName.INTEGER, result.right.getSqlTypeName());
  }

  @Test void testGetDecimalMultiplyBindingTypeWithBigIntConversion() throws Exception {
    // Test BIGINT to decimal conversion in RexCallBinding
    RexLiteral bigIntLiteral = rexBuilder.makeExactLiteral(new BigDecimal("123456789"));
    RexLiteral decimalLiteral = rexBuilder.makeExactLiteral(new BigDecimal("123.45"));
    RexCall rexCall = (RexCall) rexBuilder.makeCall(SqlStdOperatorTable.MULTIPLY,
        bigIntLiteral, decimalLiteral);

    RexCallBinding rexBinding = RexCallBinding.create(typeFactory, rexCall,
        Collections.emptyList());

    Pair<RelDataType, RelDataType> result = invokeGetDecimalMultiplyBindingType(rexBinding,
        typeFactory);

    assertEquals(SqlTypeName.DECIMAL, result.left.getSqlTypeName());
    assertEquals(9, result.left.getPrecision()); // 123456789 has 9 digits
    assertEquals(0, result.left.getScale());

    assertEquals(SqlTypeName.DECIMAL, result.right.getSqlTypeName());
  }

  @Test void testGetDecimalMultiplyBindingTypeWithZeroValue() throws Exception {
    // Test with zero value (special case for precision calculation)
    RexLiteral zeroLiteral = rexBuilder.makeExactLiteral(new BigDecimal("0"));
    RexLiteral decimalLiteral = rexBuilder.makeExactLiteral(new BigDecimal("123.45"));
    RexCall rexCall = (RexCall) rexBuilder.makeCall(SqlStdOperatorTable.MULTIPLY,
        zeroLiteral, decimalLiteral);

    RexCallBinding rexBinding = RexCallBinding.create(typeFactory, rexCall,
        Collections.emptyList());

    Pair<RelDataType, RelDataType> result = invokeGetDecimalMultiplyBindingType(rexBinding,
        typeFactory);

    assertEquals(SqlTypeName.DECIMAL, result.left.getSqlTypeName());
    assertEquals(1, result.left.getPrecision()); // 0 should have precision 1
    assertEquals(0, result.left.getScale());

    assertEquals(SqlTypeName.DECIMAL, result.right.getSqlTypeName());
  }

  @Test void testGetDecimalMultiplyBindingTypeWithNegativeValues() throws Exception {
    // Test with negative values
    RexLiteral negLiteral = rexBuilder.makeExactLiteral(new BigDecimal("-123"));
    RexLiteral decimalLiteral = rexBuilder.makeExactLiteral(new BigDecimal("456.78"));
    RexCall rexCall = (RexCall) rexBuilder.makeCall(SqlStdOperatorTable.MULTIPLY,
        negLiteral, decimalLiteral);

    RexCallBinding rexBinding = RexCallBinding.create(typeFactory, rexCall,
        Collections.emptyList());

    Pair<RelDataType, RelDataType> result = invokeGetDecimalMultiplyBindingType(rexBinding,
        typeFactory);

    assertEquals(SqlTypeName.DECIMAL, result.left.getSqlTypeName());
    assertEquals(4, result.left.getPrecision()); // -123 has 4 digits (ignoring sign)
    assertEquals(0, result.left.getScale());

    assertEquals(SqlTypeName.DECIMAL, result.right.getSqlTypeName());
  }

  @Test void testGetDecimalMultiplyBindingTypeWithNegativeValuesSqlBinding() throws Exception {
    RelDataType decimalType = typeFactory.createSqlType(SqlTypeName.DECIMAL, 10, 2);
    RelDataType integerType = typeFactory.createSqlType(SqlTypeName.INTEGER);

    SqlNumericLiteral decimalLiteral = SqlNumericLiteral.createExactNumeric("123.45",
        SqlParserPos.ZERO);
    SqlNumericLiteral intLiteral = SqlNumericLiteral.createExactNumeric("-456", SqlParserPos.ZERO);
    SqlCall call = SqlStdOperatorTable.MULTIPLY.createCall(SqlParserPos.ZERO, decimalLiteral,
        intLiteral);

    SqlValidator validator = Mockito.mock(SqlValidatorImpl.class);
    Mockito.when(validator.getTypeFactory()).thenReturn(typeFactory);
    // Create a test operator binding
    TestSqlOperatorBinding binding = new TestSqlOperatorBinding(validator, call, decimalType,
        integerType);
    Pair<RelDataType, RelDataType> result = invokeGetDecimalMultiplyBindingType(binding,
        typeFactory);

    assertEquals(SqlTypeName.DECIMAL, result.left.getSqlTypeName());

    assertEquals(SqlTypeName.DECIMAL, result.right.getSqlTypeName());
    assertEquals(4, result.right.getPrecision()); // -456 has 4 digits (ignoring sign)
    assertEquals(0, result.right.getScale());
  }

  // Helper method to invoke private getDecimalMultiplyBindingType method using reflection
  @SuppressWarnings("unchecked")
  private Pair<RelDataType, RelDataType> invokeGetDecimalMultiplyBindingType(
      SqlOperatorBinding binding, RelDataTypeFactory typeFactory) throws Exception {
    Method method = ReturnTypes.class.getDeclaredMethod("getDecimalMultiplyBindingType",
        SqlOperatorBinding.class, RelDataTypeFactory.class);
    method.setAccessible(true);
    return (Pair<RelDataType, RelDataType>) method.invoke(null, binding, typeFactory);
  }

  /**
   * Simple test implementation of SqlOperatorBinding for fallback testing.
   */
  private static class TestOperatorBinding extends SqlOperatorBinding {
    private final RelDataType[] operandTypes;

    TestOperatorBinding(RelDataTypeFactory typeFactory, RelDataType... operandTypes) {
      super(typeFactory, SqlStdOperatorTable.MULTIPLY);
      this.operandTypes = operandTypes;
    }

    @Override public int getOperandCount() {
      return operandTypes.length;
    }

    @Override public RelDataType getOperandType(int ordinal) {
      return operandTypes[ordinal];
    }

    @Override public CalciteException newError(Resources.ExInst<SqlValidatorException> e) {
      return new CalciteException(e.str(), null);
    }
  }

  /**
   * Simple test implementation of SqlOperatorBinding for testing purposes.
   */
  private static class TestSqlOperatorBinding extends SqlCallBinding {
    private final RelDataType[] operandTypes;
    private final SqlCall call;

    TestSqlOperatorBinding(SqlValidator validator, SqlCall call, RelDataType... operandTypes) {
      super(validator, null, call);
      this.operandTypes = operandTypes;
      this.call = call;
    }

    @Override public int getOperandCount() {
      return operandTypes.length;
    }

    @Override public RelDataType getOperandType(int ordinal) {
      return operandTypes[ordinal];
    }

    @Override public CalciteException newError(Resources.ExInst<SqlValidatorException> e) {
      return new CalciteException(e.str(), null);
    }
  }

}
