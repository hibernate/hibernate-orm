/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.spi;

/**
 * A collection index which is a {@link FetchSource}.
 *
 * @author Steve Ebersole
 */
public interface CollectionFetchableIndex extends FetchSource {
	/**
	 * Reference back to the collection to which this index belongs
	 *
	 * @return the collection reference.
	 */
	public CollectionReference getCollectionReference();
}
