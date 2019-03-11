/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.collection.dereferenced;

import org.hibernate.Session;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionImplementor;

import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;

/**
 * @author Gail Badner
 */
public abstract class AbstractDereferencedCollectionTest extends SessionFactoryBasedFunctionalTest {

	protected EntityEntry getEntityEntry(Session s, Object entity) {
		return ( (SessionImplementor) s ).getPersistenceContext().getEntry( entity );
	}

	protected CollectionEntry getCollectionEntry(Session s, PersistentCollection collection) {
		return ( (SessionImplementor) s ).getPersistenceContext().getCollectionEntry( collection );
	}
}
