package org.hibernate.orm.test.merge;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(annotatedClasses = FilteredCollectionSnapshotMergeTest.Owner.class)
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-21124")
class FilteredCollectionSnapshotMergeTest {

	@Test
	void mergingFilteredCollectionPreservesUnseenRows(SessionFactoryScope scope) {
		// Guards against dropping the detached snapshot to fix HHH-21124.
		scope.inTransaction( session -> {
			final var owner = new Owner();
			owner.tags.addAll( Set.of( "visible", "hidden" ) );
			session.persist( owner );
		} );

		final var detached = scope.fromTransaction( session -> {
			session.enableFilter( "visibleTags" );
			final var owner = session.find( Owner.class, 1L );
			assertEquals( Set.of( "visible" ), owner.tags );
			return owner;
		} );
		detached.tags.add( "new" );

		scope.inTransaction( session -> session.merge( detached ) );
		scope.inTransaction( session -> assertEquals(
				Set.of( "visible", "hidden", "new" ), session.find( Owner.class, 1L ).tags
		) );
	}

	@Entity(name = "FilteredSnapshotOwner")
	@FilterDef(name = "visibleTags")
	static class Owner {
		@Id
		long id = 1L;

		@ElementCollection(fetch = FetchType.EAGER)
		@Column(name = "tag")
		@Filter(name = "visibleTags", condition = "tag <> 'hidden'")
		Set<String> tags = new HashSet<>();
	}
}
