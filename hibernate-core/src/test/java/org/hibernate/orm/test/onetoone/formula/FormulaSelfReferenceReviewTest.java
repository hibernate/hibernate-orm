package org.hibernate.orm.test.onetoone.formula;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinFormula;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/// Checks both directions of self-referencing formula associations.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = {
		FormulaSelfReferenceReviewTest.Node.class,
		FormulaSelfReferenceReviewTest.SimpleNode.class
})
@SessionFactory
public class FormulaSelfReferenceReviewTest {
	@BeforeEach
	void prepare(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Node root = new Node();
			root.id = 1L;
			Node child = new Node();
			child.id = 2L;
			child.parentId = 1L;
			session.persist( root );
			session.persist( child );

			SimpleNode simpleRoot = new SimpleNode();
			simpleRoot.id = 1L;
			SimpleNode simpleChild = new SimpleNode();
			simpleChild.id = 2L;
			simpleChild.parentId = 1L;
			session.persist( simpleRoot );
			session.persist( simpleChild );
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from FormulaReviewNode" ).executeUpdate();
			session.createMutationQuery( "delete from SimpleFormulaReviewNode" ).executeUpdate();
		} );
	}

	@Test
	void testInverseFetch(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Node root = session.createQuery(
					"from FormulaReviewNode n left join fetch n.child where n.id = 1", Node.class )
					.getSingleResult();
			assertNotNull( root.child );
			assertEquals( 2L, root.child.id );
			assertEquals( 1, root.child.flag );
			assertSame( root, root.child.parent );
			assertNull( root.parent );
		} );
	}

	@Test
	void testOwningFetch(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Node child = session.createQuery(
					"from FormulaReviewNode n left join fetch n.parent where n.id = 2", Node.class )
					.getSingleResult();
			assertNotNull( child.parent );
			assertEquals( 1L, child.parent.id );
			assertEquals( 1, child.parent.flag );
			assertSame( child, child.parent.child );
			assertNull( child.child );
		} );
	}

	@Test
	void testDirectLoad(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Node root = session.find( Node.class, new NodeId( 1L, 1 ) );
			assertNotNull( root.child );
			assertEquals( 2L, root.child.id );
			assertSame( root, root.child.parent );
			assertNull( root.parent );
			assertNull( root.child.child );
		} );
	}

	@Test
	void testSimpleInverseFetch(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			SimpleNode root = session.createQuery(
					"from SimpleFormulaReviewNode n left join fetch n.child where n.id = 1", SimpleNode.class )
					.getSingleResult();
			assertNotNull( root.child );
			assertEquals( 2L, root.child.id );
			assertSame( root, root.child.parent );
			assertNull( root.parent );
		} );
	}

	@Test
	void testSimpleOwningFetch(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			SimpleNode child = session.createQuery(
					"from SimpleFormulaReviewNode n left join fetch n.parent where n.id = 2", SimpleNode.class )
					.getSingleResult();
			assertNotNull( child.parent );
			assertEquals( 1L, child.parent.id );
			assertSame( child, child.parent.child );
			assertNull( child.child );
		} );
	}

	@Entity(name = "FormulaReviewNode")
	@Table(name = "formula_review_node")
	@IdClass(NodeId.class)
	static class Node {
		@Id
		Long id;
		@Id
		int flag = 1;
		@Column(name = "parent_id")
		Long parentId;
		@OneToOne
		@JoinColumnOrFormula(column = @JoinColumn(
				name = "parent_id", referencedColumnName = "id", insertable = false, updatable = false))
		@JoinColumnOrFormula(formula = @JoinFormula(value = "1", referencedColumnName = "flag"))
		Node parent;
		@OneToOne(mappedBy = "parent")
		Node child;
	}

	public record NodeId(Long id, int flag) implements Serializable {}

	@Entity(name = "SimpleFormulaReviewNode")
	@Table(name = "simple_formula_review_node")
	static class SimpleNode {
		@Id
		Long id;
		@Column(name = "parent_id")
		Long parentId;
		@OneToOne
		@JoinFormula(value = "parent_id", referencedColumnName = "id")
		SimpleNode parent;
		@OneToOne(mappedBy = "parent")
		SimpleNode child;
	}
}
