/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.function.Consumer;
import jakarta.persistence.criteria.Expression;
import org.checkerframework.checker.nullness.qual.Nullable;

import org.hibernate.Internal;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;
import org.hibernate.type.BasicType;

import static java.util.Arrays.asList;

/**
 * The base contract for any kind of expression node in the SQM tree.
 * An expression might be a reference to an attribute, a literal,
 * a function, etc.
 *
 * @param <T> The Java type of the expression
 *
 * @author Steve Ebersole
 */
public interface SqmExpression<T> extends SqmSelectableNode<T>, JpaExpression<T> {
	/**
	 * The expression's type.
	 * <p>
	 * Can change as a result of calls to {@link #applyInferableType}
	 */
	@Override
	@Nullable
	SqmBindableType<T> getNodeType();

	/**
	 * Used to apply type information based on the expression's usage
	 * within the query.
	 *
	 * @apiNote The SqmExpressible type parameter is dropped here because
	 * the inference could technically cause a change in Java type (i.e.
	 * an implicit cast)
	 */
	@Internal
	void applyInferableType(@Nullable SqmBindableType<?> type);

	@Override
	default void visitSubSelectableNodes(Consumer<SqmSelectableNode<?>> jpaSelectionConsumer) {
		jpaSelectionConsumer.accept( this );
	}

	@Override
	default SqmExpression<Long> asLong() {
		return cast( Long.class );
	}

	@Override
	default SqmExpression<Integer> asInteger() {
		return cast( Integer.class );
	}

	@Override
	default SqmExpression<Float> asFloat() {
		return cast( Float.class );
	}

	@Override
	default SqmExpression<Double> asDouble() {
		return cast( Double.class );
	}

	@Override
	default SqmExpression<BigDecimal> asBigDecimal() {
		return cast( BigDecimal.class );
	}

	@Override
	default SqmExpression<BigInteger> asBigInteger() {
		return cast( BigInteger.class );
	}

	@Override
	default SqmExpression<String> asString() {
		return cast( String.class );
	}

	@Override
	<X> SqmExpression<X> as(Class<X> type);

	@Override
	SqmPredicate isNull();

	@Override
	SqmPredicate isNotNull();

	@Override
	SqmPredicate equalTo(Expression<?> value);

	@Override
	SqmPredicate equalTo(Object value);

	@Override
	SqmPredicate in(Object... values);

	@Override
	SqmPredicate in(Expression<?>... values);

	@Override
	SqmPredicate in(Collection<?> values);

	@Override
	SqmPredicate in(Expression<Collection<?>> values);

	@Override
	SqmExpression<T> copy(SqmCopyContext context);

	default <X> SqmExpression<X> castAs(DomainType<X> type) {
		if ( getNodeType() == type ) {
			// safe cast, because we just checked
			@SuppressWarnings("unchecked")
			final SqmExpression<X> castExpression = (SqmExpression<X>) this;
			return castExpression;
		}
		else {
			final QueryEngine queryEngine = nodeBuilder().getQueryEngine();
			final SqmCastTarget<?> target = new SqmCastTarget<>( (ReturnableType<?>) type, nodeBuilder() );
			return queryEngine.getSqmFunctionRegistry().getFunctionDescriptor( "cast" )
					.generateSqmExpression( asList( this, target ), (ReturnableType<X>) type, queryEngine );
		}
	}

	@Override
	default <X> SqmExpression<X> cast(Class<X> type) {
		final BasicType<X> basicType = nodeBuilder().getTypeConfiguration().getBasicTypeForJavaType( type );
		if ( basicType == null ) {
			throw new IllegalArgumentException( "Couldn't determine basic type for java type: " + type.getName() );
		}
		return castAs( basicType );
	}

	@Override
	JpaPredicate notEqualTo(Expression<?> value);

	@Override
	JpaPredicate notEqualTo(Object value);
}
