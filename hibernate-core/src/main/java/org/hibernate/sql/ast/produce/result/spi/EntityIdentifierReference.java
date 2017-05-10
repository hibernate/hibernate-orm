/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.result.spi;

/**
 * Descriptor for the identifier of an entity as a FetchSource (which allows for key-many-to-one handling).
 *
 * @author Steve Ebersole
 */
public interface EntityIdentifierReference {
	EntityReference getEntityReference();

	/**
	 * Can this EntityIdentifierDescription be treated as a FetchSource and if so does it have any
	 * fetches?  In other words, can it be cast to {@link FetchParent}?
	 */
	boolean hasFetches();

	/**
	 * Can this EntityIdentifierDescription be treated as a FetchSource and if so does it have any
	 * bidirectional entity references?
	 *
	 * @return {@code true} iff {@code this} can be cast to {@link FetchParent} and (after casting) it returns
	 * non-empty results for {@link FetchParent#getBidirectionalEntityReferences()}
	 *
	 * todo (6.0) : need to account for this method as per load plan's FetchSource#getBidirectionalEntityReferences
	 */
	boolean hasBidirectionalEntityReferences();
}
