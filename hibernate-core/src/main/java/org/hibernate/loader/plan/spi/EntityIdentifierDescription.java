/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.spi;

/**
 * Descriptor for the identifier of an entity as a FetchSource (which allows for key-many-to-one handling).
 *
 * @author Steve Ebersole
 */
public interface EntityIdentifierDescription {
	/**
	 * Can this EntityIdentifierDescription be treated as a FetchSource and if so does it have any
	 * fetches?
	 *
	 * @return {@code true} iff {@code this} can be cast to {@link FetchSource} and (after casting) it returns
	 * non-empty results for {@link FetchSource#getFetches()}
	 */
	public boolean hasFetches();

	/**
	 * Can this EntityIdentifierDescription be treated as a FetchSource and if so does it have any
	 * bidirectional entity references?
	 *
	 * @return {@code true} iff {@code this} can be cast to {@link FetchSource} and (after casting) it returns
	 * non-empty results for {@link FetchSource#getBidirectionalEntityReferences()}
	 */
	public boolean hasBidirectionalEntityReferences();
}
