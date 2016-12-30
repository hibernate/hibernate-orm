/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal.expression;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaBuilder.Coalesce;
import javax.persistence.criteria.Expression;

import org.hibernate.query.criteria.JpaCoalesce;
import org.hibernate.query.criteria.JpaExpressionImplementor;
import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.ParameterRegistry;
import org.hibernate.sqm.parser.criteria.tree.CriteriaVisitor;
import org.hibernate.sqm.query.expression.SqmExpression;

/**
 * Models an ANSI SQL <tt>COALESCE</tt> expression.  <tt>COALESCE</tt> is a specialized <tt>CASE</tt> statement.
 *
 * @author Steve Ebersole
 */
public class CoalesceExpression<T> extends AbstractExpression<T> implements JpaCoalesce<T>, Serializable {
	private final List<JpaExpressionImplementor<? extends T>> expressions;
	private Class<T> javaType;

	public CoalesceExpression(CriteriaBuilderImpl criteriaBuilder) {
		this( criteriaBuilder, null );
	}

	public CoalesceExpression(
			CriteriaBuilderImpl criteriaBuilder,
			Class<T> javaType) {
		super( criteriaBuilder, javaType );
		this.javaType = javaType;
		this.expressions = new ArrayList<>();
	}

	@Override
	public Class<T> getJavaType() {
		return javaType;
	}

	public JpaCoalesce<T> value(T value) {
		return value( new LiteralExpression<T>( criteriaBuilder(), value ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	public JpaCoalesce<T> value(Expression<? extends T> value) {
		criteriaBuilder().checkIsJpaExpression( value );
		value( (JpaExpressionImplementor<? extends T>) value );
		return null;
	}

	@SuppressWarnings({ "unchecked" })
	public JpaCoalesce<T> value(JpaExpressionImplementor<? extends T> value) {
		expressions.add( value );
		if ( javaType == null ) {
			javaType = (Class<T>) value.getJavaType();
		}
		return this;
	}

	public List<JpaExpressionImplementor<? extends T>> getExpressions() {
		return expressions;
	}

	public void registerParameters(ParameterRegistry registry) {
		for ( Expression expression : getExpressions() ) {
			Helper.possibleParameter(expression, registry);
		}
	}

	@Override
	public SqmExpression visitExpression(CriteriaVisitor visitor) {
		return visitor.visitCoalesce( expressions );
	}
}
