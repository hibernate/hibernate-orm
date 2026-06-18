/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.expression;

import jakarta.annotation.Nullable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.function.Consumer;
import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.Expression;

import org.hibernate.Internal;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.spi.select.SqmSelectableNode;
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

	@Nonnull
	@Override
	default SqmExpression<Long> asLong() {
		return cast( Long.class );
	}

	@Nonnull
	@Override
	default SqmExpression<Integer> asInteger() {
		return cast( Integer.class );
	}

	@Nonnull
	@Override
	default SqmExpression<Float> asFloat() {
		return cast( Float.class );
	}

	@Nonnull
	@Override
	default SqmExpression<Double> asDouble() {
		return cast( Double.class );
	}

	@Nonnull
	@Override
	default SqmExpression<BigDecimal> asBigDecimal() {
		return cast( BigDecimal.class );
	}

	@Nonnull
	@Override
	default SqmExpression<BigInteger> asBigInteger() {
		return cast( BigInteger.class );
	}

	@Nonnull
	@Override
	default SqmExpression<String> asString() {
		return cast( String.class );
	}

	@Nonnull
	@Override
	<X> SqmExpression<X> as(@Nonnull Class<X> type);

	@Nonnull
	@Override
	SqmPredicate isNull();

	@Nonnull
	@Override
	SqmPredicate isNotNull();

	@Nonnull
	@Override
	SqmPredicate equalTo(@Nonnull Expression<?> value);

	@Nonnull
	@Override
	SqmPredicate equalTo(Object value);

	@Nonnull
	@Override
	SqmPredicate in(@Nonnull Object... values);

	@Nonnull
	@Override
	SqmPredicate in(@Nonnull Expression<?>... values);

	@Nonnull
	@Override
	SqmPredicate in(@Nonnull Collection<?> values);

	@Nonnull
	@Override
	SqmPredicate in(@Nonnull Expression<Collection<?>> values);

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

	@Nonnull
	@Override
	default <X> SqmExpression<X> cast(@Nonnull Class<X> type) {
		final BasicType<X> basicType = nodeBuilder().getTypeConfiguration().getBasicTypeForJavaType( type );
		if ( basicType == null ) {
			throw new IllegalArgumentException( "Couldn't determine basic type for java type: " + type.getName() );
		}
		return castAs( basicType );
	}

	@Nonnull
	@Override
	JpaPredicate notEqualTo(@Nonnull Expression<?> value);

	@Nonnull
	@Override
	JpaPredicate notEqualTo(Object value);
}
