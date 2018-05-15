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
public class BinaryArithmeticExpression
		implements Expression, SqlExpressable, DomainResultProducer {
	private final Operation operation;
	private final Expression lhsOperand;
	private final Expression rhsOperand;
	private final SqlExpressableType resultType;

	public BinaryArithmeticExpression(
			Operation operation,
			Expression lhsOperand,
			Expression rhsOperand,
			SqlExpressableType resultType) {
		this.operation = operation;
		this.lhsOperand = lhsOperand;
		this.rhsOperand = rhsOperand;
		this.resultType = resultType;
	}

	@Override
	public SqlExpressableType getExpressableType() {
		return resultType;
	}

	@Override
	public SqlExpressableType getType() {
		return resultType;
	}

	@Override
	public SqlSelection createSqlSelection(
			int jdbcPosition,
			int valuesArrayPosition, BasicJavaDescriptor javaTypeDescriptor,
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
		walker.visitBinaryArithmeticExpression( this );
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
				resultType
		);
	}

	public enum Operation {
		ADD {
			@Override
			public String getOperatorSqlText() {
				return "+";
			}
		},
		SUBTRACT {
			@Override
			public String getOperatorSqlText() {
				return "-";
			}
		},
		MULTIPLY {
			@Override
			public String getOperatorSqlText() {
				return "*";
			}
		},
		DIVIDE {
			@Override
			public String getOperatorSqlText() {
				return "/";
			}
		},
		QUOT {
			@Override
			public String getOperatorSqlText() {
				return "/";
			}
		};

		public abstract String getOperatorSqlText();
	}

	/**
	 * Get the left-hand operand.
	 *
	 * @return The left-hand operand.
	 */
	public Expression getLeftHandOperand() {
		return lhsOperand;
	}

	/**
	 * Get the operation
	 *
	 * @return The operation
	 */
	public Operation getOperation() {
		return operation;
	}

	/**
	 * Get the right-hand operand.
	 *
	 * @return The right-hand operand.
	 */
	public Expression getRightHandOperand() {
		return rhsOperand;
	}
}
