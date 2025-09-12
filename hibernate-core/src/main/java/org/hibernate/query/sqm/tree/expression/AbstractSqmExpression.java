/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;

import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.SqmTreeCreationLogger;
import org.hibernate.query.sqm.internal.SqmCriteriaNodeBuilder;
import org.hibernate.query.sqm.tree.jpa.AbstractJpaSelection;
import org.hibernate.query.sqm.tree.predicate.SqmInPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.type.descriptor.java.JavaType;

import jakarta.persistence.criteria.Expression;

import static org.hibernate.query.internal.QueryHelper.highestPrecedenceType;
import static org.hibernate.query.internal.QueryHelper.highestPrecedenceType2;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmExpression<T> extends AbstractJpaSelection<T> implements SqmExpression<T> {

	public AbstractSqmExpression(SqmExpressible<? extends T> type, NodeBuilder criteriaBuilder) {
		super( type, criteriaBuilder );
	}

	@Override
	public SqmCriteriaNodeBuilder nodeBuilder() {
		return (SqmCriteriaNodeBuilder) super.nodeBuilder();
	}

	@Override
	public void applyInferableType(SqmExpressible<?> type) {
//		if ( type == null ) {
//			return;
//		}
//
//		final SqmExpressible<?> oldType = getNodeType();
//
//		final SqmExpressible<?> newType = highestPrecedenceType( oldType, type );
//		if ( newType != null && newType != oldType ) {
//			internalApplyInferableType( newType );
//		}
	}

	protected void internalApplyInferableType(SqmExpressible<?> newType) {
		SqmTreeCreationLogger.LOGGER.debugf(
				"Applying inferable type to SqmExpression [%s] : %s -> %s",
				this,
				getExpressible(),
				newType
		);

		setExpressibleType( highestPrecedenceType2( newType, getExpressible() ) );
	}

	@Override
	public SqmExpression<Long> asLong() {
		return castAs( nodeBuilder().getTypeConfiguration().getBasicTypeForJavaType( Long.class ) );
	}

	@Override
	public SqmExpression<Integer> asInteger() {
		return castAs( nodeBuilder().getTypeConfiguration().getBasicTypeForJavaType( Integer.class ) );
	}

	@Override
	public SqmExpression<Float> asFloat() {
		return castAs( nodeBuilder().getTypeConfiguration().getBasicTypeForJavaType( Float.class ) );
	}

	@Override
	public SqmExpression<Double> asDouble() {
		return castAs( nodeBuilder().getTypeConfiguration().getBasicTypeForJavaType( Double.class ) );
	}

	@Override
	public SqmExpression<BigDecimal> asBigDecimal() {
		return castAs( nodeBuilder().getTypeConfiguration().getBasicTypeForJavaType( BigDecimal.class ) );
	}

	@Override
	public SqmExpression<BigInteger> asBigInteger() {
		return castAs( nodeBuilder().getTypeConfiguration().getBasicTypeForJavaType( BigInteger.class ) );
	}

	@Override
	public SqmExpression<String> asString() {
		return castAs( nodeBuilder().getTypeConfiguration().getBasicTypeForJavaType( String.class ) );
	}

	@Override
	public <X> SqmExpression<X> as(Class<X> type) {
		return nodeBuilder().cast(this, type);
	}

	@Override
	public SqmPredicate isNull() {
		return nodeBuilder().isNull( this );
	}

	@Override
	public SqmPredicate isNotNull() {
		return nodeBuilder().isNotNull( this );
	}

	@Override
	public SqmPredicate in(Object... values) {
		return nodeBuilder().in( this, values );
	}

	@Override
	public SqmPredicate in(Expression<?>... values) {
		return nodeBuilder().in( this, values );
	}

	@Override
	public SqmPredicate in(Collection<?> values) {
		final SqmInPredicate<T> in = nodeBuilder().in( this );
		for ( Object value : values ) {
			if ( value instanceof SqmExpression<?> ) {
				//noinspection unchecked
				in.value( (JpaExpression<? extends T>) value );
			}
			else {
				//noinspection unchecked
				in.value( (T) value );
			}
		}

		return in;
	}

	@Override
	public SqmPredicate in(Expression<Collection<?>> values) {
		return nodeBuilder().in( this, values );
	}

	@Override
	public JpaSelection<T> alias(String name) {
		setAlias( name );
		return this;
	}

	@Override
	public JavaType<T> getJavaTypeDescriptor() {
		return getNodeType() == null ? null : getNodeType().getExpressibleJavaType();
	}
}
