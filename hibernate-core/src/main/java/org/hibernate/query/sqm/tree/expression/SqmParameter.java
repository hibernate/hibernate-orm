/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.HibernateException;
import org.hibernate.query.BindableType;
import org.hibernate.query.criteria.JpaParameterExpression;
import org.hibernate.query.sqm.tree.SqmCopyContext;

/**
 * Models a parameter expression declared in the query.
 *
 * @implNote Each usage of a given named/positional query parameter
 * will result in a unique {@code SqmParameter} instance, each will
 * simply use to the same binding. This is important to distinguish
 * usage of the same parameter in different clauses which effects
 * the rendering and value binding.
 *
 * @author Steve Ebersole
 */
public interface SqmParameter<T> extends SqmExpression<T>, JpaParameterExpression<T>, Comparable<SqmParameter<T>> {
	/**
	 * If this represents a named parameter, return that parameter name;
	 * otherwise return {@code null}.
	 *
	 * @return The parameter name, or {@code null} if not a named parameter
	 */
	String getName();

	/**
	 * If this represents a positional parameter, return that parameter position;
	 * otherwise return {@code null}.
	 *
	 * @return The parameter position
	 */
	Integer getPosition();

	/**
	 * Can a collection/array of values be bound to this parameter?
	 * <p>
	 * This is allowed in very limited contexts within the query:
	 * <ol>
	 *     <li>as the value of an IN predicate if the only value is a single param</li>
	 *     <li>(in non-strict JPA mode) as the final vararg to a function</li>
	 * </ol>
	 *
	 * @return {@code true} if binding collection/array of values is allowed
	 * for this parameter; {@code false} otherwise.
	 */
	boolean allowMultiValuedBinding();

	/**
	 * Based on the context it is declared, what is the anticipated type for
	 * bind values?
	 * <p>
	 * NOTE: If {@link #allowMultiValuedBinding()} is true, this will indicate
	 * the Type of the individual values.
	 *
	 * @return The anticipated Type.
	 */
	BindableType<T> getAnticipatedType();

	/**
	 * Make a copy
	 */
	SqmParameter<T> copy();

	@Override
	SqmParameter<T> copy(SqmCopyContext context);

	/**
	 * @implSpec Defined as default since this is an SPI to
	 * support any previous extensions
	 */
	@Override
	default int compareTo(SqmParameter<T> anotherParameter) {
		if ( this instanceof SqmNamedParameter<?> one ) {
			return anotherParameter instanceof SqmNamedParameter<?>
					? one.getName().compareTo( anotherParameter.getName() )
					: -1;
		}
		else if ( this instanceof SqmPositionalParameter<?> one ) {
			return anotherParameter instanceof SqmPositionalParameter<?>
					? one.getPosition().compareTo( anotherParameter.getPosition() )
					: 1;
		}
		else if ( this instanceof SqmJpaCriteriaParameterWrapper
				&& anotherParameter instanceof SqmJpaCriteriaParameterWrapper ) {
			return Integer.compare( this.hashCode(), anotherParameter.hashCode() );
		}
		throw new HibernateException( "Unexpected SqmParameter type for comparison : " + this + " & " + anotherParameter );
	}
}
