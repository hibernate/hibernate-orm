/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.internal.SqmCriteriaNodeBuilder;
import org.hibernate.query.sqm.tree.jpa.AbstractJpaSelection;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;

import jakarta.persistence.criteria.Expression;
import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.query.internal.QueryHelper.highestPrecedenceType2;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmExpression<T> extends AbstractJpaSelection<T> implements SqmExpression<T> {

	public AbstractSqmExpression(@Nullable SqmBindableType<? super T> type, NodeBuilder criteriaBuilder) {
		super( type, criteriaBuilder );
	}

	@Override
	public SqmCriteriaNodeBuilder nodeBuilder() {
		return (SqmCriteriaNodeBuilder) super.nodeBuilder();
	}

	@Override
	public void applyInferableType(@Nullable SqmBindableType<?> type) {
	}

	protected void internalApplyInferableType(@Nullable SqmBindableType<?> newType) {
//		SqmTreeCreationLogger.LOGGER.tracef(
//				"Applying inferable type to SqmExpression [%s]: %s -> %s",
//				this,
//				getExpressible(),
//				newType
//		);

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
		final BasicType<X> basicTypeForJavaType = nodeBuilder().getTypeConfiguration().getBasicTypeForJavaType( type );
		if ( basicTypeForJavaType == null ) {
			throw new IllegalArgumentException( "Can't cast expression to unknown type: " + type.getCanonicalName() );
		}
		return new AsWrapperSqmExpression<>( basicTypeForJavaType, this );
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
	public SqmPredicate equalTo(Expression<?> value) {
		return nodeBuilder().equal( this, value );
	}

	@Override
	public SqmPredicate equalTo(Object value) {
		return nodeBuilder().equal( this, value );
	}

	@Override
	public SqmPredicate notEqualTo(Expression<?> value) {
		return nodeBuilder().notEqual( this, value );
	}

	@Override
	public SqmPredicate notEqualTo(Object value) {
		return nodeBuilder().notEqual( this, value );
	}

	@Override
	public <X> SqmExpression<X> cast(Class<X> castTarget) {
		return nodeBuilder().cast( this, castTarget );
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
		final SqmBindableType<T> nodeType = getNodeType();
		return nodeType == null ? null : nodeType.getExpressibleJavaType();
	}
}
