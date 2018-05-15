/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.type.descriptor.java.internal.CollectionJavaDescriptor;

/**
 * Represents a reference to a persistent collection either as a
 * {@link DomainResult} ({@link CollectionResult} or as a
 * {@link Fetch} ({@link CollectionFetch}).
 *
 * @author Steve Ebersole
 */
public interface CollectionMappingNode extends ResultSetMappingNode {
	/**
	 * Retrieves the CollectionPersister describing the collection associated with this CollectionReference.
	 *
	 * @return The PersistentCollectionDescriptor.
	 */
	PersistentCollectionDescriptor getCollectionDescriptor();

	@Override
	CollectionJavaDescriptor getJavaTypeDescriptor();

	DomainResult getKeyContainerResult();

	DomainResult getKeyCollectionResult();
}
