package org.hibernate.orm.test.refresh;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(overrideCacheStrategy = false, annotatedClasses = {
		NestedCascadeRefreshTest.Root.class,
		NestedCascadeRefreshTest.Parent.class,
		NestedCascadeRefreshTest.Child.class
})
@SessionFactory
@ServiceRegistry(settings = @Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "false"))
@JiraKey("HHH-21009")
class NestedCascadeRefreshTest {
	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void refreshInitializedNestedCollection(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Root root = createRoot();
			final Parent parent = root.parents.get( 0 );
			final Child child = parent.children.get( 0 );
			session.persist( root );
			session.flush();

			assertThat( session.createNativeMutationQuery( "update nested_refresh_child set label = 'new' where id = 3" )
					.executeUpdate() ).isEqualTo( 1 );
			assertThat( child.label ).isEqualTo( "old" );

			session.refresh( root );

			assertThat( child.label ).isEqualTo( "new" );
			assertThat( root.parents.get( 0 ) ).isSameAs( parent );
			assertThat( parent.children.get( 0 ) ).isSameAs( child );
		} );
	}

	@ParameterizedTest
	@CsvSource({
			"false, false, false",
			"false, true, false",
			"true, false, false",
			"true, true, false",
			"true, true, true"
	})
	void refreshManagedGrandchild(
			boolean initializeRoot,
			boolean initializeParent,
			boolean readOnly,
			SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( createRoot() ) );
		scope.inTransaction( session -> {
			final Root root = session.find( Root.class, 1L );
			final Parent parent = session.find( Parent.class, 2L );
			final Child child = session.find( Child.class, 3L );
			if ( initializeRoot ) {
				Hibernate.initialize( root.parents );
			}
			if ( initializeParent ) {
				Hibernate.initialize( parent.children );
			}
			assertThat( Hibernate.isInitialized( root.parents ) ).isEqualTo( initializeRoot );
			assertThat( Hibernate.isInitialized( parent.children ) ).isEqualTo( initializeParent );
			session.setReadOnly( parent, readOnly );
			session.setReadOnly( child, readOnly );
			session.createNativeMutationQuery( "update nested_refresh_child set label = 'new' where id = 3" )
					.executeUpdate();

			session.refresh( root );

			assertThat( child.label ).isEqualTo( "new" );
			assertThat( root.parents ).containsExactly( parent );
			assertThat( parent.children ).containsExactly( child );
			assertThat( session.isReadOnly( parent ) ).isEqualTo( readOnly );
			assertThat( session.isReadOnly( child ) ).isEqualTo( readOnly );
		} );
	}

	@Test
	void refreshNestedCollectionMembership(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Root root = createRoot();
			final Parent parent = root.parents.get( 0 );
			final Child removed = new Child();
			removed.id = 5;
			removed.label = "removed";
			removed.parent = parent;
			parent.children.add( removed );
			session.persist( root );
		} );
		scope.inTransaction( session -> {
			final Root root = session.find( Root.class, 1L );
			Hibernate.initialize( root.parents.get( 0 ).children );
		} );
		if ( scope.getSessionFactory().getSessionFactoryOptions().isSecondLevelCacheEnabled() ) {
			assertThat( scope.getSessionFactory().getCache()
					.containsCollection( Parent.class.getName() + ".children", 2L ) ).isTrue();
		}
		scope.inTransaction( session -> {
			final Root root = session.find( Root.class, 1L );
			final Parent parent = root.parents.get( 0 );
			Hibernate.initialize( parent.children );
			final Child child = session.find( Child.class, 3L );
			// Bypass Hibernate's mutation cache invalidation as well as its managed state.
			session.doWork( connection -> {
				try ( var statement = connection.createStatement() ) {
					assertThat( statement.executeUpdate( "update nested_refresh_child set label = 'new' where id = 3" ) )
							.isEqualTo( 1 );
					assertThat( statement.executeUpdate( "delete from nested_refresh_child where id = 5" ) )
							.isEqualTo( 1 );
					assertThat( statement.executeUpdate(
							"insert into nested_refresh_child (id, label, parent_id) values (4, 'added', 2)" ) )
							.isEqualTo( 1 );
				}
			} );

			session.refresh( root );

			assertThat( child.label ).isEqualTo( "new" );
			assertThat( parent.children ).contains( child );
			assertThat( parent.children ).extracting( element -> element.id ).containsExactlyInAnyOrder( 3L, 4L );
			session.flush();
			session.doWork( connection -> {
				try ( var statement = connection.createStatement() ) {
					statement.executeUpdate( "update nested_refresh_child set label = 'newer' where id = 3" );
				}
			} );

			session.refresh( root );

			assertThat( child.label ).isEqualTo( "newer" );
			assertThat( parent.children ).extracting( element -> element.id ).containsExactlyInAnyOrder( 3L, 4L );
		} );
	}

	@Test
	void refreshStopsAtNonCascadedCollection(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Root root = createRoot();
			final Parent otherParent = new Parent();
			otherParent.id = 20;
			otherParent.uncascadedRoot = root;
			root.otherParents.add( otherParent );
			final Child otherChild = new Child();
			otherChild.id = 30;
			otherChild.parent = otherParent;
			otherChild.label = "old";
			otherParent.children.add( otherChild );
			session.persist( root );
		} );
		scope.inTransaction( session -> {
			final Child otherChild = session.find( Child.class, 30L );
			final Root root = session.getReference( Root.class, 1L );
			assertThat( Hibernate.isInitialized( root ) ).isFalse();
			session.createNativeMutationQuery( "update nested_refresh_child set label = 'new' where id in (3, 30)" ).executeUpdate();

			session.refresh( root );

			assertThat( otherChild.label ).isEqualTo( "old" );
			assertThat( session.find( Child.class, 3L ).label ).isEqualTo( "new" );
		} );
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void refreshDoesNotRefreshOtherCollectionOwners(boolean subselect, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( createRoot() );
			final Root otherRoot = createRoot();
			otherRoot.id = 10;
			final Parent otherParent = otherRoot.parents.get( 0 );
			otherParent.id = 20;
			otherParent.children.get( 0 ).id = 30;
			session.persist( otherRoot );
		} );
		scope.inTransaction( session -> {
			session.setSubselectFetchingEnabled( subselect );
			final Root root = session.find( Root.class, 1L );
			final List<Parent> parents = session.createSelectionQuery(
					"from NestedRefreshParent p order by p.id", Parent.class ).getResultList();
			final Parent otherParent = parents.get( 1 );
			final Child child = session.find( Child.class, 3L );
			final Child otherChild = session.find( Child.class, 30L );
			assertThat( Hibernate.isInitialized( otherParent.children ) ).isFalse();
			session.doWork( connection -> {
				try ( var statement = connection.createStatement() ) {
					assertThat( statement.executeUpdate( "update nested_refresh_child set label = 'new' where id in (3, 30)" ) )
							.isEqualTo( 2 );
				}
			} );

			session.refresh( root );

			assertThat( child.label ).isEqualTo( "new" );
			assertThat( otherChild.label ).isEqualTo( "old" );
			assertThat( Hibernate.isInitialized( otherParent.children ) ).isFalse();
		} );
	}

	private static Root createRoot() {
		final Root root = new Root();
		root.id = 1;
		final Parent parent = new Parent();
		parent.id = 2;
		parent.root = root;
		root.parents.add( parent );
		final Child child = new Child();
		child.id = 3;
		child.parent = parent;
		child.label = "old";
		parent.children.add( child );
		return root;
	}

	@Entity(name = "NestedRefreshRoot")
	@Table(name = "nested_refresh_root")
	public static class Root {
		@Id
		private long id;

		@OneToMany(mappedBy = "root", cascade = CascadeType.ALL)
		private List<Parent> parents = new ArrayList<>();

		@OneToMany(mappedBy = "uncascadedRoot", cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
		@Fetch(FetchMode.SELECT)
		private List<Parent> otherParents = new ArrayList<>();
	}

	@Entity(name = "NestedRefreshParent")
	@Table(name = "nested_refresh_parent")
	public static class Parent {
		@Id
		private long id;

		@ManyToOne(fetch = FetchType.LAZY)
		private Root root;

		@ManyToOne(fetch = FetchType.LAZY)
		private Root uncascadedRoot;

		@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
		@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
		@BatchSize(size = 10)
		private List<Child> children = new ArrayList<>();
	}

	@Entity(name = "NestedRefreshChild")
	@Table(name = "nested_refresh_child")
	public static class Child {
		@Id
		private long id;

		private String label;

		@ManyToOne(fetch = FetchType.LAZY)
		private Parent parent;
	}
}
