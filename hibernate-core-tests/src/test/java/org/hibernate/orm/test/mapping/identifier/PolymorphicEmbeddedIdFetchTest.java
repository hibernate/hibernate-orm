package org.hibernate.orm.test.mapping.identifier;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;

import org.hibernate.Hibernate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Fetches a polymorphic association inside the identifier of a fetched entity.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = {
		PolymorphicEmbeddedIdFetchTest.Start.class,
		PolymorphicEmbeddedIdFetchTest.Relation.class,
		PolymorphicEmbeddedIdFetchTest.Base.class,
		PolymorphicEmbeddedIdFetchTest.Sub1.class,
		PolymorphicEmbeddedIdFetchTest.Sub2.class
})
@SessionFactory
@JiraKey("HHH-10292")
class PolymorphicEmbeddedIdFetchTest {
	@BeforeEach
	void populate(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var first = new Sub1();
			first.id = 1L;
			first.detail = "first";
			var second = new Sub2();
			second.id = 2L;
			second.detail = "second";
			session.persist( first );
			session.persist( second );
			for ( var base : new Base[] { first, second } ) {
				var relation = new Relation();
				relation.id = new RelationId();
				relation.id.name = "relation";
				relation.id.base = base;
				session.persist( relation );
				var start = new Start();
				start.id = base.id;
				start.relation = relation;
				session.persist( start );
			}
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void testWithoutFetchJoin(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var start = session.find( Start.class, 1L );
			assertThat( Hibernate.isInitialized( start.relation ) ).isFalse();
			Hibernate.initialize( start.relation );
			var relation = (Relation) Hibernate.unproxy( start.relation );
			assertThat( Hibernate.isInitialized( relation.id.base ) ).isFalse();
		} );
	}

	@Test
	void testInnerJoinFetch(SessionFactoryScope scope) {
		assertFetch( scope, "join fetch" );
	}

	@Test
	void testLeftJoinFetch(SessionFactoryScope scope) {
		assertFetch( scope, "left join fetch" );
	}

	private void assertFetch(SessionFactoryScope scope, String join) {
		var results = scope.fromTransaction( session -> {
			var starts = session.createQuery(
					"select s from PolyIdStart s " + join + " s.relation r "
							+ join + " r.id.base order by s.id", Start.class ).getResultList();
			assertThat( starts ).hasSize( 2 );
			for ( var start : starts ) {
				assertThat( Hibernate.isInitialized( start.relation ) ).isTrue();
				assertThat( Hibernate.isInitialized( start.relation.id.base ) ).isTrue();
			}
			return starts;
		} );
		assertThat( Hibernate.unproxy( results.get( 0 ).relation.id.base ) )
				.isInstanceOfSatisfying( Sub1.class, base -> assertThat( base.detail ).isEqualTo( "first" ) );
		assertThat( Hibernate.unproxy( results.get( 1 ).relation.id.base ) )
				.isInstanceOfSatisfying( Sub2.class, base -> assertThat( base.detail ).isEqualTo( "second" ) );
	}

	@Entity(name = "PolyIdStart")
	public static class Start {
		@Id
		Long id;
		@ManyToOne(fetch = FetchType.LAZY)
		Relation relation;
	}

	@Entity(name = "PolyIdRelation")
	public static class Relation {
		@EmbeddedId
		RelationId id;
	}

	@Embeddable
	public static class RelationId implements Serializable {
		String name;
		@ManyToOne(fetch = FetchType.LAZY)
		Base base;

		@Override
		public boolean equals(Object object) {
			return object instanceof RelationId other
					&& Objects.equals( name, other.name ) && Objects.equals( base.getId(), other.base.getId() );
		}

		@Override
		public int hashCode() {
			return Objects.hash( name, base.getId() );
		}
	}

	@Entity(name = "PolyIdBase")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public static abstract class Base {
		@Id
		Long id;

		public Long getId() {
			return id;
		}
	}

	@Entity(name = "PolyIdSub1")
	public static class Sub1 extends Base {
		String detail;
	}

	@Entity(name = "PolyIdSub2")
	public static class Sub2 extends Base {
		String detail;
	}
}
