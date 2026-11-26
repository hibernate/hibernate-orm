package org.hibernate.orm.test.pagination;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.SqlFragmentAlias;
import org.hibernate.cfg.QuerySettings;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@JiraKey("HHH-21273")
@DomainModel(annotatedClasses = {
		CollectionFetchFilterPaginationTest.Parent.class,
		CollectionFetchFilterPaginationTest.Child.class
})
@ServiceRegistry(settings = @Setting(
		name = QuerySettings.FAIL_ON_PAGINATION_OVER_COLLECTION_FETCH,
		value = "true"
))
@SessionFactory
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsOffsetInSubquery.class)
class CollectionFetchFilterPaginationTest {
	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( int id = 1; id <= 3; id++ ) {
				final var parent = new Parent( id );
				parent.children.add( new Child( id * 10 + 1, parent, id != 2 ) );
				parent.children.add( new Child( id * 10 + 2, parent, false ) );
				session.persist( parent );
			}
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void filteredChildrenAreFetchedForTheRequestedParents(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "activePaginationChildren" ).setParameter( "active", true );
			final var page = session.createSelectionQuery(
					"from FilterPaginationParent p left join fetch p.children order by p.id",
					Parent.class
			).setFirstResult( 1 ).setMaxResults( 2 ).getResultList();

			assertThat( page ).extracting( parent -> parent.id ).containsExactly( 2, 3 );
			assertThat( page.get( 0 ).children ).isEmpty();
			assertThat( page.get( 1 ).children ).extracting( child -> child.id ).containsExactly( 31 );
		} );
	}

	@Test
	void differentFilterValueKeepsTheSameParentPage(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "activePaginationChildren" ).setParameter( "active", false );
			final var page = session.createSelectionQuery(
					"from FilterPaginationParent p left join fetch p.children order by p.id",
					Parent.class
			).setFirstResult( 1 ).setMaxResults( 2 ).getResultList();

			assertThat( page ).extracting( parent -> parent.id ).containsExactly( 2, 3 );
			assertThat( page.get( 0 ).children ).extracting( child -> child.id ).containsExactlyInAnyOrder( 21, 22 );
			assertThat( page.get( 1 ).children ).extracting( child -> child.id ).containsExactly( 32 );
		} );
	}

	@Test
	void paginationWithoutFilterFetchesAllChildren(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var page = session.createSelectionQuery(
					"from FilterPaginationParent p left join fetch p.children order by p.id",
					Parent.class
			).setFirstResult( 1 ).setMaxResults( 2 ).getResultList();

			assertThat( page ).extracting( parent -> parent.id ).containsExactly( 2, 3 );
			assertThat( page.get( 0 ).children ).extracting( child -> child.id ).containsExactlyInAnyOrder( 21, 22 );
			assertThat( page.get( 1 ).children ).extracting( child -> child.id ).containsExactlyInAnyOrder( 31, 32 );
		} );
	}

	@Entity(name = "FilterPaginationParent")
	@Table(name = "filter_page_parent")
	@FilterDef(name = "activePaginationChildren", parameters = @ParamDef(name = "active", type = Boolean.class))
	static class Parent {
		@Id
		int id;

		@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
		@Filter(
				name = "activePaginationChildren",
				condition = "{child}.active_flag = :active",
				deduceAliasInjectionPoints = false,
				aliases = @SqlFragmentAlias(alias = "child", table = "filter_page_child")
		)
		List<Child> children = new ArrayList<>();

		Parent() {
		}

		Parent(int id) {
			this.id = id;
		}
	}

	@Entity(name = "FilterPaginationChild")
	@Table(name = "filter_page_child")
	static class Child {
		@Id
		int id;

		@ManyToOne
		@JoinColumn(name = "parent_id")
		Parent parent;

		@Column(name = "active_flag", nullable = false)
		boolean active;

		Child() {
		}

		Child(int id, Parent parent, boolean active) {
			this.id = id;
			this.parent = parent;
			this.active = active;
		}
	}
}
