/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.criteria.Expression;

import org.hibernate.query.criteria.JpaSearchedCase;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * Models what ANSI SQL terms a <tt>searched case expression</tt>.  This is a <tt>CASE</tt> expression
 * in the form<pre>
 * CASE
 *     WHEN [firstCondition] THEN [firstResult]
 *     WHEN [secondCondition] THEN [secondResult]
 *     ELSE [defaultResult]
 * END
 * </pre>
 *
 * @author Steve Ebersole
 */
public class SearchedCase<T> extends AbstractExpression<T> implements JpaSearchedCase<T> {
	private List<WhenClause> whenClauses = new ArrayList<>();
	private ExpressionImplementor<? extends T> otherwiseResult;

	public SearchedCase(CriteriaNodeBuilder criteriaBuilder) {
		super( ( JavaTypeDescriptor<T>) null, criteriaBuilder );
	}

	public SearchedCase(Class<T> javaType, CriteriaNodeBuilder criteriaBuilder) {
		super( javaType, criteriaBuilder );
	}

	public SearchedCase<T> when(Expression<Boolean> condition, T result) {
		return when( condition, buildLiteral( result ) );
	}

	@SuppressWarnings({"unchecked"})
	private LiteralExpression<T> buildLiteral(T result) {
		final Class type = result != null
				? result.getClass()
				: getJavaType();
		return new LiteralExpression<>( type, result, nodeBuilder() );
	}

	@SuppressWarnings({"unchecked"})
	public SearchedCase<T> when(Expression<Boolean> condition, Expression<? extends T> result) {
		WhenClause whenClause = new WhenClause( (ExpressionImplementor) condition, (ExpressionImplementor) result );
		whenClauses.add( whenClause );
		setJavaType( result.getJavaType() );
		return this;
	}

	public ExpressionImplementor<T> otherwise(T result) {
		return otherwise( buildLiteral( result ) );
	}

	@SuppressWarnings({"unchecked"})
	public ExpressionImplementor<T> otherwise(Expression<? extends T> result) {
		this.otherwiseResult = (ExpressionImplementor) result;
		setJavaType( result.getJavaType() );
		return this;
	}

	public ExpressionImplementor<? extends T> getOtherwiseResult() {
		return otherwiseResult;
	}

	public List<WhenClause> getWhenClauses() {
		return whenClauses;
	}

	@Override
	public <R> R accept(CriteriaVisitor visitor) {
		return visitor.visitSearchedCase( this );
	}

	public class WhenClause {
		private final ExpressionImplementor<Boolean> condition;
		private final ExpressionImplementor<? extends T> result;

		public WhenClause(ExpressionImplementor<Boolean> condition, ExpressionImplementor<? extends T> result) {
			this.condition = condition;
			this.result = result;
		}

		public ExpressionImplementor<Boolean> getCondition() {
			return condition;
		}

		public ExpressionImplementor<? extends T> getResult() {
			return result;
		}
	}
}
