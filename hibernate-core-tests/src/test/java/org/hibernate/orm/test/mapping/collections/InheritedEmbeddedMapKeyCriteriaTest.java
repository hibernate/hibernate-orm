package org.hibernate.orm.test.mapping.collections;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.MapJoin;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies Criteria map-key resolution for an embeddable inherited from a mapped superclass.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = InheritedEmbeddedMapKeyCriteriaTest.City.class)
@SessionFactory
@JiraKey("HHH-15368")
public class InheritedEmbeddedMapKeyCriteriaTest {
	@BeforeAll
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new City( 1, "native_match", Map.of() ) );
			session.persist( new City( 2, "localized", Map.of(
					"NAME_match", "translated_match",
					"NAME_alternative", "alternative_match"
			) ) );
			session.persist( new City( 3, "other", Map.of( "DESCRIPTION_match", "translated_match" ) ) );
			session.persist( new City( 4, "empty", Map.of() ) );
			session.persist( new City( 5, "unrelated", Map.of( "NAME_other", "unrelated" ) ) );
		} );
	}

	@AfterAll
	void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from LocalizedCity" ).executeUpdate() );
	}

	@Test
	void testMapKeyPredicate(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var cb = session.getCriteriaBuilder();
			final var query = cb.createQuery( Integer.class );
			final var root = query.from( City.class );
			final MapJoin<Localizations, String, String> map = root.<Localizations>join( "localizations" )
					.joinMap( "data", JoinType.LEFT );
			query.select( root.get( "id" ) ).distinct( true ).where(
					cb.or(
							cb.like( root.get( "nativeName" ), "%match%" ),
							cb.like( map.key(), "NAME%" )
					)
			).orderBy( cb.asc( root.get( "id" ) ) );

			assertThat( session.createQuery( query ).getResultList() ).containsExactly( 1, 2, 5 );
		} );
	}

	@Test
	void testMapKeyAndValuePredicates(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var cb = session.getCriteriaBuilder();
			final var query = cb.createQuery( Integer.class );
			final var root = query.from( City.class );
			final MapJoin<Localizations, String, String> map = root.<Localizations>join( "localizations" )
					.joinMap( "data", JoinType.LEFT );
			query.select( root.get( "id" ) ).distinct( true ).where(
					cb.or(
							cb.like( root.get( "nativeName" ), "%match%" ),
							cb.and( cb.like( map.key(), "NAME%" ), cb.like( map.value(), "%match%" ) )
					)
			).orderBy( cb.asc( root.get( "id" ) ) );

			assertThat( session.createQuery( query ).getResultList() ).containsExactly( 1, 2 );
		} );
	}

	@MappedSuperclass
	public static abstract class AbstractLocEntity {
		@Id
		Integer id;

		@Embedded
		Localizations localizations = new Localizations();
	}

	@Embeddable
	public static class Localizations {
		@ElementCollection
		@MapKeyColumn(name = "tkey", nullable = false)
		@Column(name = "locVal")
		Map<String, String> data = new HashMap<>();
	}

	@Entity(name = "LocalizedCity")
	@Table(name = "hhh15368_city")
	public static class City extends AbstractLocEntity {
		String nativeName;

		public City() {
		}

		City(int id, String nativeName, Map<String, String> data) {
			this.id = id;
			this.nativeName = nativeName;
			localizations.data.putAll( data );
		}
	}
}
