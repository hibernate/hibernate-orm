package org.hibernate.orm.test.mapping.where;

import java.util.Arrays;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.SoftDelete;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A bulk update which assigns a to-one writes its foreign key, even when the
 * target entity has a restriction, and a read of the association joins it.
 */
@DomainModel(annotatedClasses = {
		ToOneSQLRestrictionBulkUpdateTest.Node.class,
		ToOneSQLRestrictionBulkUpdateTest.Category.class,
		ToOneSQLRestrictionBulkUpdateTest.Product.class,
		ToOneSQLRestrictionBulkUpdateTest.Parent.class,
		ToOneSQLRestrictionBulkUpdateTest.Special.class,
		ToOneSQLRestrictionBulkUpdateTest.SoftNode.class
})
@SessionFactory(useCollectingStatementInspector = true)
@JiraKey("HHH-21067")
class ToOneSQLRestrictionBulkUpdateTest {

	@Entity(name = "Node")
	@Table(name = "nodes")
	@SQLRestriction("deleted = false")
	static class Node {
		@Id
		Long id;
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "parent_id")
		Node parent;
		boolean deleted;
	}

	@Entity(name = "Category")
	@Table(name = "categories")
	@SQLRestriction("deleted = false")
	static class Category {
		@Id
		@Column(name = "code")
		String code;
		boolean deleted;
	}

	@Entity(name = "Product")
	@Table(name = "products")
	static class Product {
		@Id
		Long id;
		// a column with the same name as the primary key of the target
		@Column(name = "code")
		String code;
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "category_code")
		Category category;
	}

	@Entity(name = "Parent")
	@Table(name = "parents")
	@Inheritance(strategy = InheritanceType.JOINED)
	@SQLRestriction("deleted = false")
	static class Parent {
		@Id
		Long id;
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "special_id")
		Special special;
		boolean deleted;
	}

	@Entity(name = "Special")
	@Table(name = "specials")
	static class Special extends Parent {
		String extra;
	}

	@Entity(name = "SoftNode")
	@Table(name = "soft_nodes")
	@SoftDelete
	static class SoftNode {
		@Id
		Long id;
		@ManyToOne
		@JoinColumn(name = "parent_id")
		SoftNode parent;
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void selfReference(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var root = node( session, 1L, null, false );
			node( session, 2L, root, false );
			// the parent of node 3 is not visible
			node( session, 3L, node( session, 4L, null, true ), false );
		} );
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		final int count = scope.fromTransaction( session ->
				session.createMutationQuery( "update Node n set n.parent = null where n.id in (2, 3)" ).executeUpdate() );
		assertThat( count ).isEqualTo( 2 );
		assertNoJoin( inspector, "parent_id" );
		assertThat( rows( scope, "select id, parent_id from nodes order by id" ) )
				.containsExactly( "[1, null]", "[2, null]", "[3, null]", "[4, null]" );
	}

	@Test
	void assignSubquery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			node( session, 1L, null, false );
			node( session, 2L, null, false );
		} );
		final int count = scope.fromTransaction( session -> session.createMutationQuery(
				"update Node n set n.parent = (select p from Node p where p.id = 1) where n.id = 2" ).executeUpdate() );
		assertThat( count ).isEqualTo( 1 );
		assertThat( rows( scope, "select id, parent_id from nodes order by id" ) )
				.containsExactly( "[1, null]", "[2, 1]" );
	}

	@Test
	void columnWithNameOfTargetKey(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var category = new Category();
			category.code = "C";
			session.persist( category );
			final var product = new Product();
			product.id = 1L;
			product.code = "P";
			product.category = category;
			session.persist( product );
		} );
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		final int count = scope.fromTransaction( session ->
				session.createMutationQuery( "update Product p set p.category = null" ).executeUpdate() );
		assertThat( count ).isEqualTo( 1 );
		assertNoJoin( inspector, "category_code" );
		assertThat( rows( scope, "select code, category_code from products" ) )
				.containsExactly( "[P, null]" );
	}

	@Test
	void joinedInheritance(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var special = new Special();
			special.id = 1L;
			special.extra = "x";
			session.persist( special );
			final var parent = new Parent();
			parent.id = 2L;
			parent.special = special;
			session.persist( parent );
		} );
		final int count = scope.fromTransaction( session ->
				session.createMutationQuery( "update Parent p set p.special = null" ).executeUpdate() );
		assertThat( count ).isEqualTo( 2 );
		assertThat( rows( scope, "select id, special_id from parents order by id" ) )
				.containsExactly( "[1, null]", "[2, null]" );
	}

	@Test
	void softDelete(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var root = softNode( session, 1L, null );
			softNode( session, 2L, root );
			softNode( session, 3L, softNode( session, 4L, null ) );
		} );
		// the parent of node 3 is deleted
		scope.inTransaction( session -> session.createMutationQuery( "delete from SoftNode where id = 4" ).executeUpdate() );
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		final int count = scope.fromTransaction( session ->
				session.createMutationQuery( "update SoftNode n set n.parent = null where n.id in (2, 3)" ).executeUpdate() );
		assertThat( count ).isEqualTo( 2 );
		assertNoJoin( inspector, "parent_id" );
		assertThat( rows( scope, "select id, parent_id from soft_nodes order by id" ) )
				.containsExactly( "[1, null]", "[2, null]", "[3, null]", "[4, null]" );
	}

	private static Node node(Session session, Long id, Node parent, boolean deleted) {
		final var node = new Node();
		node.id = id;
		node.parent = parent;
		node.deleted = deleted;
		session.persist( node );
		return node;
	}

	private static SoftNode softNode(Session session, Long id, SoftNode parent) {
		final var node = new SoftNode();
		node.id = id;
		node.parent = parent;
		session.persist( node );
		return node;
	}

	private static void assertNoJoin(SQLStatementInspector inspector, String foreignKeyColumn) {
		assertThat( inspector.getSqlQueries() ).hasSize( 1 );
		final String sql = inspector.getSqlQueries().get( 0 ).toLowerCase();
		assertThat( sql ).doesNotContain( " join " );
		assertThat( sql ).contains( foreignKeyColumn );
	}

	private static List<String> rows(SessionFactoryScope scope, String sql) {
		return scope.fromTransaction( session -> session.createNativeQuery( sql, Object[].class ).getResultList()
				.stream().map( Arrays::toString ).toList() );
	}
}
