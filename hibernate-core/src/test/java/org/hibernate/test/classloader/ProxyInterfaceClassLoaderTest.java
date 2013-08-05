/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.classloader;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Proxy;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests if javassist instrumentation is done with the proper classloader for entities with proxy class. The classloader
 * of {@link HibernateProxy} will not see {@link IPerson}, since it is only accessible from this package. But: the
 * classloader of {@link IPerson} will see {@link HibernateProxy}, so instrumentation will only work if this classloader
 * is chosen for creating the instrumented proxy class. We need to check the class of a loaded object though, since
 * building the configuration will not fail, only log the error and fall back to using the entity class itself as a
 * proxy.
 *
 * @author lgathy
 */
public class ProxyInterfaceClassLoaderTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Person.class };
	}

	@Test
	public void testProxyClassLoader() {

		Session s = openSession();
		Transaction t = s.beginTransaction();
		IPerson p = new Person();
		p.setId( 1 );
		s.persist( p );
		s.flush();
		s.clear();

		Object lp = s.load( Person.class, p.getId() );

		Assert.assertTrue( "Loaded entity is not an instance of the proxy interface", IPerson.class.isInstance( lp ) );
		Assert.assertFalse( "Proxy class was not created", Person.class.isInstance( lp ) );

		s.delete( lp );
		t.commit();
		s.close();
	}

	interface IPerson {

		int getId();

		void setId(int id);

	}

	@Entity( name = "Person" )
	@Proxy(proxyClass = IPerson.class)
	static class Person implements IPerson {

		@Id
		private int id;

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

	}

}
