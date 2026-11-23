package org.hibernate.orm.test.annotations.idclass;

import java.io.Serializable;
import java.util.Objects;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/// A bidirectional one-to-one must not compare the owner's string ID as the target's composite ID.
///
/// @author Steve Ebersole
@JiraKey( "HHH-14824" )
@Jpa( annotatedClasses = { OneToOneDifferentIdTypesTest.A.class, OneToOneDifferentIdTypesTest.B.class } )
public class OneToOneDifferentIdTypesTest {
	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testLinkAfterSeparateFlushes(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final A a = new A();
			a.id = "String1";
			entityManager.persist( a );
			entityManager.flush();

			final B b = new B();
			b.field1 = 1;
			b.field2 = 2;
			entityManager.persist( b );
			entityManager.flush();

			a.b = b;
			b.a = a;
			entityManager.flush();
		} );

		scope.inEntityManager( entityManager -> {
			final A a = entityManager.find( A.class, "String1" );
			assertNotNull( a );
			assertNotNull( a.b );
			assertEquals( 1, a.b.field1 );
			assertEquals( 2, a.b.field2 );
			assertSame( a, a.b.a );
			assertSame( a.b, entityManager.find( B.class, new BId( 1, 2 ) ) );
		} );
	}

	@Entity( name = "A" )
	@Table( name = "hhh14824_a" )
	public static class A {
		@Id
		private String id;

		@OneToOne( mappedBy = "a", cascade = CascadeType.ALL )
		private B b;
	}

	@Entity( name = "B" )
	@Table( name = "hhh14824_b" )
	@IdClass( BId.class )
	public static class B {
		@Id
		private int field1;
		@Id
		private int field2;

		@OneToOne
		private A a;
	}

	public static class BId implements Serializable {
		private int field1;
		private int field2;

		public BId() {
		}

		public BId(int field1, int field2) {
			this.field1 = field1;
			this.field2 = field2;
		}

		@Override
		public boolean equals(Object other) {
			return other instanceof BId id && field1 == id.field1 && field2 == id.field2;
		}

		@Override
		public int hashCode() {
			return Objects.hash( field1, field2 );
		}
	}
}
