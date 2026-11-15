package org.hibernate.orm.test.merge;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;

import org.hibernate.annotations.Bag;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(annotatedClasses = DetachedCollectionSnapshotMergeTest.Owner.class)
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-21124")
class DetachedCollectionSnapshotMergeTest {

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@ParameterizedTest
	@ValueSource(ints = { 1, 2 })
	void mergeDirtyBagMatchingDetachedSnapshot(int mergeCount, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var owner = new Owner();
			owner.replaceContents( "A" );
			session.persist( owner );
		} );

		final var detached = scope.fromTransaction( session -> session.find( Owner.class, 1L ) );
		detached.replaceContents( "A" );
		assertTrue( ( (PersistentCollection<?>) detached.bag ).isDirty() );

		scope.inTransaction( session -> session.find( Owner.class, 1L ).replaceContents( "B" ) );

		scope.inTransaction( session -> {
			for ( int i = 0; i < mergeCount; i++ ) {
				assertContents( session.merge( detached ), "A" );
			}
		} );

		scope.inTransaction( session -> assertContents( session.find( Owner.class, 1L ), "A" ) );
	}

	private static void assertContents(Owner owner, String value) {
		assertEquals( List.of( value ), new ArrayList<>( owner.bag ) );
	}

	@Entity(name = "SnapshotOwner")
	static class Owner {
		@Id
		long id = 1L;

		@ElementCollection(fetch = FetchType.EAGER)
		@Bag
		List<String> bag = new ArrayList<>();

		void replaceContents(String value) {
			bag.clear();
			bag.add( value );
		}
	}
}
