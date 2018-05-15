/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.tree.spi.expression;

import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.spi.SqlExpressable;
import org.hibernate.sql.results.internal.domain.basic.BasicResultImpl;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationContext;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.DomainResultProducer;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class UnaryOperation implements Expression, SqlExpressable, DomainResultProducer {

	public enum Operator {
		PLUS,
		MINUS;
	}
	private final Operator operator;

	private final Expression operand;
	private final SqlExpressableType type;

	public UnaryOperation(Operator operator, Expression operand, SqlExpressableType type) {
		this.operator = operator;
		this.operand = operand;
		this.type = type;
	}

	public Operator getOperator() {
		return operator;
	}

	public Expression getOperand() {
		return operand;
	}

	@Override
	public SqlExpressableType getExpressableType() {
		return type;
	}

	@Override
	public SqlSelection createSqlSelection(
			int jdbcPosition,
			int valuesArrayPosition,
			BasicJavaDescriptor javaTypeDescriptor,
			TypeConfiguration typeConfiguration) {
		return new SqlSelectionImpl(
				jdbcPosition,
				valuesArrayPosition,
				this,
				getExpressableType()
		);
	}

	@Override
	public void accept(SqlAstWalker  walker) {
		walker.visitUnaryOperationExpression( this );
	}

	@Override
	public DomainResult createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState,
			DomainResultCreationContext creationContext) {
		return new BasicResultImpl(
				resultVariable,
				creationState.getSqlExpressionResolver().resolveSqlSelection(
						this,
						getType().getJavaTypeDescriptor(),
						creationContext.getSessionFactory().getTypeConfiguration()
				),
				getType()
		);
	}
}
