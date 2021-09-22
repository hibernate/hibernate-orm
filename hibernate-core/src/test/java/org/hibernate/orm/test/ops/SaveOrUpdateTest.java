/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.ops;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Gavin King
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/ops/Node.hbm.xml",
		concurrencyStrategy = "nonstrict-read-write"
)
@SessionFactory(
		generateStatistics = true
)
@ServiceRegistry(
		settings = @Setting(name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "0")
)
public class SaveOrUpdateTest {

	@Test
	public void testSaveOrUpdateDeepTree(SessionFactoryScope scope) {
		clearCounts( scope );

		Node root = new Node( "root" );
		Node child = new Node( "child" );
		Node grandchild = new Node( "grandchild" );

		scope.inTransaction(
				session -> {
					root.addChild( child );
					child.addChild( grandchild );
					session.saveOrUpdate( root );
				}
		);

		assertInsertCount( 3, scope );
		assertUpdateCount( 0, scope );
		clearCounts( scope );

		grandchild.setDescription( "the grand child" );
		Node grandchild2 = new Node( "grandchild2" );
		child.addChild( grandchild2 );

		scope.inTransaction(
				session ->
						session.saveOrUpdate( root )
		);

		assertInsertCount( 1, scope );
		assertUpdateCount( 1, scope );
		clearCounts( scope );

		Node child2 = new Node( "child2" );
		Node grandchild3 = new Node( "grandchild3" );
		child2.addChild( grandchild3 );
		root.addChild( child2 );

		scope.inTransaction(
				session ->
						session.saveOrUpdate( root )
		);

		assertInsertCount( 2, scope );
		assertUpdateCount( 0, scope );
		clearCounts( scope );

		scope.inTransaction(
				session -> {
					session.delete( grandchild );
					session.delete( grandchild2 );
					session.delete( grandchild3 );
					session.delete( child );
					session.delete( child2 );
					session.delete( root );
				}
		);
	}

	@Test
	public void testSaveOrUpdateDeepTreeWithGeneratedId(SessionFactoryScope scope) {
		boolean instrumented = PersistentAttributeInterceptable.class.isAssignableFrom( NumberedNode.class );
		clearCounts( scope );

		NumberedNode root = new NumberedNode( "root" );
		NumberedNode c = new NumberedNode( "child" );
		NumberedNode gc = new NumberedNode( "grandchild" );
		scope.inTransaction(
				session -> {
					root.addChild( c );
					c.addChild( gc );
					session.saveOrUpdate( root );
				}
		);

		assertInsertCount( 3, scope );
		assertUpdateCount( 0, scope );
		clearCounts( scope );

		NumberedNode child = (NumberedNode) root.getChildren().iterator().next();
		NumberedNode grandchild = (NumberedNode) child.getChildren().iterator().next();
		grandchild.setDescription( "the grand child" );
		NumberedNode grandchild2 = new NumberedNode( "grandchild2" );
		child.addChild( grandchild2 );

		scope.inTransaction(
				session ->
						session.saveOrUpdate( root )
		);

		assertInsertCount( 1, scope );
		assertUpdateCount( instrumented ? 1 : 3, scope );
		clearCounts( scope );

		NumberedNode child2 = new NumberedNode( "child2" );
		NumberedNode grandchild3 = new NumberedNode( "grandchild3" );
		child2.addChild( grandchild3 );
		root.addChild( child2 );

		scope.inTransaction(
				session ->
						session.saveOrUpdate( root )
		);

		assertInsertCount( 2, scope );
		assertUpdateCount( instrumented ? 0 : 4, scope );
		clearCounts( scope );

		scope.inTransaction(
				session -> {
					session.createQuery( "delete from NumberedNode where name like 'grand%'" ).executeUpdate();
					session.createQuery( "delete from NumberedNode where name like 'child%'" ).executeUpdate();
					session.createQuery( "delete from NumberedNode" ).executeUpdate();
				}
		);
	}

	@Test
	public void testSaveOrUpdateTree(SessionFactoryScope scope) {
		clearCounts( scope );

		Node root = new Node( "root" );
		Node child = new Node( "child" );
		scope.inTransaction(
				session -> {
					root.addChild( child );
					session.saveOrUpdate( root );
				}
		);

		assertInsertCount( 2, scope );
		clearCounts( scope );

		root.setDescription( "The root node" );
		child.setDescription( "The child node" );

		Node secondChild = new Node( "second child" );

		root.addChild( secondChild );

		scope.inTransaction(
				session ->
						session.saveOrUpdate( root )
		);

		assertInsertCount( 1, scope );
		assertUpdateCount( 2, scope );

		scope.inTransaction(
				session -> {
					session.createQuery( "delete from Node where parent is not null" ).executeUpdate();
					session.createQuery( "delete from Node" ).executeUpdate();
				}
		);
	}

	@Test
	public void testSaveOrUpdateTreeWithGeneratedId(SessionFactoryScope scope) {
		clearCounts( scope );

		NumberedNode root = new NumberedNode( "root" );
		NumberedNode child = new NumberedNode( "child" );
		scope.inTransaction(
				session -> {
					root.addChild( child );
					session.saveOrUpdate( root );
				}
		);

		assertInsertCount( 2, scope );
		clearCounts( scope );

		root.setDescription( "The root node" );
		child.setDescription( "The child node" );

		NumberedNode secondChild = new NumberedNode( "second child" );

		root.addChild( secondChild );

		scope.inTransaction(
				session ->
						session.saveOrUpdate( root )
		);

		assertInsertCount( 1, scope );
		assertUpdateCount( 2, scope );

		scope.inTransaction(
				session -> {
					session.createQuery( "delete from NumberedNode where parent is not null" ).executeUpdate();
					session.createQuery( "delete from NumberedNode" ).executeUpdate();
				}
		);
	}

	@Test
	public void testSaveOrUpdateManaged(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					NumberedNode root = new NumberedNode( "root" );
					session.saveOrUpdate( root );

					session.getTransaction().commit();

					session.beginTransaction();
					NumberedNode child = new NumberedNode( "child" );
					root.addChild( child );
					session.saveOrUpdate( root );
					assertFalse( session.contains( child ) );
					session.flush();
					assertTrue( session.contains( child ) );
					session.getTransaction().commit();

					assertTrue( root.getChildren().contains( child ) );
					assertThat( root.getChildren().size(), is( 1 ) );

					session.beginTransaction();
					assertThat(
							getRowCount( session, NumberedNode.class ),
							is( 2L )
					);
					session.delete( root );
					session.delete( child );
				}
		);
	}

	@Test
	public void testSaveOrUpdateGot(SessionFactoryScope scope) {
		clearCounts( scope );

		boolean instrumented = PersistentAttributeInterceptable.class.isAssignableFrom( NumberedNode.class );

		NumberedNode root = new NumberedNode( "root" );
		scope.inTransaction(
				session ->
						session.saveOrUpdate( root )
		);

		assertInsertCount( 1, scope );
		assertUpdateCount( 0, scope );
		clearCounts( scope );

		scope.inTransaction(
				session ->
						session.saveOrUpdate( root )
		);

		assertInsertCount( 0, scope );
		assertUpdateCount( instrumented ? 0 : 1, scope );

		NumberedNode r = scope.fromTransaction(
				session -> {
					NumberedNode r1 = session.get( NumberedNode.class, root.getId() );
					Hibernate.initialize( r1.getChildren() );
					return r1;
				}
		);

		clearCounts( scope );

		scope.inTransaction(
				session -> {
					NumberedNode child = new NumberedNode( "child" );
					r.addChild( child );
					session.saveOrUpdate( r );
					assertTrue( session.contains( child ) );
					session.getTransaction().commit();

					assertInsertCount( 1, scope );
					assertUpdateCount( instrumented ? 0 : 1, scope );

					session.beginTransaction();
					assertThat(
							getRowCount( session, NumberedNode.class ),
							is( 2L )
					);
					session.delete( r );
					session.delete( child );
				}
		);
	}

	@Test
	public void testSaveOrUpdateGotWithMutableProp(SessionFactoryScope scope) {
		clearCounts( scope );

		Node root = new Node( "root" );
		scope.inTransaction(
				session ->
						session.saveOrUpdate( root )
		);

		assertInsertCount( 1, scope );
		assertUpdateCount( 0, scope );
		clearCounts( scope );

		scope.inTransaction(
				session ->
						session.saveOrUpdate( root )
		);

		assertInsertCount( 0, scope );
		assertUpdateCount( 0, scope );

		Node root1 = scope.fromTransaction(
				session -> {
					Node r1 = session.get( Node.class, "root" );
					Hibernate.initialize( r1.getChildren() );
					return r1;
				}
		);

		clearCounts( scope );

		scope.inTransaction(
				session -> {
					Node child = new Node( "child" );
					root1.addChild( child );
					session.saveOrUpdate( root1 );
					assertTrue( session.contains( child ) );
					session.getTransaction().commit();

					assertInsertCount( 1, scope );
					assertUpdateCount( 1, scope ); //note: will fail here if no second-level cache

					session.beginTransaction();
					assertThat(
							getRowCount( session, Node.class ),
							is( 2L )
					);
					session.delete( root1 );
					session.delete( child );
				}
		);
	}

	@Test
	public void testEvictThenSaveOrUpdate(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Node parent = new Node( "1:parent" );
					Node child = new Node( "2:child" );
					Node grandchild = new Node( "3:grandchild" );
					parent.addChild( child );
					child.addChild( grandchild );
					session.saveOrUpdate( parent );
				}
		);


		Session s1 = scope.getSessionFactory().openSession();
		Session s2 = null;
		try {
			s1.getTransaction().begin();
			Node child = s1.load( Node.class, "2:child" );
			assertTrue( s1.contains( child ) );
			assertFalse( Hibernate.isInitialized( child ) );
			assertTrue( s1.contains( child.getParent() ) );
			assertTrue( Hibernate.isInitialized( child ) );
			assertFalse( Hibernate.isInitialized( child.getChildren() ) );
			assertFalse( Hibernate.isInitialized( child.getParent() ) );
			assertTrue( s1.contains( child ) );
			s1.evict( child );
			assertFalse( s1.contains( child ) );
			assertTrue( s1.contains( child.getParent() ) );

			s2 = scope.getSessionFactory().openSession();
			try {
				s2.getTransaction().begin();
				s2.saveOrUpdate( child );
				fail();
			}
			catch (HibernateException ex) {
				// expected because parent is connected to s1
			}
			finally {
				s2.getTransaction().rollback();
			}
			s2.close();

			s1.evict( child.getParent() );
			assertFalse( s1.contains( child.getParent() ) );

			s2 = scope.getSessionFactory().openSession();
			s2.getTransaction().begin();
			s2.saveOrUpdate( child );
			assertTrue( s2.contains( child ) );
			assertFalse( s1.contains( child ) );
			assertTrue( s2.contains( child.getParent() ) );
			assertFalse( s1.contains( child.getParent() ) );
			assertFalse( Hibernate.isInitialized( child.getChildren() ) );
			assertFalse( Hibernate.isInitialized( child.getParent() ) );
			assertThat( child.getChildren().size(), is( 1 ) );
			assertThat( child.getParent().getName(), is( "1:parent" ) );
			assertTrue( Hibernate.isInitialized( child.getChildren() ) );
			assertFalse( Hibernate.isInitialized( child.getParent() ) );
			assertNull( child.getParent().getDescription() );
			assertTrue( Hibernate.isInitialized( child.getParent() ) );

			s1.getTransaction().commit();
			s2.getTransaction().commit();
		}
		finally {
			if ( s1.getTransaction().isActive() ) {
				s1.getTransaction().rollback();
			}
			s1.close();
			if ( s2 != null ) {
				if ( s2.getTransaction().isActive() ) {
					s1.getTransaction().rollback();
				}
				s2.close();
			}
		}

		scope.inTransaction(
				session -> {
					session.delete( session.get( Node.class, "3:grandchild" ) );
					session.delete( session.get( Node.class, "2:child" ) );
					session.delete( session.get( Node.class, "1:parent" ) );
				}
		);
	}

	@Test
	public void testSavePersistentEntityWithUpdate(SessionFactoryScope scope) {
		clearCounts( scope );

		NumberedNode root = new NumberedNode( "root" );
		root.setName( "a name" );
		scope.inTransaction(
				session ->
						session.saveOrUpdate( root )
		);

		assertInsertCount( 1, scope );
		assertUpdateCount( 0, scope );
		clearCounts( scope );

		NumberedNode r = scope.fromTransaction(
				session -> {
					NumberedNode node = session.get( NumberedNode.class, root.getId() );
					assertThat( node.getName(), is( "a name" ) );
					node.setName( "a new name" );
					session.save( node );
					return node;
				}
		);

		assertInsertCount( 0, scope );
		assertUpdateCount( 1, scope );
		clearCounts( scope );

		scope.inTransaction(
				session -> {
					NumberedNode node = session.get( NumberedNode.class, r.getId() );
					assertThat( node.getName(), is( "a new name" ) );
					session.delete( node );
				}
		);
	}

	private Long getRowCount(Session s, Class clazz) {
		CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
		CriteriaQuery<Long> criteria = criteriaBuilder.createQuery( Long.class );
		Root root = criteria.from( clazz );
		criteria.select( criteriaBuilder.count( root ) );
		return s.createQuery( criteria ).uniqueResult();
	}

	private void clearCounts(SessionFactoryScope scope) {
		scope.getSessionFactory().getStatistics().clear();
	}

	private void assertInsertCount(int count, SessionFactoryScope scope) {
		int inserts = (int) scope.getSessionFactory().getStatistics().getEntityInsertCount();
		assertThat( inserts, is( count ) );
	}

	private void assertUpdateCount(int count, SessionFactoryScope scope) {
		int updates = (int) scope.getSessionFactory().getStatistics().getEntityUpdateCount();
		assertThat( updates, is( count ) );
	}
}

