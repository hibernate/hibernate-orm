/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.result.spi;

import org.hibernate.metamodel.model.domain.spi.PersistentCollectionMetadata;

/**
 * Represents a reference to a persistent collection either as a Return or as a {@link FetchCollectionAttribute}.
 *
 * @author Steve Ebersole
 */
public interface CollectionReference {
	/**
	 * Retrieves the CollectionPersister describing the collection associated with this CollectionReference.
	 *
	 * @return The CollectionPersister.
	 */
	PersistentCollectionMetadata getCollectionMetadata();
}
