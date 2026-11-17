package org.hibernate.orm.test.filter;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = {
		FilterConditionPrecedenceTest.Item.class,
		FilterConditionPrecedenceTest.Owner.class
})
@SessionFactory
@JiraKey("HHH-21130")
class FilterConditionPrecedenceTest {
	@BeforeEach
	void prepare(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Owner owner = new Owner( 1 );
			Owner otherOwner = new Owner( 2 );
			session.persist( owner );
			session.persist( otherOwner );
			session.persist( new Item( 1, false, otherOwner ) );
			session.persist( new Item( 2, false, owner ) );
			session.persist( new Item( 3, true, owner ) );
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void queryRetainsIdRestriction(boolean includeDeleted, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "visible" ).setParameter( "includeDeleted", includeDeleted );
			assertThat( session.createQuery(
					"select i.id from FilterItem i where i.id = :id", Integer.class )
					.setParameter( "id", 2 ).getResultList() ).containsExactly( 2 );
		} );
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void collectionRetainsOwnerRestriction(boolean includeDeleted, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "visible" ).setParameter( "includeDeleted", includeDeleted );
			Owner owner = session.find( Owner.class, 1 );
			assertThat( owner.items ).extracting( item -> item.id )
					.containsExactlyInAnyOrderElementsOf( includeDeleted ? List.of( 2, 3 ) : List.of( 2 ) );
		} );
	}

	@Test
	void parameterlessFilterRetainsIdRestriction(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "parameterless" );
			assertThat( session.createQuery(
					"select i.id from FilterItem i where i.id = :id", Integer.class )
					.setParameter( "id", 2 ).getResultList() ).containsExactly( 2 );
		} );
	}

	@Test
	void enabledFiltersAreCombinedWithAnd(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "visible" ).setParameter( "includeDeleted", false );
			session.enableFilter( "selectedIds" );
			assertThat( session.createQuery(
					"select i.id from FilterItem i", Integer.class ).getResultList() )
					.containsExactly( 2 );
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = { "parenthesized", "nonOr" })
	void existingConditionFormsRetainIdRestriction(String filterName, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( filterName );
			assertThat( session.createQuery(
					"select i.id from FilterItem i where i.id = :id", Integer.class )
					.setParameter( "id", 2 ).getResultList() ).containsExactly( 2 );
		} );
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void fetchedCollectionRetainsOwnerRestriction(boolean includeDeleted, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "visible" ).setParameter( "includeDeleted", includeDeleted );
			Owner owner = session.createQuery(
					"select o from FilterOwner o left join fetch o.items where o.id = :id", Owner.class )
					.setParameter( "id", 1 ).getSingleResult();
			assertThat( owner.items ).extracting( item -> item.id )
					.containsExactlyInAnyOrderElementsOf( includeDeleted ? List.of( 2, 3 ) : List.of( 2 ) );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsDmlTargetColumnQualifier.class)
	void bulkUpdateRetainsIdRestriction(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "visible" ).setParameter( "includeDeleted", true );
			assertThat( session.createMutationQuery(
					"update FilterItem set deleted = true where id = :id" )
					.setParameter( "id", 2 ).executeUpdate() ).isEqualTo( 1 );
		} );
		scope.inTransaction( session -> {
			assertThat( session.createQuery(
					"select i.id from FilterItem i where i.deleted = false", Integer.class )
					.getResultList() ).containsExactly( 1 );
		} );
	}

	@Entity(name = "FilterItem")
	@Table(name = "filter_item")
	@FilterDef(
			name = "visible",
			defaultCondition = ":includeDeleted = true or deleted = false",
			parameters = @ParamDef(name = "includeDeleted", type = Boolean.class)
	)
	@FilterDef(name = "parameterless", defaultCondition = "id = 1 or deleted = false")
	@FilterDef(name = "selectedIds", defaultCondition = "id = 2 or id = 3")
	@FilterDef(name = "parenthesized", defaultCondition = "(id = 1 or deleted = false)")
	@FilterDef(name = "nonOr", defaultCondition = "deleted = false")
	@Filter(name = "visible")
	@Filter(name = "parameterless")
	@Filter(name = "selectedIds")
	@Filter(name = "parenthesized")
	@Filter(name = "nonOr")
	static class Item {
		@Id
		Integer id;
		boolean deleted;

		@ManyToOne(fetch = FetchType.LAZY)
		Owner owner;

		Item() {
		}

		Item(Integer id, boolean deleted, Owner owner) {
			this.id = id;
			this.deleted = deleted;
			this.owner = owner;
		}
	}

	@Entity(name = "FilterOwner")
	@Table(name = "filter_owner")
	static class Owner {
		@Id
		Integer id;

		@OneToMany(mappedBy = "owner")
		@Filter(name = "visible")
		List<Item> items;

		Owner() {
		}

		Owner(Integer id) {
			this.id = id;
		}
	}
}
