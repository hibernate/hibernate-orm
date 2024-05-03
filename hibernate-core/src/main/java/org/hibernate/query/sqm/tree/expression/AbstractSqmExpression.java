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

import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.SqmTreeCreationLogger;
import org.hibernate.query.sqm.internal.SqmCriteriaNodeBuilder;
import org.hibernate.query.sqm.tree.jpa.AbstractJpaSelection;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.type.descriptor.java.JavaType;

import jakarta.persistence.criteria.Expression;
import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.query.internal.QueryHelper.highestPrecedenceType2;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmExpression<T> extends AbstractJpaSelection<T> implements SqmExpression<T> {

	public AbstractSqmExpression(@Nullable SqmExpressible<? super T> type, NodeBuilder criteriaBuilder) {
		super( type, criteriaBuilder );
	}

	@Override
	public SqmCriteriaNodeBuilder nodeBuilder() {
		return (SqmCriteriaNodeBuilder) super.nodeBuilder();
	}

	@Override
	public void applyInferableType(@Nullable SqmExpressible<?> type) {
	}

	protected void internalApplyInferableType(@Nullable SqmExpressible<?> newType) {
		SqmTreeCreationLogger.LOGGER.debugf(
				"Applying inferable type to SqmExpression [%s] : %s -> %s",
				this,
				getExpressible(),
				newType
		);

		setExpressibleType( highestPrecedenceType2( newType, getExpressible() ) );
	}

	private <B> SqmExpression<B> castToBasicType(Class<B> javaType) {
		return castAs( nodeBuilder().getTypeConfiguration().getBasicTypeForJavaType( javaType ) );
	}

	@Override
	public SqmExpression<Long> asLong() {
		return castToBasicType( Long.class );
	}

	@Override
	public SqmExpression<Integer> asInteger() {
		return castToBasicType( Integer.class );
	}

	@Override
	public SqmExpression<Float> asFloat() {
		return castToBasicType( Float.class );
	}

	@Override
	public SqmExpression<Double> asDouble() {
		return castToBasicType( Double.class );
	}

	@Override
	public SqmExpression<BigDecimal> asBigDecimal() {
		return castToBasicType( BigDecimal.class );
	}

	@Override
	public SqmExpression<BigInteger> asBigInteger() {
		return castToBasicType( BigInteger.class );
	}

	@Override
	public SqmExpression<String> asString() {
		return castToBasicType( String.class );
	}

	@Override
	public <X> SqmExpression<X> as(Class<X> type) {
		return nodeBuilder().cast( this, type );
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
	public SqmPredicate equalTo(Expression<T> that) {
		return nodeBuilder().equal( this, that );
	}

	@Override
	public SqmPredicate equalTo(T that) {
		return nodeBuilder().equal( this, that );
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
		//noinspection unchecked
		return nodeBuilder().in( this, (Collection<T>) values );
	}

	@Override
	public SqmPredicate in(Expression<Collection<?>> values) {
		return nodeBuilder().in( this, values );
	}

	@Override
	public @Nullable JavaType<T> getJavaTypeDescriptor() {
		return getNodeType() == null ? null : getNodeType().getExpressibleJavaType();
	}
}
