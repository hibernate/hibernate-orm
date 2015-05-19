/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.proxy.narrow;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.proxy.HibernateProxy;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Yoann Rodière
 * @author Guillaume Smet
 */
public class ProxyNarrowingTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { AbstractEntity.class, ConcreteEntity.class, LazyAbstractEntityReference.class };
	}

	@Test
	public void testNarrowedProxyIsInitializedIfOriginalProxyIsInitialized() {
		Session session = openSession();

		Integer entityReferenceId = null;

		// Populate the database
		try {
			Transaction t = session.beginTransaction();

			ConcreteEntity entity = new ConcreteEntity();
			session.save( entity );

			LazyAbstractEntityReference reference = new LazyAbstractEntityReference( entity );
			session.save( reference );
			entityReferenceId = reference.getId();

			session.flush();
			t.commit();
		}
		finally {
			session.close();
		}

		session = openSession();

		try {
			// load a proxified version of the entity into the session: the proxy is based on the AbstractEntity class
			// as the reference class property is of type AbstractEntity.
			LazyAbstractEntityReference reference = (LazyAbstractEntityReference) session.get( LazyAbstractEntityReference.class, entityReferenceId );
			AbstractEntity abstractEntityProxy = reference.getEntity();

			assertTrue( ( abstractEntityProxy instanceof HibernateProxy ) && !Hibernate.isInitialized( abstractEntityProxy ) );
			Hibernate.initialize( abstractEntityProxy );
			assertTrue( Hibernate.isInitialized( abstractEntityProxy ) );

			// load the concrete class via session.load to trigger the StatefulPersistenceContext.narrowProxy code
			ConcreteEntity concreteEntityProxy = (ConcreteEntity) session.load( ConcreteEntity.class, abstractEntityProxy.getId() );

			// the new proxy created should be initialized
			assertTrue( Hibernate.isInitialized( concreteEntityProxy ) );
			assertTrue( session.contains( concreteEntityProxy ) );
		}
		finally {
			session.close();
		}
	}

}
