/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;

/**
 * Models a parameter expression declared in the query.
 *
 * @implNote Each usage of a given named/positional query parameter
 * will result in a unique SqmParameter instance, each will simply
 * use to the same binding.  This is important to distinguish usage
 * of the same parameter in different clauses which effects the
 * rendering and value binding.
 *
 * @author Steve Ebersole
 */
public interface SqmParameter extends ImpliedTypeSqmExpression {
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
	 * <P/>
	 * This is allowed in very limited contexts within the query:<ol>
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
	 * <p/>
	 * NOTE: If {@link #allowMultiValuedBinding()} is true, this will indicate
	 * the Type of the individual values.
	 *
	 * todo (6.0) : shouldn't this be AllowableParameterType?
	 *
	 * @return The anticipated Type.
	 */
	AllowableParameterType getAnticipatedType();

	@Override
	AllowableParameterType getExpressableType();

	@Override
	AllowableParameterType getInferableType();
}
