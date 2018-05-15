/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.inheritance.discriminator;

import java.io.Serializable;
import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Christian Beikov
 */
public class MultiSingleTableLoadTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Holder.class, A.class, X.class,
				B.class, C.class,
				Y.class, Z.class
		};
	}

	private void createTestData() {
		doInHibernate( this::sessionFactory, session -> {
			Holder holder1 = new Holder( 1, new B( 1, new Y( 1 ) ) );
			Holder holder2 = new Holder( 2, new C( 2, new Z( 2 ) ) );

			session.persist( holder1 );
			session.persist( holder2 );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-5954")
	public void testEagerLoadMultipleHoldersWithDifferentSubtypes() {
		createTestData();
		doInHibernate( this::sessionFactory, session -> {
			Holder task1 = session.find( Holder.class, 1L );
			Holder task2 = session.find( Holder.class, 2L );
			assertNotNull( task1 );
			assertNotNull( task2 );
		} );
	}

	@Test
	public void testFetchJoinLoadMultipleHoldersWithDifferentSubtypes() {
		createTestData();
		doInHibernate( this::sessionFactory, session -> {
			Holder task1 = session.createQuery( "FROM Holder h JOIN FETCH h.a WHERE h.id = :id", Holder.class )
					.setParameter( "id", 1L ).getSingleResult();
			Holder task2 = session.createQuery( "FROM Holder h JOIN FETCH h.a WHERE h.id = :id", Holder.class )
					.setParameter( "id", 2L ).getSingleResult();
			assertNotNull( task1 );
			assertNotNull( task2 );
			assertTrue( task1.a instanceof B );
			assertTrue( task2.a instanceof C );
		} );
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Entity(name = "Holder")
	@Table(name = "holder")
	public static class Holder implements Serializable {
		@Id
		private long id;
		@ManyToOne(optional = false, cascade = CascadeType.ALL)
		@JoinColumn(name = "a_id")
		private A a;

		public Holder() {
		}

		public Holder(long id, A a) {
			this.id = id;
			this.a = a;
		}
	}

	@Entity
	@Table(name = "tbl_a")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public static abstract class A implements Serializable {
		@Id
		private long id;

		public A() {
		}

		public A(long id) {
			this.id = id;
		}
	}

	@Entity
	@DiscriminatorValue("B")
	public static class B extends A {
		@ManyToOne(optional = true, cascade = CascadeType.ALL)
		@JoinColumn(name = "x_id")
		private Y x;

		public B() {
		}

		public B(long id, Y x) {
			super( id );
			this.x = x;
		}
	}

	@Entity
	@DiscriminatorValue("C")
	public static class C extends A {
		@ManyToOne(optional = true, cascade = CascadeType.ALL)
		@JoinColumn(name = "x_id")
		private X x;

		public C() {
		}

		public C(long id, X x) {
			super( id );
			this.x = x;
		}
	}

	@Entity
	@Table(name = "tbl_x")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public static abstract class X implements Serializable {
		@Id
		private long id;

		public X() {
		}

		public X(long id) {
			this.id = id;
		}
	}

	@Entity
	@DiscriminatorValue("Y")
	public static class Y extends X {
		public Y() {
		}

		public Y(long id) {
			super( id );
		}
	}

	@Entity
	@DiscriminatorValue("Z")
	public static class Z extends X {
		public Z() {
		}

		public Z(long id) {
			super( id );
		}
	}


}
