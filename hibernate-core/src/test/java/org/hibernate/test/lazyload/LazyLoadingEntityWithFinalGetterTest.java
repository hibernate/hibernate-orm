/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.lazyload;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;

/**
 * This test demonstrates what happens if enity has final methods and they are accessed trough lazy loaded proxy. Proxy
 * informs that it's initialised but getter gets wrong data.
 * 
 * @author @ikettu
 */
public class LazyLoadingEntityWithFinalGetterTest extends BaseCoreFunctionalTestCase {

	@Entity
	public static class A {

		@Id
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		private B b;

		public A() {
		};

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public B getB() {
			return b;
		}

		public void setB(B b) {
			this.b = b;
		}
	}

	@Entity
	public static class B {

		@Id
		private Long id;

		private String text;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		/**
		 * Key point here that this method is final.
		 * 
		 * @return
		 */
		public final String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				A.class, B.class
		};
	}

	protected void createTestData() {
		Session s = openSession();

		// initialize data
		Transaction tx0 = s.beginTransaction();

		B b0 = new B();

		b0.setId( 1L );
		b0.setText( "xyzzy" );
		session.save( b0 );

		A a0 = new A();
		a0.setId( 2L );
		a0.setB( b0 );
		session.save( a0 );

		session.flush();
		tx0.commit();
		s.close();
	}

	@Test
	@TestForIssue(jiraKey="HHH-12121")
	public void testLazyLoadedAssociationWithFinalGetter() throws Exception {

		// given
		createTestData();

		Session s = openSession();
		Transaction tx1 = s.beginTransaction();

		// when

		A a = session.find( A.class, 2L );

		Assert.assertTrue( Hibernate.isInitialized( a ) );
		Assert.assertFalse( Hibernate.isInitialized( a.getB() ) );

		// this fails without any warning why lazy loading did not work
		try {
			// then
			Assert.assertEquals( "xyzzy", a.getB().getText() );
		}
		catch (HibernateException he) {
			// should throw some exception, or at least give some warning?
		}

		tx1.commit();

		s.close();
	}

	@Test
	@TestForIssue(jiraKey="HHH-12121")
	public void testInitializedLazyLoadedAssociationWithFinalGetter() throws Exception {

		// given
		createTestData();

		Session s = openSession();
		Transaction tx1 = s.beginTransaction();

		// when

		A a = session.find( A.class, 2L );

		Assert.assertTrue( Hibernate.isInitialized( a ) );

		B b = a.getB();
		Assert.assertFalse( Hibernate.isInitialized( b ) );

		Hibernate.initialize( b );

		// then

		Assert.assertEquals( "xyzzy", b.getText() );

		tx1.commit();

		s.close();
	}

	@Test
	@TestForIssue(jiraKey="HHH-12121")
	public void testUnproxiedLazyLoadedAssociationWithFinalGetter() throws Exception {

		// given
		createTestData();

		Session s = openSession();
		Transaction tx1 = s.beginTransaction();

		// when

		A a = session.find( A.class, 2L );

		Assert.assertTrue( Hibernate.isInitialized( a ) );

		B b = a.getB();
		Assert.assertFalse( Hibernate.isInitialized( b ) );

		b = (B) Hibernate.unproxy( b );

		// then

		Assert.assertEquals( "xyzzy", b.getText() );

		tx1.commit();

		s.close();
	}

}
