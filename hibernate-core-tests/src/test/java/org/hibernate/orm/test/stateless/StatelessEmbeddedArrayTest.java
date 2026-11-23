package org.hibernate.orm.test.stateless;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = {
		StatelessEmbeddedArrayTest.DirectArrayOwner.class,
		StatelessEmbeddedArrayTest.EmbeddedArrayOwner.class
})
@SessionFactory
@JiraKey("HHH-20997")
class StatelessEmbeddedArrayTest {
	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void testInsertPreservesNonNullArray(SessionFactoryScope scope) {
		final var owner = new EmbeddedArrayOwner();
		final String[] tags = { "first", "second" };
		owner.values.tags = tags;

		scope.inStatelessTransaction( session -> session.insert( owner ) );

		assertThat( owner.values.tags ).isSameAs( tags ).containsExactly( "first", "second" );
		scope.inTransaction( session -> assertThat( session.createNativeQuery(
				"select tags from embedded_array_tags order by tag_order", String.class
		).getResultList() ).containsExactly( "first", "second" ) );
	}

	@Test
	void testDeleteWithNullDirectArray(SessionFactoryScope scope) {
		final var owner = new DirectArrayOwner();
		owner.tags = new String[] { "original" };
		scope.inTransaction( session -> session.persist( owner ) );
		owner.tags = null;
		scope.inStatelessTransaction( session -> session.delete( owner ) );
		scope.inTransaction( session -> {
			assertThat( session.find( DirectArrayOwner.class, owner.id ) ).isNull();
			assertThat( session.createNativeQuery( "select count(*) from direct_array_tags", Long.class )
					.getSingleResult() ).isZero();
		} );
		assertThat( owner.tags ).isNull();
	}

	@Test
	void testInsertWithNullComponent(SessionFactoryScope scope) {
		final var owner = new EmbeddedArrayOwner();
		owner.values = null;
		scope.inStatelessTransaction( session -> session.insert( owner ) );
		assertThat( owner.id ).isNotNull();
		assertThat( owner.values ).isNull();
		assertCollectionEmpty( scope );
	}

	@Test
	void testUpdateWithNullComponent(SessionFactoryScope scope) {
		final var owner = seed( scope );
		owner.values = null;
		scope.inStatelessTransaction( session -> session.update( owner ) );
		assertThat( owner.values ).isNull();
		assertCollectionEmpty( scope );
	}

	@Test
	void testUpdateWithNullArray(SessionFactoryScope scope) {
		final var owner = seed( scope );
		owner.values.tags = null;
		scope.inStatelessTransaction( session -> session.update( owner ) );
		assertThat( owner.values.tags ).isNull();
		assertCollectionEmpty( scope );
	}

	@Test
	void testDeleteWithNullComponent(SessionFactoryScope scope) {
		final var owner = seed( scope );
		owner.values = null;
		scope.inStatelessTransaction( session -> session.delete( owner ) );
		scope.inTransaction( session -> assertThat( session.find( EmbeddedArrayOwner.class, owner.id ) ).isNull() );
		assertThat( owner.values ).isNull();
		assertCollectionEmpty( scope );
	}

	private static EmbeddedArrayOwner seed(SessionFactoryScope scope) {
		final var owner = new EmbeddedArrayOwner();
		owner.values.tags = new String[] { "original" };
		scope.inTransaction( session -> session.persist( owner ) );
		return owner;
	}

	private static void assertCollectionEmpty(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createNativeQuery(
				"select count(*) from embedded_array_tags", Long.class
		).getSingleResult() ).isZero() );
	}

	@Entity(name = "DirectArrayOwner")
	static class DirectArrayOwner {
		@Id
		@GeneratedValue
		Long id;

		@ElementCollection
		@CollectionTable(name = "direct_array_tags")
		@OrderColumn(name = "tag_order")
		String[] tags;
	}

	@Entity(name = "EmbeddedArrayOwner")
	static class EmbeddedArrayOwner {
		@Id
		@GeneratedValue
		Long id;

		@Embedded
		ArrayValues values = new ArrayValues();
	}

	@Embeddable
	static class ArrayValues {
		@ElementCollection
		@CollectionTable(name = "embedded_array_tags")
		@OrderColumn(name = "tag_order")
		String[] tags;
	}
}
