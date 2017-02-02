/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.proxy;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;


import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Christian Beikov
 */
@TestForIssue(jiraKey = "HHH-9638")
public class ProxyReferenceEqualityTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				A.class,
				B.class
		};
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Test
	public void testProxyFromQuery() {
		doInHibernate( this::sessionFactory, s -> {
			A a = new A();
			a.id = 1L;
			a.b = new B();
			a.b.id = 1L;
			s.persist( a );
		} );

		doInHibernate( this::sessionFactory, s -> {
			A a = s.find( A.class, 1L );
			List<B> result = s.createQuery( "FROM " + B.class.getName() + " b", B.class ).getResultList();
			assertEquals( 1, result.size() );
			assertTrue( a.b == result.get( 0 ) );
		} );
	}

	@Entity
	public static class A {
		@Id
		Long id;
		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
		@LazyToOne(LazyToOneOption.NO_PROXY)
		B b;
	}

	@Entity
	public static class B {
		@Id
		Long id;
	}
}
