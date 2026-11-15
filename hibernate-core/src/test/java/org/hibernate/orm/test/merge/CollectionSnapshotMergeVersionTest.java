package org.hibernate.orm.test.merge;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(annotatedClasses = CollectionSnapshotMergeVersionTest.Owner.class)
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-21124")
class CollectionSnapshotMergeVersionTest {

	@Test
	void mergingBareCollectionRevertsManagedChangeWithoutIncrementingVersion(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var owner = new Owner();
			owner.tags.add( "original" );
			session.persist( owner );
		} );
		final var detached = scope.fromTransaction( session -> session.find( Owner.class, 1L ) );
		final long version = detached.version;
		detached.tags = new HashSet<>( detached.tags );

		scope.inTransaction( session -> {
			session.find( Owner.class, 1L ).tags.add( "temporary" );
			session.merge( detached );
		} );
		scope.inTransaction( session -> {
			final var owner = session.find( Owner.class, 1L );
			assertEquals( Set.of( "original" ), owner.tags );
			assertEquals( version, owner.version );
		} );
	}

	@Entity(name = "VersionedSnapshotOwner")
	static class Owner {
		@Id
		long id = 1L;

		@Version
		long version;

		@ElementCollection(fetch = FetchType.EAGER)
		Set<String> tags = new HashSet<>();
	}
}
