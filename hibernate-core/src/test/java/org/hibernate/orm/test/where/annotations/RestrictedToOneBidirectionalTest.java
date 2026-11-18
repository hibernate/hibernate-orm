package org.hibernate.orm.test.where.annotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@JiraKey("HHH-21131")
@DomainModel(annotatedClasses = { RestrictedToOneBidirectionalTest.Parent.class,
		RestrictedToOneBidirectionalTest.Child.class })
@SessionFactory(useCollectingStatementObserver = true)
class RestrictedToOneBidirectionalTest {
	@BeforeEach
	void prepare(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var parent = new Parent();
			parent.id = 1L;
			session.persist( parent );
			var child = new Child();
			child.id = 2L;
			child.parent = parent;
			session.persist( child );
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	void bidirectionalFetchReusesVisibleParent(SessionFactoryScope scope) {
		assertReusesParent( scope, false );
	}

	@Test
	void bidirectionalFetchReusesParentVisibleToEntityFilter(SessionFactoryScope scope) {
		assertReusesParent( scope, true );
	}

	private void assertReusesParent(SessionFactoryScope scope, boolean filtered) {
		scope.inTransaction( session -> {
			if ( filtered ) {
				session.enableFilter( "bidirectionalEntity" ).setParameter( "id", 1L );
			}
			final var observer = scope.getCollectingStatementObserver();
			observer.clear();
			var parent = session.createSelectionQuery(
					"from BidirectionalRestrictedParent p join fetch p.children", Parent.class ).getSingleResult();
			assertThat( parent.children ).hasSize( 1 );
			assertThat( parent.children.get( 0 ).parent ).isSameAs( parent );
			assertThat( observer.getSqlQueries() ).hasSize( 1 );
			final var sql = observer.getSqlQueries().get( 0 );
			assertThat( sql.toLowerCase( Locale.ROOT ).split( " join ", -1 ).length - 1 ).as( sql ).isEqualTo( 1 );
		} );
	}

	@Test
	void associationFilterStillExcludesVisibleParent(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "bidirectionalAssociation" ).setParameter( "id", 99L );
			var parent = session.createSelectionQuery(
					"from BidirectionalRestrictedParent p join fetch p.children", Parent.class ).getSingleResult();
			assertThat( parent.children ).hasSize( 1 );
			assertThat( parent.children.get( 0 ).parent ).isNull();
		} );
	}

	@Entity(name = "BidirectionalRestrictedParent")
	@Table(name = "bidir_restricted_parent")
	@SQLRestriction("deleted = 0")
	@FilterDef(name = "bidirectionalEntity", applyToLoadByKey = true, parameters = @ParamDef(name = "id", type = Long.class))
	@Filter(name = "bidirectionalEntity", condition = "id = :id")
	@FilterDef(name = "bidirectionalAssociation", parameters = @ParamDef(name = "id", type = Long.class))
	static class Parent {
		@Id Long id;
		int deleted;
		@OneToMany(mappedBy = "parent") List<Child> children = new ArrayList<>();
	}

	@Entity(name = "BidirectionalRestrictedChild")
	@Table(name = "bidir_restricted_child")
	@SQLRestriction("deleted = 0")
	static class Child {
		@Id Long id;
		int deleted;
		@ManyToOne @JoinColumn(name = "parent_id")
		@Filter(name = "bidirectionalAssociation", condition = "id = :id")
		Parent parent;
	}
}
