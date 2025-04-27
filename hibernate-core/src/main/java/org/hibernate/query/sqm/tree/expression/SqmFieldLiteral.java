/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.hibernate.QueryException;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.UnknownPathException;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmDomainType;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;
import org.hibernate.type.descriptor.java.JavaType;

import jakarta.persistence.criteria.Expression;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @author Steve Ebersole
 */
public class SqmFieldLiteral<T> implements SqmExpression<T>, SqmExpressible<T>, SqmSelectableNode<T>, SemanticPathPart {
	private final T value;
	private final JavaType<T> fieldJavaType;
	private final String fieldName;
	private final NodeBuilder nodeBuilder;

	private final SqmExpressible<T> expressible;

	public SqmFieldLiteral(
			Field field,
			JavaType<T> fieldJavaType,
			NodeBuilder nodeBuilder){
		this(
				extractValue( field ),
				fieldJavaType,
				field.getName(),
				nodeBuilder
		);
	}

	public SqmFieldLiteral(
			T value,
			JavaType<T> fieldJavaType,
			String fieldName,
			NodeBuilder nodeBuilder) {
		this.value = value;
		this.fieldJavaType = fieldJavaType;
		this.fieldName = fieldName;
		this.nodeBuilder = nodeBuilder;

		this.expressible = this;
	}

	private static <T> T extractValue(Field field) {
		try {
			//noinspection unchecked
			return (T) field.get( null );
		}
		catch (IllegalAccessException e) {
			throw new QueryException( "Could not access Field value for SqmFieldLiteral", e );
		}
	}

	@Override
	public SqmFieldLiteral<T> copy(SqmCopyContext context) {
		final SqmFieldLiteral<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		return context.registerCopy(
				this,
				new SqmFieldLiteral<>(
						value,
						fieldJavaType,
						fieldName,
						nodeBuilder()
				)
		);
	}

	public T getValue() {
		return value;
	}

	public String getFieldName() {
		return fieldName;
	}

	public NodeBuilder getNodeBuilder() {
		return nodeBuilder;
	}

	@Override
	public SqmExpressible<T> getNodeType() {
		return expressible;
	}

	@Override
	public void applyInferableType(@Nullable SqmExpressible<?> type) {
	}

	@Override
	public JavaType<T> getExpressibleJavaType() {
		return expressible == this ? fieldJavaType : expressible.getExpressibleJavaType();

	}

	@Override
	public JavaType<T> getJavaTypeDescriptor() {
		return getExpressibleJavaType();
	}

	@Override
	public Class<T> getBindableJavaType() {
		return getJavaTypeDescriptor().getJavaTypeClass();
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitFieldLiteral( this );
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		SqmLiteral.appendHqlString( hql, getJavaTypeDescriptor(), getValue() );
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof SqmFieldLiteral<?> that
			&& Objects.equals( value, that.value );
	}

	@Override
	public int hashCode() {
		return Objects.hash( value );
	}

	@Override
	public NodeBuilder nodeBuilder() {
		return nodeBuilder;
	}

	@Override
	public SqmPredicate isNull() {
		return nodeBuilder().isNull( this );
	}

	@Override
	public SqmPredicate equalTo(Expression<?> that) {
		return nodeBuilder().equal( this, that );
	}

	@Override
	public SqmPredicate equalTo(Object that) {
		return nodeBuilder().equal( this, that );
	}

	@Override
	public SqmPredicate notEqualTo(Expression<?> that) {
		return nodeBuilder().notEqual( this, that );
	}

	@Override
	public SqmPredicate notEqualTo(Object that) {
		return nodeBuilder().notEqual( this, that );
	}

	@Override
	public <X> SqmExpression<X> cast(Class<X> type) {
		return null;
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
		//noinspection unchecked
		return nodeBuilder().in( this, (Collection<T>) values );
	}

	@Override
	public SqmPredicate in(Expression<Collection<?>> values) {
		return nodeBuilder().in( this, values );
	}

	@Override
	public SqmExpression<Long> asLong() {
		//noinspection unchecked
		return (SqmExpression<Long>) this;
	}

	@Override
	public SqmExpression<Integer> asInteger() {
		//noinspection unchecked
		return (SqmExpression<Integer>) this;
	}

	@Override
	public SqmExpression<Float> asFloat() {
		//noinspection unchecked
		return (SqmExpression<Float>) this;
	}

	@Override
	public SqmExpression<Double> asDouble() {
		//noinspection unchecked
		return (SqmExpression<Double>) this;
	}

	@Override
	public SqmExpression<BigDecimal> asBigDecimal() {
		//noinspection unchecked
		return (SqmExpression<BigDecimal>) this;
	}

	@Override
	public SqmExpression<BigInteger> asBigInteger() {
		//noinspection unchecked
		return (SqmExpression<BigInteger>) this;
	}

	@Override
	public SqmExpression<String> asString() {
		//noinspection unchecked
		return (SqmExpression<String>) this;
	}

	@Override
	public <X> SqmExpression<X> as(Class<X> type) {
		return null;
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			boolean isTerminal,
			SqmCreationState creationState) {
		throw new UnknownPathException(
				String.format(
						Locale.ROOT,
						"Static field reference [%s#%s] cannot be de-referenced",
						fieldJavaType.getTypeName(),
						fieldName
				)
		);
	}

	@Override
	public SqmPath<?> resolveIndexedAccess(
			SqmExpression<?> selector,
			boolean isTerminal,
			SqmCreationState creationState) {
		throw new UnknownPathException(
				String.format(
						Locale.ROOT,
						"Static field reference [%s#%s] cannot be de-referenced",
						fieldJavaType.getTypeName(),
						fieldName
				)
		);
	}

	@Override
	public boolean isCompoundSelection() {
		return false;
	}

	@Override
	public List<? extends JpaSelection<?>> getSelectionItems() {
		// per-JPA
		throw new IllegalStateException( "Not a compound selection" );
	}

	@Override
	public JpaSelection<T> alias(String name) {
		return null;
	}

	@Override
	public String getAlias() {
		return null;
	}

	@Override
	public SqmDomainType<T> getSqmType() {
		return null;
	}

}
