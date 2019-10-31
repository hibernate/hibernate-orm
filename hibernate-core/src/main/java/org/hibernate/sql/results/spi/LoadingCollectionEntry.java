/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import java.io.Serializable;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.sql.exec.spi.ExecutionContext;

/**
 * Represents a collection currently being loaded.
 *
 * @author Steve Ebersole
 */
public interface LoadingCollectionEntry {
	CollectionPersister getCollectionDescriptor();

	CollectionInitializer getInitializer();

	Serializable getKey();

	PersistentCollection getCollectionInstance();

	void finishLoading(ExecutionContext executionContext);
}
