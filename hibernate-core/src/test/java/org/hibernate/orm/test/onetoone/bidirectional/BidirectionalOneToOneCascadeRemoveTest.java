/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.onetoone.bidirectional;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		BidirectionalOneToOneCascadeRemoveTest.A.class,
		BidirectionalOneToOneCascadeRemoveTest.B.class,
} )
@SessionFactory
public class BidirectionalOneToOneCascadeRemoveTest {
	@Test
	public void testWithFlush(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final A a1 = new A( "1", "a1", 1 );
			session.persist( a1 );
			final B bRef = new B( "2", "b2", 2, a1 );
			session.persist( bRef );
			session.flush();

			session.remove( bRef );
		} );
		scope.inTransaction( session -> {
			assertThat( session.find( A.class, "1" ) ).isNull();
			assertThat( session.find( B.class, "2" ) ).isNull();
		} );
	}

	@Test
	public void testWithoutFlush(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final A a1 = new A( "1", "a1", 1 );
			session.persist( a1 );
			final B bRef = new B( "2", "b2", 2, a1 );
			session.persist( bRef );

			session.remove( bRef );
		} );
		scope.inTransaction( session -> {
			assertThat( session.find( A.class, "1" ) ).isNull();
			assertThat( session.find( B.class, "2" ) ).isNull();
		} );
	}

	@Entity( name = "EntityA" )
	static class A {
		@Id
		protected String id;

		@Column( name = "name_col" )
		protected String name;

		@Column( name = "value_col" )
		protected int value;


		@OneToOne( mappedBy = "a1" )
		protected B b1;

		public A() {
		}

		public A(String id, String name, int value) {
			this.id = id;
			this.name = name;
			this.value = value;
		}
	}

	@Entity( name = "EntityB" )
	static class B {
		@Id
		protected String id;

		@Column( name = "name_col" )
		protected String name;

		@Column( name = "value_col" )
		protected int value;

		// ===========================================================
		// relationship fields

		@OneToOne( cascade = CascadeType.REMOVE )
		@JoinColumn( name = "a1_id" )
		protected A a1;

		// ===========================================================
		// constructors

		public B() {
		}

		public B(String id, String name, int value, A a1) {
			this.id = id;
			this.name = name;
			this.value = value;
			this.a1 = a1;
		}
	}
}