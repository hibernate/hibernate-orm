package org.hibernate.orm.test.mapping.where;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.SoftDelete;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Grouping or sorting by a to-one whose target entity has a restriction uses
 * its foreign key, so that no row is lost to an inner join with the target,
 * and every column which the select list reads for the association is grouped.
 */
@DomainModel(annotatedClasses = {
		ToOneSQLRestrictionGroupByTest.Parent.class,
		ToOneSQLRestrictionGroupByTest.Child.class,
		ToOneSQLRestrictionGroupByTest.Target.class,
		ToOneSQLRestrictionGroupByTest.Dangling.class,
		ToOneSQLRestrictionGroupByTest.SoftParent.class,
		ToOneSQLRestrictionGroupByTest.SoftChild.class
})
@SessionFactory(useCollectingStatementInspector = true)
@JiraKey("HHH-21078")
class ToOneSQLRestrictionGroupByTest {

	@Entity(name = "Parent")
	@Table(name = "parents")
	@SQLRestriction("deleted = false")
	static class Parent {
		@Id
		Long id;
		boolean deleted;
	}

	@Entity(name = "Child")
	@Table(name = "children")
	static class Child {
		@Id
		Long id;
		String name;
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "parent_id")
		Parent parent;
	}

	@Entity(name = "Target")
	@Table(name = "targets")
	static class Target {
		@Id
		Long id;
	}

	@Entity(name = "Dangling")
	@Table(name = "danglings")
	static class Dangling {
		@Id
		Long id;
		@ManyToOne
		@NotFound(action = NotFoundAction.IGNORE)
		@JoinColumn(name = "target_id")
		Target target;
	}

	@Entity(name = "SoftParent")
	@Table(name = "soft_parents")
	@SoftDelete
	static class SoftParent {
		@Id
		Long id;
	}

	@Entity(name = "SoftChild")
	@Table(name = "soft_children")
	static class SoftChild {
		@Id
		Long id;
		@ManyToOne
		@JoinColumn(name = "parent_id")
		SoftParent parent;
	}

	@BeforeEach
	void createData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var visible = parent( session, 1L, false );
			final var hidden = parent( session, 2L, true );
			child( session, 10L, "a", visible );
			child( session, 11L, "b", hidden );
			child( session, 12L, "c", null );
			child( session, 13L, "d", visible );
		} );
	}

	@AfterEach
	void dropData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void groupBySelectedOwner(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		final List<Child> children = scope.fromTransaction( session -> session.createSelectionQuery(
				"select c from Child c group by c.id, c.name, c.parent order by c.id", Child.class ).getResultList() );
		assertThat( children ).extracting( child -> child.id ).containsExactly( 10L, 11L, 12L, 13L );
		assertNoInnerJoin( inspector );
		assertThat( inspector.getSqlQueries().get( 0 ).toLowerCase() ).contains( "parent_id" );
	}

	@Test
	void groupByOwner(SessionFactoryScope scope) {
		final List<Child> children = scope.fromTransaction( session -> session.createSelectionQuery(
				"select c from Child c group by c order by c.id", Child.class ).getResultList() );
		assertThat( children ).extracting( child -> child.id ).containsExactly( 10L, 11L, 12L, 13L );
	}

	@Test
	void groupByAssociation(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		final List<Long> counts = scope.fromTransaction( session -> session.createSelectionQuery(
				"select count(*) from Child c group by c.parent", Long.class ).getResultList() );
		assertThat( counts ).containsExactlyInAnyOrder( 2L, 1L, 1L );
		assertNoInnerJoin( inspector );
	}

	@Test
	void groupByCorrelatedAssociation(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		final List<Long> ids = longs( scope, "select c.id from Child c where exists "
				+ "(select count(*) from Child x group by c.parent having count(*) > 0) order by c.id" );
		assertThat( ids ).containsExactly( 10L, 11L, 12L, 13L );
		assertNoInnerJoin( inspector );
	}

	@Test
	void groupByCorrelatedAssociationWithEmptyInput(SessionFactoryScope scope) {
		assertThat( longs( scope, """
				select c.id from Child c where exists (
					select count(*) from Child x where x.id < c.id
					group by c.parent having count(*) = 0
				)
				""" ) ).isEmpty();
	}

	@Test
	void groupByIdentifierAndAssociation(SessionFactoryScope scope) {
		final List<Long> ids = scope.fromTransaction( session -> session.createSelectionQuery(
				"select c.id from Child c group by c.id, c.parent order by c.id", Long.class ).getResultList() );
		assertThat( ids ).containsExactly( 10L, 11L, 12L, 13L );
	}

	@Test
	void havingAssociation(SessionFactoryScope scope) {
		assertThat( longs( scope, "select count(*) from Child c group by c.parent having c.parent is not null" ) ).containsExactly( 2L );
	}

	@Test
	void orderByAssociation(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		final List<Long> ids = scope.fromTransaction( session -> session.createSelectionQuery(
				"select c.id from Child c order by c.parent, c.id", Long.class ).getResultList() );
		assertThat( ids ).containsExactlyInAnyOrder( 10L, 11L, 12L, 13L );
		assertNoInnerJoin( inspector );
	}

	@Test
	void orderBySelectedOwner(SessionFactoryScope scope) {
		final List<Child> children = scope.fromTransaction( session -> session.createSelectionQuery(
				"select c from Child c order by c.parent, c.id", Child.class ).getResultList() );
		assertThat( children ).extracting( child -> child.id ).containsExactlyInAnyOrder( 10L, 11L, 12L, 13L );
	}

	@Test
	void orderByDistinctSelectedAssociation(SessionFactoryScope scope) {
		final List<Parent> parents = scope.fromTransaction( session -> session.createSelectionQuery(
				"select distinct c.parent from Child c order by c.parent", Parent.class ).getResultList() );
		assertThat( parents ).extracting( parent -> parent.id ).containsExactly( 1L );
	}

	@Test
	void notFound(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var target = new Target();
			target.id = 1L;
			session.persist( target );
			dangling( session, 1L, target );
			dangling( session, 2L, null );
		} );
		assertThat( longs( scope, "select count(*) from Dangling d group by d.target" ) )
				.containsExactlyInAnyOrder( 1L, 1L );
		assertThat( longs( scope, "select d.id from Dangling d order by d.target, d.id" ) )
				.containsExactlyInAnyOrder( 1L, 2L );
	}

	@Test
	void softDelete(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var visible = new SoftParent();
			visible.id = 1L;
			session.persist( visible );
			final var deleted = new SoftParent();
			deleted.id = 2L;
			session.persist( deleted );
			softChild( session, 1L, visible );
			softChild( session, 2L, deleted );
			softChild( session, 3L, null );
		} );
		scope.inTransaction( session -> session.createMutationQuery( "delete from SoftParent where id = 2" )
				.executeUpdate() );
		assertThat( longs( scope, "select count(*) from SoftChild c group by c.parent" ) )
				.containsExactlyInAnyOrder( 1L, 1L, 1L );
		assertThat( longs( scope, "select c.id from SoftChild c order by c.parent, c.id" ) )
				.containsExactlyInAnyOrder( 1L, 2L, 3L );
	}

	private static List<Long> longs(SessionFactoryScope scope, String hql) {
		return scope.fromTransaction( session -> session.createSelectionQuery( hql, Long.class ).getResultList() );
	}

	private static void assertNoInnerJoin(SQLStatementInspector inspector) {
		assertThat( inspector.getSqlQueries() ).hasSize( 1 );
		final String sql = inspector.getSqlQueries().get( 0 ).toLowerCase();
		assertThat( sql.replace( "left join", "" ) ).doesNotContain( " join " );
	}

	private static Parent parent(Session session, Long id, boolean deleted) {
		final var parent = new Parent();
		parent.id = id;
		parent.deleted = deleted;
		session.persist( parent );
		return parent;
	}

	private static void child(Session session, Long id, String name, Parent parent) {
		final var child = new Child();
		child.id = id;
		child.name = name;
		child.parent = parent;
		session.persist( child );
	}

	private static void dangling(Session session, Long id, Target target) {
		final var dangling = new Dangling();
		dangling.id = id;
		dangling.target = target;
		session.persist( dangling );
	}

	private static void softChild(Session session, Long id, SoftParent parent) {
		final var child = new SoftChild();
		child.id = id;
		child.parent = parent;
		session.persist( child );
	}
}
