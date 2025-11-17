/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance.discriminator;

import java.io.Serializable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Christian Beikov
 */
@DomainModel(
		annotatedClasses = {
				MultiSingleTableLoadTest.Holder.class,
				MultiSingleTableLoadTest.A.class,
				MultiSingleTableLoadTest.X.class,
				MultiSingleTableLoadTest.B.class,
				MultiSingleTableLoadTest.C.class,
				MultiSingleTableLoadTest.Y.class,
				MultiSingleTableLoadTest.Z.class
		}
)
@SessionFactory
public class MultiSingleTableLoadTest {

	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Holder holder1 = new Holder( 1, new B( 1, new Y( 1, "y" ) ) );
			Holder holder2 = new Holder( 2, new C( 2, new Z( 2, "z" ) ) );

			session.persist( holder1 );
			session.persist( holder2 );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey(value = "HHH-5954")
	public void testEagerLoadMultipleHoldersWithDifferentSubtypes(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Holder task1 = session.find( Holder.class, 1L );
			Holder task2 = session.find( Holder.class, 2L );
			assertNotNull( task1 );
			A task1A = task1.getA();
			assertTrue( task1A instanceof B );
			B b = (B) task1A;
			assertTrue( b.getX() instanceof Y );
			// Previously, we asserted that X was initialized in this case,
			// but doing that is not very sound, since EAGER initialization may only happen
			// task1.a were instanceof C. Since that is not the case, no initialization happens
//			assertTrue( Hibernate.isInitialized( b.getX() ) );
			assertFalse( Hibernate.isInitialized( b.getX() ) );
			assertEquals( "y", b.getX().getTheString() );

			assertNotNull( task2 );

			A task2A = task2.getA();
			assertTrue( task2A instanceof C );
			C c = (C) task2A;
			assertTrue( c.getX() instanceof Z );
			assertTrue( Hibernate.isInitialized( c.getX() ) );
			Z z = (Z) c.getX();
			assertEquals( "z", z.getTheString() );
		} );
	}

	@Test
	public void testFetchJoinLoadMultipleHoldersWithDifferentSubtypes(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
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

		public A getA() {
			return a;
		}
	}

	@Entity(name = "A")
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

	@Entity(name = "B")
	@DiscriminatorValue("B")
	public static class B extends A {
		@ManyToOne(optional = true, cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		@JoinColumn(name = "x_id")
		private Y x;

		public B() {
		}

		public B(long id, Y x) {
			super( id );
			this.x = x;
		}

		public Y getX() {
			return x;
		}
	}

	@Entity(name = "C")
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

		public X getX() {
			return x;
		}
	}

	@Entity(name = "X")
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

	@Entity(name = "Y")
	@DiscriminatorValue("Y")
	public static class Y extends X {
		private String theString;

		public Y() {
		}

		public Y(long id, String theString) {
			super( id );
			this.theString = theString;
		}

		public String getTheString() {
			return theString;
		}
	}

	@Entity(name = "Z")
	@DiscriminatorValue("Z")
	public static class Z extends X {
		private String theString;

		public Z() {
		}

		public Z(long id, String theString) {
			super( id );
			this.theString = theString;
		}

		public String getTheString() {
			return theString;
		}
	}
}
