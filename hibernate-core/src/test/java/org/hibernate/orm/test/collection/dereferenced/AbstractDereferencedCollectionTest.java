/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.dereferenced;

import org.hibernate.Session;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionImplementor;

/**
 * @author Gail Badner
 */
public abstract class AbstractDereferencedCollectionTest {

	protected EntityEntry getEntityEntry(Session s, Object entity) {
		return ( (SessionImplementor) s ).getPersistenceContextInternal().getEntry( entity );
	}

	protected CollectionEntry getCollectionEntry(Session s, PersistentCollection<?> collection) {
		return ( (SessionImplementor) s ).getPersistenceContextInternal().getCollectionEntry( collection );
	}
}
