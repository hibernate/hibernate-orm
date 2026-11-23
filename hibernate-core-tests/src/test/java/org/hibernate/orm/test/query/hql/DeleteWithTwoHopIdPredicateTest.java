package org.hibernate.orm.test.query.hql;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.Session;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static jakarta.persistence.CascadeType.PERSIST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/// Exercises the delete predicate and independent association mappings reported in HHH-13348.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = {
		DeleteWithTwoHopIdPredicateTest.A.class,
		DeleteWithTwoHopIdPredicateTest.B.class,
		DeleteWithTwoHopIdPredicateTest.C.class
})
@SessionFactory
@JiraKey("HHH-13348")
public class DeleteWithTwoHopIdPredicateTest {
	@Test
	void testOriginalSetupWithUnsetManyToOneAssociations(SessionFactoryScope scope) {
		final Long rootId = scope.fromTransaction( session -> {
			final C c = new C();
			final B b = new B();
			b.setCs( List.of( c ) );
			final A a = new A();
			a.setBs( List.of( b ) );
			session.persist( a );
			return a.getId();
		} );

		scope.inTransaction( session -> {
			final A a = session.find( A.class, rootId );
			final B b = a.getBs().get( 0 );
			assertNull( b.getA() );
			assertNull( b.getCs().get( 0 ).getB() );
			// The collections are independent join-table mappings, not the navigated foreign keys.
			assertEquals( 0, session.createMutationQuery( "delete from C c where c.b.a.id = :rootId" )
					.setParameter( "rootId", rootId ).executeUpdate() );
		} );

		scope.inTransaction( session -> assertEquals( 1L,
				session.createQuery( "select count(c) from C c", Long.class ).getSingleResult() ) );
	}

	@Test
	void testDeleteWithPopulatedManyToOneAssociations(SessionFactoryScope scope) {
		final Long rootId = scope.fromTransaction( session -> {
			final A selected = createRoot( session, 2 );
			createRoot( session, 1 );
			session.persist( new C() );
			return selected.getId();
		} );

		scope.inTransaction( session -> assertEquals( 2,
				session.createMutationQuery( "delete from C c where c.b.a.id = :rootId" )
						.setParameter( "rootId", rootId ).executeUpdate() ) );

		scope.inTransaction( session -> {
			assertEquals( 2L, session.createQuery( "select count(c) from C c", Long.class ).getSingleResult() );
			assertEquals( 0L, session.createQuery( "select count(c) from C c where c.b.a.id = :rootId", Long.class )
					.setParameter( "rootId", rootId ).getSingleResult() );
			assertEquals( 1L, session.createQuery( "select count(c) from C c where c.b.a.id <> :rootId", Long.class )
					.setParameter( "rootId", rootId ).getSingleResult() );
			assertEquals( 1L, session.createQuery( "select count(c) from C c where c.b is null", Long.class )
					.getSingleResult() );
			assertEquals( 2L, session.createQuery( "select count(a) from A a", Long.class ).getSingleResult() );
			assertEquals( 2L, session.createQuery( "select count(b) from B b", Long.class ).getSingleResult() );
		} );
	}

	private static A createRoot(Session session, int leafCount) {
		final A a = new A();
		session.persist( a );
		final B b = new B();
		b.setA( a );
		session.persist( b );
		// Populate only the foreign keys navigated by the predicate. Incoming collection
		// join-table references would prevent bulk deletion without separate cleanup.
		for ( int index = 0; index < leafCount; index++ ) {
			final C c = new C();
			c.setB( b );
			session.persist( c );
		}
		return a;
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name = "A")
	@Table(name = "two_hop_id_a")
	public static class A {
		private Long id;
		private List<B> bs;

		@Id
		@GeneratedValue
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		@OneToMany(cascade = PERSIST)
		public List<B> getBs() {
			return bs;
		}

		public void setBs(List<B> bs) {
			this.bs = bs;
		}
	}

	@Entity(name = "B")
	@Table(name = "two_hop_id_b")
	public static class B {
		private Long id;
		private A a;
		private List<C> cs;

		@Id
		@GeneratedValue
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		@ManyToOne
		@JoinColumn(name = "a_id")
		public A getA() {
			return a;
		}

		public void setA(A a) {
			this.a = a;
		}

		@OneToMany(cascade = PERSIST)
		public List<C> getCs() {
			return cs;
		}

		public void setCs(List<C> cs) {
			this.cs = cs;
		}
	}

	@Entity(name = "C")
	@Table(name = "two_hop_id_c")
	public static class C {
		private Long id;
		private B b;

		@Id
		@GeneratedValue
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		@ManyToOne
		@JoinColumn(name = "b_id")
		public B getB() {
			return b;
		}

		public void setB(B b) {
			this.b = b;
		}
	}
}
