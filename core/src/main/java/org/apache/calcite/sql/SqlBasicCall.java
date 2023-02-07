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
package org.apache.calcite.sql;

import org.apache.calcite.sql.parser.SqlParserPos;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.apache.calcite.linq4j.Nullness.castNonNull;

/**
 * Implementation of {@link SqlCall} that keeps its operands in an array.
 */
public class SqlBasicCall extends SqlCall {
  private SqlOperator operator;
  /**
   * [CALCITE-4795] Calcite 1.30 change operands to operandList which is ImmutableNullableList.
   * The deep copy operation will cause the operator and operandList reference objects
   * to be inconsistent during the rewrite call, which will eventually lead to
   * an exception, thus changing the deep copy of the immutable collection to
   * a modification of the object reference.
   * @see #setOperand(int, SqlNode)
    */
  private final @Nullable SqlNode[] operands;
  private final @Nullable SqlLiteral functionQuantifier;

  @Deprecated // to be removed before 2.0
  public SqlBasicCall(
      SqlOperator operator,
      @Nullable SqlNode[] operands,
      SqlParserPos pos) {
    this(operator, operands, pos, null);
  }

  /** Creates a SqlBasicCall.
   *
   * <p>It is not expanded; call {@link #withExpanded withExpanded(true)}
   * to expand. */
  public SqlBasicCall(
      SqlOperator operator,
      List<? extends @Nullable SqlNode> operandList,
      SqlParserPos pos) {
    this(operator, operandList.toArray(SqlNode.EMPTY_ARRAY), pos, null);
  }

  /** Creates a SqlBasicCall with an optional function qualifier.
   *
   * <p>It is not expanded; call {@link #withExpanded withExpanded(true)}
   * to expand. */
  public SqlBasicCall(
      SqlOperator operator,
      @Nullable SqlNode[] operands,
      SqlParserPos pos,
      @Nullable SqlLiteral functionQualifier) {
    super(pos);
    this.operator = Objects.requireNonNull(operator, "operator");
    this.operands = operands;
    this.functionQuantifier = functionQualifier;
  }

  @Override public SqlKind getKind() {
    return operator.getKind();
  }

  /** Sets whether this call is expanded.
   *
   * @see #isExpanded() */
  public SqlCall withExpanded(boolean expanded) {
    return !expanded
        ? this
        : new ExpandedBasicCall(operator, operands, pos,
            functionQuantifier);
  }

  @Override public void setOperand(int i, @Nullable SqlNode operand) {
    operands[i] = operand;
  }

  /** Sets the operator (or function) that is being called.
   *
   * <p>This method is used by the validator to set a more refined version of
   * the same operator (for instance, a version where overloading has been
   * resolved); use with care. */
  public void setOperator(SqlOperator operator) {
    this.operator = Objects.requireNonNull(operator, "operator");
  }

  @Override public SqlOperator getOperator() {
    return operator;
  }

  @SuppressWarnings("nullness")
  @Override public List<SqlNode> getOperandList() {
    return Arrays.asList(operands);
  }

  @SuppressWarnings("unchecked")
  @Override public <S extends SqlNode> S operand(int i) {
    return (S) castNonNull(operands[i]);
  }

  @Override public int operandCount() {
    return operands.length;
  }

  @Override public @Nullable SqlLiteral getFunctionQuantifier() {
    return functionQuantifier;
  }

  @Override public SqlNode clone(SqlParserPos pos) {
    return getOperator().createCall(getFunctionQuantifier(), pos, operands);
  }

  /** Sub-class of {@link org.apache.calcite.sql.SqlBasicCall}
   * for which {@link #isExpanded()} returns true. */
  private static class ExpandedBasicCall extends SqlBasicCall {
    ExpandedBasicCall(SqlOperator operator,
        @Nullable SqlNode[] operands, SqlParserPos pos,
        @Nullable SqlLiteral functionQualifier) {
      super(operator, operands, pos, functionQualifier);
    }

    @Override public boolean isExpanded() {
      return true;
    }

    @Override public SqlCall withExpanded(boolean expanded) {
      return expanded
          ? this
          : new SqlBasicCall(getOperator(), getOperandList().toArray(SqlNode.EMPTY_ARRAY), pos,
              getFunctionQuantifier());
    }
  }
}
