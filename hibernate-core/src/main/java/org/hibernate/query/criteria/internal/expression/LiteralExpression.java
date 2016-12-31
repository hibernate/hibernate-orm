/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal.expression;

import java.io.Serializable;

import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaExpressionImplementor;
import org.hibernate.query.criteria.internal.ParameterRegistry;
import org.hibernate.sqm.domain.DomainReference;
import org.hibernate.sqm.parser.criteria.tree.CriteriaVisitor;
import org.hibernate.sqm.query.expression.SqmExpression;

/**
 * Represents a literal expression.
 *
 * @author Steve Ebersole
 */
public class LiteralExpression<T> extends AbstractExpression<T> implements JpaExpressionImplementor<T>, Serializable {
	private Object literal;
	private boolean wasReset;

	@SuppressWarnings({ "unchecked" })
	public LiteralExpression(HibernateCriteriaBuilder criteriaBuilder, T literal) {
		this( criteriaBuilder, (Class<T>) determineClass( literal ), literal );
	}

	private static Class determineClass(Object literal) {
		return literal == null ? null : literal.getClass();
	}

	public LiteralExpression(HibernateCriteriaBuilder criteriaBuilder, Class<T> type, T literal) {
		super( criteriaBuilder, type );
		this.literal = literal;
	}

	@SuppressWarnings({ "unchecked" })
	public T getLiteral() {
		return (T) literal;
	}

	public void registerParameters(ParameterRegistry registry) {
		// nothing to do
	}

	@Override
	public DomainReference getExpressionSqmType() {
		return null;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	protected void resetJavaType(Class targetType) {
		super.resetJavaType( targetType );
		wasReset = true;
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmExpression visitExpression(CriteriaVisitor visitor) {
		final T value = wasReset
				? (T) convert( literal, getJavaType() )
				: (T) literal;
		return visitor.visitConstant( value, getJavaType() );
	}

	@SuppressWarnings("unchecked")
	private <X> Object convert(X literal, Class<T> javaType) {
		// todo : convert the literal value based on the Java type.  This requires access to Session though
		return ( (MetamodelImplementor) criteriaBuilder().getEntityManagerFactory().getMetamodel() ).getTypeConfiguration()
				.getJavaTypeDescriptorRegistry()
				.getDescriptor( (Class<X>) literal.getClass() )
				.unwrap( literal, javaType, null );
	}
}
