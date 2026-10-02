package org.hibernate.orm.test.query.hql;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.criteria.JoinType;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Covers ordinary counts of embeddables and distinct counts of entities with composite identifiers.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = EmbeddableCountRegressionTest.CountedEntity.class)
@SessionFactory
public class EmbeddableCountRegressionTest {
	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( int i = 1; i <= 3; i++ ) {
				final var entity = new CountedEntity();
				entity.id = new CompositeId( 1, i );
				entity.details = new Details();
				entity.details.number = i;
				entity.details.name = "entity" + i;
				entity.labels.addAll( List.of( "first", "second" ) );
				session.persist( entity );
			}
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey("HHH-8589")
	public void testCountEmbeddable(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertThat( session.createQuery( "select count(e.details) from CountedEntity e", Long.class )
					.getSingleResult() ).isEqualTo( 3L );

			final var builder = session.getCriteriaBuilder();
			final var query = builder.createQuery( Long.class );
			final var root = query.from( CountedEntity.class );
			query.select( builder.count( root.get( "details" ) ) );
			assertThat( session.createQuery( query ).getSingleResult() ).isEqualTo( 3L );
		} );
	}

	@Test
	@JiraKey("HHH-9814")
	public void testCountDistinctCompositeIdentifier(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertThat( session.createQuery(
					"select count(e) from CountedEntity e left join e.labels label", Long.class
			).getSingleResult() ).isEqualTo( 6L );
			assertThat( session.createQuery(
					"select count(distinct e) from CountedEntity e left join e.labels label", Long.class
			).getSingleResult() ).isEqualTo( 3L );

			final var builder = session.getCriteriaBuilder();
			final var query = builder.createQuery( Long.class );
			final var root = query.from( CountedEntity.class );
			root.join( "labels", JoinType.LEFT );
			query.select( builder.countDistinct( root ) );
			assertThat( session.createQuery( query ).getSingleResult() ).isEqualTo( 3L );
		} );
	}

	@Entity(name = "CountedEntity")
	public static class CountedEntity {
		@EmbeddedId
		CompositeId id;

		@Embedded
		Details details;

		@ElementCollection
		List<String> labels = new ArrayList<>();
	}

	@Embeddable
	public static class Details {
		@Column(name = "detail_number")
		Integer number;
		String name;
	}

	@Embeddable
	public static class CompositeId implements Serializable {
		Integer firstPart;
		Integer secondPart;

		public CompositeId() {
		}

		CompositeId(Integer firstPart, Integer secondPart) {
			this.firstPart = firstPart;
			this.secondPart = secondPart;
		}

		@Override
		public boolean equals(Object other) {
			return other instanceof CompositeId that
					&& Objects.equals( firstPart, that.firstPart )
					&& Objects.equals( secondPart, that.secondPart );
		}

		@Override
		public int hashCode() {
			return Objects.hash( firstPart, secondPart );
		}
	}
}
