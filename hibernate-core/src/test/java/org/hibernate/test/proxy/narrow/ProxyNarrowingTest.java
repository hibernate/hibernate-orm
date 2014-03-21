/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.test.proxy.narrow;

import static org.junit.Assert.assertTrue;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

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
