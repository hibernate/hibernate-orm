package org.hibernate.orm.test.mapping.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.PropertyValueException;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// Aggregate arrays validate the state of each non-null embeddable element.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = AggregateArrayNullabilityTest.Holder.class)
@SessionFactory
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonAggregate.class)
class AggregateArrayNullabilityTest {
	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void nullAndEmptyArraysAreAllowed(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Holder( 1L, null ) );
			session.persist( new Holder( 2L, new Item[0] ) );
			session.persist( new Holder( 3L, new Item[] { new Item( "valid" ) } ) );
		} );
		scope.inTransaction( session -> {
			assertThat( session.find( Holder.class, 1L ).items ).isNull();
			assertThat( session.find( Holder.class, 2L ).items ).isEmpty();
			assertThat( session.find( Holder.class, 3L ).items ).extracting( item -> item.name ).containsExactly( "valid" );
		} );
	}

	@Test
	void nonNullablePropertyIsCheckedInEveryElement(SessionFactoryScope scope) {
		final var failure = assertThrows( PropertyValueException.class, () -> scope.inTransaction( session ->
				session.persist( new Holder( 4L, new Item[] { new Item( "valid" ), new Item( null ) } ) ) ) );
		assertThat( failure.getPropertyName() ).isEqualTo( "items.name" );
	}

	@Entity(name = "AggregateNullabilityHolder")
	public static class Holder {
		@Id
		Long id;
		@JdbcTypeCode(SqlTypes.JSON_ARRAY)
		Item[] items;

		public Holder() {
		}

		Holder(Long id, Item[] items) {
			this.id = id;
			this.items = items;
		}
	}

	@Embeddable
	public static class Item {
		@Column(nullable = false)
		String name;

		public Item() {
		}

		Item(String name) {
			this.name = name;
		}
	}
}
