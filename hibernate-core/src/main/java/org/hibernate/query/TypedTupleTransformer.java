/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query;

/**
 * Extension to TupleTransformer exposing the transformation target type.
 *
 * @apiNote This is mainly intended for use in equality checking while applying
 * result de-duplication for queries.
 *
 * @author Steve Ebersole
 */
public interface TypedTupleTransformer<T> extends TupleTransformer<T> {
	/**
	 * The type resulting from this transformation
	 */
	Class<T> getTransformedType();
}
