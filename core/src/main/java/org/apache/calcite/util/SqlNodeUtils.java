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

import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNumericLiteral;
import org.apache.calcite.sql.SqlOperatorBinding;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.sql.type.SqlTypeUtil;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Utility class for working with {@link SqlNode} and {@link RexNode} objects.
 *
 * <p>This class provides various static methods to analyze and validate SQL nodes,
 * particularly focusing on numeric and decimal constant detection. It includes methods
 * to check if nodes represent decimal constants, numeric literals, or complex expressions
 * containing numeric values.</p>
 *
 * <p>The utility supports both {@link SqlNode} (parse tree representation) and
 * {@link RexNode} (relational expression representation) objects, providing consistent
 * behavior across different stages of SQL processing.</p>
 */
public class SqlNodeUtils {

  /**
   * Private constructor to prevent instantiation of this utility class.
   * All methods in this class are static and should be accessed directly.
   */
  private SqlNodeUtils() {
  }

  /**
   * Checks if the given {@link SqlNode} represents a decimal constant.
   *
   * <p>A decimal constant is defined as a {@link SqlNumericLiteral} with DECIMAL type
   * that is not an integer literal. This method specifically excludes integer values
   * even if they are stored as DECIMAL type.</p>
   *
   * @param node the SQL node to check, may be null
   * @return true if the node is a decimal constant (non-integer DECIMAL), false otherwise
   */
  public static boolean isDecimalConstant(SqlNode node) {
    if (node instanceof SqlNumericLiteral) {
      SqlNumericLiteral numericLiteral = (SqlNumericLiteral) node;
      SqlTypeName typeName = numericLiteral.getTypeName();
      // Check if it's a DECIMAL type and not an integer literal
      if (typeName == null) {
        return false;
      }
      return typeName == SqlTypeName.DECIMAL && !numericLiteral.isInteger();
    }

    return false;
  }

  /**
   * Checks if the given {@link SqlNode} represents a decimal or integer constant.
   *
   * <p>Unlike {@link #isDecimalConstant(SqlNode)}, this method includes both decimal
   * and integer values. During SQL parsing, integers are converted to DECIMAL type,
   * so this method checks for DECIMAL type regardless of whether it's an integer
   * or decimal value.</p>
   *
   * @param node the SQL node to check, may be null
   * @return true if the node is a DECIMAL type numeric literal (including integers), false
   * otherwise
   */
  public static boolean isDecimalOrIntegerConstant(SqlNode node) {
    if (node instanceof SqlNumericLiteral) {
      SqlNumericLiteral numericLiteral = (SqlNumericLiteral) node;
      SqlTypeName typeName = numericLiteral.getTypeName();
      // When parsing into a **SqlNode**, integers are converted to **DECIMAL** type.
      if (typeName == null) {
        return false;
      }
      return typeName == SqlTypeName.DECIMAL;
    }

    return false;
  }

  /**
   * Checks if the given {@link RexNode} represents a decimal constant.
   *
   * <p>This method checks if the node is a {@link RexLiteral} with DECIMAL type
   * or any approximate numeric type (like FLOAT, DOUBLE). Unlike the SqlNode
   * version, this includes approximate numeric types as well.</p>
   *
   * @param node the Rex node to check, may be null
   * @return true if the node is a decimal or approximate numeric constant, false otherwise
   */
  public static boolean isDecimalConstant(RexNode node) {
    if (node instanceof RexLiteral) {
      RexLiteral literal = (RexLiteral) node;
      RelDataType type = literal.getType();
      // Check if it's a DECIMAL type and not an integer literal
      return type.getSqlTypeName() == SqlTypeName.DECIMAL || SqlTypeUtil.isApproximateNumeric(type);
    }
    return false;
  }

  /**
   * Checks if the given {@link SqlNode} is a numeric literal.
   *
   * <p>This is a simple type check that returns true if the node is an instance
   * of {@link SqlNumericLiteral}, regardless of the specific numeric type.</p>
   *
   * @param node the SQL node to check, may be null
   * @return true if the node is a numeric literal, false otherwise
   */
  public static boolean isNumericLiteral(SqlNode node) {
    return node instanceof SqlNumericLiteral;
  }

  /**
   * Checks if the given {@link RexNode} is a numeric literal.
   *
   * <p>This method checks if the node is a {@link RexLiteral} with a numeric type.
   * It uses {@link SqlTypeUtil#isNumeric(RelDataType)} to determine if the type
   * is numeric, which includes all numeric types like INTEGER, DECIMAL, FLOAT, etc.</p>
   *
   * @param node the Rex node to check, may be null
   * @return true if the node is a numeric literal, false otherwise
   */
  public static boolean isNumericLiteral(RexNode node) {
    if (node instanceof RexLiteral) {
      RexLiteral literal = (RexLiteral) node;
      RelDataType type = literal.getType();
      return SqlTypeUtil.isNumeric(type);
    }
    return false;
  }

  /**
   * Checks if the operand at the specified ordinal in a {@link SqlOperatorBinding}
   * is a numeric literal.
   *
   * <p>This method verifies that the operand is both a literal and has a numeric type.
   * It's commonly used in operator validation to ensure operands are numeric literals.</p>
   *
   * @param binding the operator binding containing the operands
   * @param ordinal the zero-based index of the operand to check
   * @return true if the operand at the specified position is a numeric literal, false otherwise
   */
  public static boolean isNumericLiteral(SqlOperatorBinding binding, int ordinal) {
    return binding.isOperandLiteral(ordinal, false) && SqlTypeUtil.isNumeric(binding.getOperandType(ordinal));
  }

  /**
   * Checks if the given {@link RexNode} represents a decimal constant expression.
   *
   * <p>This method performs a deep analysis of the expression tree to determine if
   * it contains only numeric literals and at least one decimal constant. It traverses
   * binary arithmetic operations (like +, -, *, /) and checks all operands.</p>
   *
   * <p>The method returns true only if:
   * <ul>
   *   <li>All leaf nodes in the expression are numeric literals</li>
   *   <li>At least one leaf node is a decimal constant</li>
   *   <li>All intermediate nodes are binary arithmetic operations</li>
   * </ul>
   * </p>
   *
   * <p>For example, this would return true for expressions like:
   * <ul>
   *   <li>1.5 + 2 (contains decimal 1.5)</li>
   *   <li>3.14 * 2 - 1 (contains decimal 3.14)</li>
   * </ul>
   * But false for:
   * <ul>
   *   <li>1 + 2 (no decimal constants)</li>
   *   <li>1.5 + column_name (contains non-literal)</li>
   * </ul>
   * </p>
   *
   * @param node the Rex node to check, may be null
   * @return true if the expression contains only numeric literals and at least one decimal constant
   */
  public static boolean isDecimalConstantRexNode(RexNode node) {
    if (node == null) {
      return false;
    }

    // If the node itself is a decimal constant, return true immediately
    if (isDecimalConstant(node)) {
      return true;
    }

    Queue<RexNode> nodesToCheck = new ArrayDeque<>();
    // Add all operands of the initial RexCall to the queue for checking
    nodesToCheck.add(node);

    boolean allNumeric = true;
    boolean anyDecimal = false;

    while (!nodesToCheck.isEmpty()) {
      RexNode currentNode = nodesToCheck.poll();
      // Check if the current node is a decimal constant
      if (isDecimalConstant(currentNode)) {
        anyDecimal = true;
        continue; // This node is a decimal constant, check the next one
      }

      // Check if the current node is a numeric literal
      if (isNumericLiteral(currentNode)) {
        continue; // This node is numeric, check the next one
      }

      // If the current node is a RexCall with binary arithmetic operations, add its operands to
      // the queue
      if (currentNode instanceof RexCall) {
        RexCall call = (RexCall) currentNode;
        SqlKind kind = call.getKind();
        if (kind.belongsTo(SqlKind.BINARY_ARITHMETIC)) {
          // Add all operands to the queue for checking
          nodesToCheck.addAll(call.getOperands());
          continue;
        }
      }

      // If we reach here, the node is not a decimal constant, not a numeric literal,
      // and not a valid arithmetic call
      allNumeric = false;
      break; // Early exit if we find a non-numeric node
    }

    // Return true only if all nodes are numeric and at least one is a decimal constant
    return allNumeric && anyDecimal;
  }

  /**
   * Checks if the given {@link RexNode} represents an expression containing only numeric literals.
   *
   * <p>This method performs a deep analysis of the expression tree to determine if
   * it contains only numeric literals. It traverses binary arithmetic operations
   * and checks all operands to ensure they are all numeric literals.</p>
   *
   * <p>Unlike {@link #isDecimalConstantRexNode(RexNode)}, this method doesn't require
   * at least one decimal constant - it accepts expressions with only integer literals as well.</p>
   *
   * <p>For example, this would return true for expressions like:
   * <ul>
   *   <li>1 + 2 (both integer literals)</li>
   *   <li>1.5 * 3.14 (both decimal literals)</li>
   *   <li>10 - 5 + 2 (multiple integer literals)</li>
   * </ul>
   * But false for:
   * <ul>
   *   <li>1 + column_name (contains non-literal)</li>
   *   <li>function_call(5) (contains function call)</li>
   * </ul>
   * </p>
   *
   * @param node the Rex node to check, may be null
   * @return true if the expression contains only numeric literals, false otherwise
   */
  public static boolean isNumericLiteralRexNode(RexNode node) {
    if (node == null) {
      return false;
    }

    Queue<RexNode> nodesToCheck = new ArrayDeque<>();
    nodesToCheck.add(node);

    while (!nodesToCheck.isEmpty()) {
      RexNode currentNode = nodesToCheck.poll();

      if (isNumericLiteral(currentNode)) {
        continue; // This node is numeric, check the next one
      }

      if (currentNode instanceof RexCall) {
        RexCall call = (RexCall) currentNode;
        SqlKind kind = call.getKind();
        if (kind.belongsTo(SqlKind.BINARY_ARITHMETIC)) {
          // Add all operands to the queue for checking
          nodesToCheck.addAll(call.getOperands());
          continue;
        }
      }

      // If we reach here, the node is not a numeric literal and not a valid arithmetic call
      return false;
    }

    // If we've checked all nodes and none failed the numeric test, return true
    return true;
  }

  /**
   * Checks if the given {@link SqlNode} represents a decimal constant expression.
   *
   * <p>This method is the SqlNode equivalent of {@link #isDecimalConstantRexNode(RexNode)}.
   * It performs a deep analysis of the SQL expression tree to determine if
   * it contains only numeric literals and at least one decimal constant.</p>
   *
   * <p>The method traverses binary arithmetic operations and checks all operands.
   * It returns true only if all leaf nodes are numeric literals and at least one
   * is a decimal constant.</p>
   *
   * <p>This method is typically used during SQL parsing and validation stages,
   * before the SQL is converted to relational expressions.</p>
   *
   * @param node the SQL node to check, may be null
   * @return true if the expression contains only numeric literals and at least one decimal constant
   */
  public static boolean isDecimalConstantSqlNode(SqlNode node) {
    if (node == null) {
      return false;
    }

    // If the node itself is a decimal constant, return true immediately
    if (isDecimalConstant(node)) {
      return true;
    }

    Queue<SqlNode> nodesToCheck = new ArrayDeque<>();
    nodesToCheck.add(node);
    boolean allNumeric = true;
    boolean anyDecimal = false;

    while (!nodesToCheck.isEmpty()) {
      SqlNode currentNode = nodesToCheck.poll();

      // Check if the current node is a decimal constant
      if (isDecimalConstant(currentNode)) {
        anyDecimal = true;
        continue; // This node is a decimal constant, check the next one
      }

      // Check if the current node is a numeric literal
      if (isNumericLiteral(currentNode)) {
        continue; // This node is numeric, check the next one
      }

      // If the current node is a SqlCall with binary arithmetic operations, add its operands to
      // the queue
      if (currentNode instanceof SqlCall) {
        SqlCall call = (SqlCall) currentNode;
        SqlKind kind = call.getKind();
        if (kind.belongsTo(SqlKind.BINARY_ARITHMETIC)) {
          // Add all operands to the queue for checking
          nodesToCheck.addAll(call.getOperandList());
          continue;
        }
      }

      // If we reach here, the node is not a decimal constant, not a numeric literal,
      // and not a valid arithmetic call
      allNumeric = false;
      break; // Early exit if we find a non-numeric node
    }

    // Return true only if all nodes are numeric and at least one is a decimal constant
    return allNumeric && anyDecimal;
  }

  /**
   * Checks if the given {@link SqlNode} represents an expression containing only numeric literals.
   *
   * <p>This method is the SqlNode equivalent of {@link #isNumericLiteralRexNode(RexNode)}.
   * It performs a deep analysis of the SQL expression tree to determine if
   * it contains only numeric literals.</p>
   *
   * <p>The method traverses binary arithmetic operations and checks all operands
   * to ensure they are all numeric literals. It accepts expressions with both
   * integer and decimal literals.</p>
   *
   * <p>This method is typically used during SQL parsing and validation to identify
   * constant expressions that can be evaluated at compile time.</p>
   *
   * @param node the SQL node to check, may be null
   * @return true if the expression contains only numeric literals, false otherwise
   */
  public static boolean isNumericLiteralSqlNode(SqlNode node) {
    if (node == null) {
      return false;
    }

    Queue<SqlNode> nodesToCheck = new ArrayDeque<>();
    nodesToCheck.add(node);

    while (!nodesToCheck.isEmpty()) {
      SqlNode currentNode = nodesToCheck.poll();

      if (isNumericLiteral(currentNode)) {
        continue; // This node is numeric, check the next one
      }

      if (currentNode instanceof SqlCall) {
        SqlCall call = (SqlCall) currentNode;
        SqlKind kind = call.getKind();
        if (kind.belongsTo(SqlKind.BINARY_ARITHMETIC)) {
          // Add all operands to the queue for checking
          nodesToCheck.addAll(call.getOperandList());
          continue;
        }
      }

      // If we reach here, the node is not a numeric literal and not a valid arithmetic call
      return false;
    }

    // If we've checked all nodes and none failed the numeric test, return true
    return true;
  }
}
