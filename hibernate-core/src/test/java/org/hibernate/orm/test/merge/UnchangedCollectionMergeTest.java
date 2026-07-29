/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.merge;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Version;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static jakarta.persistence.CascadeType.ALL;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Reproducer for HHH-3007: merging a detached entity whose collection has
 * not been changed causes the collection to be marked dirty, which in turn
 * increments the {@code @Version} counter even though nothing actually changed.
 */
@Jpa(annotatedClasses = {
		UnchangedCollectionMergeTest.Root.class,
		UnchangedCollectionMergeTest.Leaf.class
})
@Jira("https://hibernate.atlassian.net/browse/HHH-3007")
public class UnchangedCollectionMergeTest {

	@Test
	void mergingUnchangedCollectionMustNotIncrementVersion(EntityManagerFactoryScope scope) {
		// Persist root with one leaf
		scope.inTransaction( em -> {
			Root root = new Root();
			Leaf leaf = new Leaf();
			root.leaves.add( leaf );
			em.persist( root );
		} );

		Root detached = scope.fromTransaction( em -> em.find( Root.class, 1L ) );
		long versionAfterPersist = detached.version;
		detached.leaves = new HashSet<>( detached.leaves );

		// Merge the detached root without touching the collection
		long versionAfterMerge = scope.fromTransaction( em -> em.merge( detached ) ).version;

		// The version must not have changed since nothing was modified
		assertEquals( versionAfterPersist, versionAfterMerge,
				"HHH-3007: version must not be incremented when the collection was not modified during merge" );
	}

	@Entity(name = "Root")
	static class Root {
		@Id
		long id = 1L;

		@Version
		long version;

		@OneToMany(cascade = ALL, fetch = FetchType.EAGER)
		Set<Leaf> leaves = new HashSet<>();
	}

	@Entity(name = "Leaf")
	static class Leaf {
		@Id @GeneratedValue
		Long id;
	}
}
