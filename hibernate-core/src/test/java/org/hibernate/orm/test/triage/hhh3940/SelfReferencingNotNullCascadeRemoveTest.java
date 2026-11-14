package org.hibernate.orm.test.triage.hhh3940;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.PropertyValueException;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for self-referencing entities with {@code @OnDelete(CASCADE)}.
 * <p>
 * On databases requiring self-referential FK nullification (MySQL, MariaDB),
 * deletion should skip nullification when {@code @OnDelete(CASCADE)} is present.
 *
 * @see <a href="https://hibernate.atlassian.net/browse/HHH-21133">HHH-21133</a>
 */
@DomainModel(
		annotatedClasses = {
				SelfReferencingNotNullCascadeRemoveTest.SelfRefWithCascade.class,
				SelfReferencingNotNullCascadeRemoveTest.SelfRefWithoutCascade.class,
				SelfReferencingNotNullCascadeRemoveTest.TreeNode.class
		}
)
@SessionFactory(useCollectingStatementInspector = true)
@RequiresDialectFeature(
		feature = DialectFeatureChecks.SupportsCascadeDeleteCheck.class,
		comment = "Requires ON DELETE CASCADE support"
)
public class SelfReferencingNotNullCascadeRemoveTest {

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testSelfReferenceWithCascade(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			SelfRefWithCascade root = new SelfRefWithCascade();
			root.id = 1;
			root.parent = root;
			session.persist( root );

			SelfRefWithCascade child = new SelfRefWithCascade();
			child.id = 2;
			child.parent = root;
			session.persist( child );
		} );

		var inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		scope.inTransaction( session -> {
			SelfRefWithCascade root = session.find( SelfRefWithCascade.class, 1 );
			session.remove( root );
		} );

		if ( scope.getSessionFactory().getJdbcServices().getDialect().getForeignKeySupport()
				.supportsOnDeleteAction( OnDeleteAction.CASCADE ) ) {
			long deleteCount = inspector.getSqlQueries().stream()
					.filter( sql -> sql.trim().toLowerCase().startsWith( "delete" ) )
					.count();
			assertEquals( 1L, deleteCount );
		}

		scope.inTransaction( session -> {
			long count = session.createSelectionQuery(
					"select count(*) from SelfRefWithCascade",
					Long.class
			).getSingleResult();
			assertEquals( 0L, count );
		} );
	}

	@Test
	public void testSelfReferenceWithoutCascadeStillFails(SessionFactoryScope scope) {
		if ( !scope.getSessionFactory().getJdbcServices().getDialect()
				.getForeignKeySupport().requiresSelfReferentialForeignKeyNullification() ) {
			return;
		}

		scope.inTransaction( session -> {
			SelfRefWithoutCascade entity = new SelfRefWithoutCascade();
			entity.id = 1;
			entity.parent = entity;
			session.persist( entity );
		} );

		assertThrows( PropertyValueException.class, () -> {
			scope.inTransaction( session -> {
				SelfRefWithoutCascade entity = session.find( SelfRefWithoutCascade.class, 1 );
				session.remove( entity );
			} );
		} );
	}

	@Test
	public void testMultipleSelfReferencingProperties(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			TreeNode node = new TreeNode();
			node.id = 1;
			node.left = node;
			node.right = node;
			session.persist( node );

			TreeNode child = new TreeNode();
			child.id = 2;
			child.left = node;
			child.right = node;
			session.persist( child );
		} );

		scope.inTransaction( session -> {
			TreeNode node = session.find( TreeNode.class, 1 );
			session.remove( node );
		} );

		scope.inTransaction( session -> {
			long count = session.createSelectionQuery(
					"select count(*) from TreeNode",
					Long.class
			).getSingleResult();
			assertEquals( 0L, count );
		} );
	}

	@Test
	public void testNativeDeleteWithCascade(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			SelfRefWithCascade root = new SelfRefWithCascade();
			root.id = 1;
			root.parent = root;
			session.persist( root );

			SelfRefWithCascade child = new SelfRefWithCascade();
			child.id = 2;
			child.parent = root;
			session.persist( child );
		} );

		scope.inTransaction( session -> {
			session.createNativeMutationQuery( "delete from self_ref_cascade where id = 1" )
					.executeUpdate();
		} );

		scope.inTransaction( session -> {
			long count = session.createSelectionQuery(
					"select count(*) from SelfRefWithCascade",
					Long.class
			).getSingleResult();
			assertEquals( 0L, count );
		} );
	}

	@Entity(name = "SelfRefWithCascade")
	@Table(name = "self_ref_cascade")
	public static class SelfRefWithCascade {
		@Id
		Integer id;

		@ManyToOne(optional = false)
		@OnDelete(action = OnDeleteAction.CASCADE)
		SelfRefWithCascade parent;
	}

	@Entity(name = "SelfRefWithoutCascade")
	@Table(name = "self_ref_no_cascade")
	public static class SelfRefWithoutCascade {
		@Id
		Integer id;

		@ManyToOne(optional = false)
		SelfRefWithoutCascade parent;
	}

	@Entity(name = "TreeNode")
	@Table(name = "tree_node")
	public static class TreeNode {
		@Id
		Integer id;

		@ManyToOne(optional = false)
		@OnDelete(action = OnDeleteAction.CASCADE)
		TreeNode left;

		@ManyToOne(optional = false)
		@OnDelete(action = OnDeleteAction.CASCADE)
		TreeNode right;
	}
}
