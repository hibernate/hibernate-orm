/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;

/**
 * Represents a reference to a persistent collection either as a
 * {@link QueryResult} ({@link PluralAttributeQueryResult} or as a
 * {@link Fetch} ({@link PluralAttributeFetch}).
 *
 * @author Steve Ebersole
 */
public interface PluralAttributeMappingNode extends ResultSetMappingNode {
	/**
	 * Retrieves the CollectionPersister describing the collection associated with this CollectionReference.
	 *
	 * @return The PersistentCollectionDescriptor.
	 */
	PersistentCollectionDescriptor getCollectionDescriptor();
}
