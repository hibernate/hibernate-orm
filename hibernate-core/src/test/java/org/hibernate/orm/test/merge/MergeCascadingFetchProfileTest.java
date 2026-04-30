/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.merge;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.Hibernate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@code CascadingFetchProfile.MERGE} only eagerly loads
 * associations reachable through a cascade-eligible path.
 * <p>
 * The model has a {@code Root} entity with two {@link ManyToOne} associations
 * to {@code Parent}: one with {@code cascade=ALL} and one without.
 * Each {@code Parent} owns a lazy {@code cascade=MERGE} collection of {@code Child}.
 * When merging {@code Root}, only the cascade-reachable parent's children
 * should be eagerly initialized.
 *
 * @author Marco Belladelli
 */
@DomainModel(annotatedClasses = {
		MergeCascadingFetchProfileTest.Root.class,
		MergeCascadingFetchProfileTest.Parent.class,
		MergeCascadingFetchProfileTest.Child.class,
})
@SessionFactory(useCollectingStatementInspector = true)
@Jira("https://hibernate.atlassian.net/browse/HHH-20394")
public class MergeCascadingFetchProfileTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var cascadeParent = new Parent( 1L, "cascadeParent" );
			var noCascadeParent = new Parent( 2L, "noCascadeParent" );
			session.persist( cascadeParent );
			session.persist( noCascadeParent );
			for ( int i = 0; i < 5; i++ ) {
				session.persist( new Child( (long) (i + 1), cascadeParent, "c_" + i ) );
				session.persist( new Child( (long) (i + 6), noCascadeParent, "nc_" + i ) );
			}
			session.persist( new Root( 1L, cascadeParent, noCascadeParent ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	public void testMergeCascadeLoads(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		final var detached = new Root( 1L, new Parent( 1L, "cascadeParent" ), new Parent( 2L, "noCascadeParent" ) );

		scope.inTransaction( session -> {
			final var merged = session.merge( detached );

			// cascadeParent.children should be initialized because the entire
			// path Root -> cascadeParent (cascade=ALL) -> children (cascade=ALL) is cascade-reachable
			assertThat( Hibernate.isInitialized( merged.getCascadeParent().getChildren() ) )
					.as( "cascadeParent.children should be initialized "
						+ "because the entire path has cascade=ALL" )
					.isTrue();

			// noCascadeParent is loaded (eager ManyToOne), but its children
			// should NOT be initialized: no cascade path from Root
			assertThat( Hibernate.isInitialized( merged.getNoCascadeParent().getChildren() ) )
					.as( "noCascadeParent.children should not be initialized "
						+ "because Root.noCascadeParent has no cascade" )
					.isFalse();

			// Root + cascadeParent + 5 cascade children + noCascadeParent = 8
			final var entityCount = session
					.getPersistenceContextInternal()
					.getNumberOfManagedEntities();
			assertThat( entityCount )
					.as( "Only Root, both Parents, and the cascade-reachable children should be managed" )
					.isEqualTo( 8 );
		} );

		// 1 SELECT for the initial merge load, no additional lazy-loading or update queries
		inspector.assertExecutedCount( 1 );
		inspector.assertIsSelect( 0 );
	}

	@Test
	public void testMergeCascadeChanges(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		// Build a detached Root with a modified child on the cascade-reachable parent,
		// and a sneaky new child on the non-cascade parent
		final var detachedCascade = new Parent( 1L, "cascadeParent" );
		detachedCascade.getChildren().add( new Child( 1L, detachedCascade, "modified_label" ) );
		final var detachedNoCascade = new Parent( 2L, "noCascadeParent" );
		detachedNoCascade.getChildren().add( new Child( 99L, detachedNoCascade, "sneaky" ) );
		final var detached = new Root( 1L, detachedCascade, detachedNoCascade );

		scope.inTransaction( session -> session.merge( detached ) );

		// 1 SELECT for the initial load + 1 UPDATE for the modified child, no additional lazy-loading queries
		inspector.assertExecutedCount( 2 );
		inspector.assertIsSelect( 0 );
		inspector.assertIsUpdate( 1 );

		scope.inSession( session -> {
			// Verify the modified label was actually persisted through the cascade path
			assertThat( session.find( Child.class, 1L ).getLabel() ).isEqualTo( "modified_label" );
			// Verify the sneaky child was NOT persisted through the non-cascade path
			assertThat( session.find( Child.class, 99L ) ).isNull();
		} );
	}

	@Entity(name = "MergeRoot")
	public static class Root {
		@Id
		private Long id;

		@ManyToOne(cascade = CascadeType.ALL)
		private Parent cascadeParent;

		@ManyToOne
		private Parent noCascadeParent;

		public Root() {
		}

		public Root(Long id, Parent cascadeParent, Parent noCascadeParent) {
			this.id = id;
			this.cascadeParent = cascadeParent;
			this.noCascadeParent = noCascadeParent;
		}

		public Parent getCascadeParent() {
			return cascadeParent;
		}

		public Parent getNoCascadeParent() {
			return noCascadeParent;
		}
	}

	@Entity(name = "MergeParent")
	public static class Parent {
		@Id
		private Long id;

		private String name;

		@OneToMany(mappedBy = "parent", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
		private List<Child> children = new ArrayList<>();

		public Parent() {
		}

		public Parent(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public List<Child> getChildren() {
			return children;
		}
	}

	@Entity(name = "MergeChild")
	public static class Child {
		@Id
		private Long id;

		@ManyToOne
		private Parent parent;

		private String label;

		public Child() {
		}

		public Child(Long id, Parent parent, String label) {
			this.id = id;
			this.parent = parent;
			this.label = label;
		}

		public String getLabel() {
			return label;
		}
	}
}
