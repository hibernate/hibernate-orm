/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.ops;

import java.util.ArrayList;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.StaleObjectStateException;
import org.hibernate.dialect.HANADialect;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.orm.junit.ExtraAssertions.assertTyping;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Gavin King
 */
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsNoColumnInsert.class)
public class MergeTest extends AbstractOperationTestCase {

	@Test
	public void testMergeStaleVersionFails(SessionFactoryScope scope) {
		VersionedEntity entity = new VersionedEntity( "entity", "entity" );
		scope.inTransaction(
				session ->
						session.persist( entity )
		);

		// make the detached 'entity' reference stale...
		scope.inTransaction(
				session -> {
					VersionedEntity entity2 = session.get( VersionedEntity.class, entity.getId() );
					entity2.setName( "entity-name" );
				}
		);

		// now try to reattach it
		scope.inSession(
				session -> {
					try {
						session.beginTransaction();
						session.merge( entity );
						session.getTransaction().commit();
						fail( "was expecting staleness error" );
					}
					catch (PersistenceException e) {
						// expected
						assertTyping( StaleObjectStateException.class, e.getCause() );
					}
					finally {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}
				}
		);
	}

	@Test
	public void testMergeBidiPrimaryKeyOneToOne(SessionFactoryScope scope) {
//		scope.getSessionFactory().close();
		Person p = new Person( "steve" );
		scope.inTransaction(
				session -> {
					new PersonalDetails( "I have big feet", p );
					session.persist( p );
				}
		);

		clearCounts( scope );

		p.getDetails().setSomePersonalDetail( p.getDetails().getSomePersonalDetail() + " and big hands too" );

		Person person = scope.fromTransaction(
				session ->
						session.merge( p )
		);

		assertInsertCount( 0, scope );
		assertUpdateCount( 1, scope );
		assertDeleteCount( 0, scope );

		scope.inTransaction(
				session ->
						session.remove( person )
		);
	}

	@Test
	public void testMergeBidiForeignKeyOneToOne(SessionFactoryScope scope) {
		Person p = new Person( "steve" );
		Address a = new Address( "123 Main", "Austin", "US", p );
		scope.inTransaction(
				session -> {
					new PersonalDetails( "I have big feet", p );
					session.persist( a );
					session.persist( p );
				}
		);

		clearCounts( scope );

		p.getAddress().setStreetAddress( "321 Main" );
		Person person = scope.fromTransaction(
				session ->
						session.merge( p )
		);

		assertInsertCount( 0, scope );
		assertUpdateCount( 0, scope ); // no cascade
		assertDeleteCount( 0, scope );

		scope.inTransaction(
				session -> {
					session.remove( a );
					session.remove( person );
				}
		);
	}

	@Test
	public void testNoExtraUpdatesOnMerge(SessionFactoryScope scope) {
		Node node = new Node( "test" );
		scope.inTransaction(
				session ->
						session.persist( node )
		);

		clearCounts( scope );

		// node is now detached, but we have made no changes.  so attempt to merge it
		// into this new session; this should cause no updates...
		Node n = (Node) scope.fromTransaction(
				session ->
						session.merge( node )
		);

		assertUpdateCount( 0, scope );
		assertInsertCount( 0, scope );

		///////////////////////////////////////////////////////////////////////
		// as a control measure, now update the node while it is detached and
		// make sure we get an update as a result...
		n.setDescription( "new description" );

		scope.inTransaction(
				session ->
						session.merge( n )
		);

		assertUpdateCount( 1, scope );
		assertInsertCount( 0, scope );
		///////////////////////////////////////////////////////////////////////

		cleanup( scope );
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testNoExtraUpdatesOnMergeWithCollection(SessionFactoryScope scope) {
		Node parent = new Node( "parent" );
		scope.inTransaction(
				session -> {
					Node child = new Node( "child" );
					parent.getChildren().add( child );
					child.setParent( parent );
					session.persist( parent );
				}
		);

		clearCounts( scope );

		// parent is now detached, but we have made no changes.  so attempt to merge it
		// into this new session; this should cause no updates...
		Node p = scope.fromTransaction(
				session ->
						session.merge( parent )
		);

		assertUpdateCount( 0, scope );
		assertInsertCount( 0, scope );

		///////////////////////////////////////////////////////////////////////
		// as a control measure, now update the node while it is detached and
		// make sure we get an update as a result...
		( (Node) p.getChildren().iterator().next() ).setDescription( "child's new description" );
		p.addChild( new Node( "second child" ) );
		scope.inTransaction(
				session ->
						session.merge( p )
		);
		assertUpdateCount( 1, scope );
		assertInsertCount( 1, scope );
		///////////////////////////////////////////////////////////////////////

		cleanup( scope );
	}

	@Test
	public void testNoExtraUpdatesOnMergeVersioned(SessionFactoryScope scope) {
		VersionedEntity entity = new VersionedEntity( "entity", "entity" );
		scope.inTransaction(
				session ->
						session.persist( entity )
		);

		clearCounts( scope );

		// entity is now detached, but we have made no changes.  so attempt to merge it
		// into this new session; this should cause no updates...
		VersionedEntity mergedEntity = scope.fromTransaction(
				session ->
						session.merge( entity )
		);

		assertUpdateCount( 0, scope );
		assertInsertCount( 0, scope );
		assertThat( "unexpected version increment", mergedEntity.getVersion(), is( entity.getVersion() ) );


		///////////////////////////////////////////////////////////////////////
		// as a control measure, now update the node while it is detached and
		// make sure we get an update as a result...
		entity.setName( "new name" );

		scope.inTransaction(
				session ->
						session.merge( entity )
		);
		assertUpdateCount( 1, scope );
		assertInsertCount( 0, scope );
		///////////////////////////////////////////////////////////////////////

		cleanup( scope );
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testNoExtraUpdatesOnMergeVersionedWithCollection(SessionFactoryScope scope) {
		VersionedEntity parent = new VersionedEntity( "parent", "parent" );
		VersionedEntity child = new VersionedEntity( "child", "child" );
		scope.inTransaction(
				session -> {
					parent.getChildren().add( child );
					child.setParent( parent );
					session.persist( parent );
				}
		);

		clearCounts( scope );

		// parent is now detached, but we have made no changes.  so attempt to merge it
		// into this new session; this should cause no updates...
		VersionedEntity mergedParent =

				scope.fromTransaction(
						session ->
								session.merge( parent )
				);

		assertUpdateCount( 0, scope );
		assertInsertCount( 0, scope );
		assertThat( "unexpected parent version increment", mergedParent.getVersion(), is( parent.getVersion() ) );
		VersionedEntity mergedChild = (VersionedEntity) mergedParent.getChildren().iterator().next();
		assertThat( "unexpected child version increment", mergedChild.getVersion(), is( child.getVersion() ) );

		///////////////////////////////////////////////////////////////////////
		// as a control measure, now update the node while it is detached and
		// make sure we get an update as a result...
		mergedParent.setName( "new name" );
		mergedParent.getChildren().add( new VersionedEntity( "child2", "new child" ) );

		scope.inTransaction(
				session ->
						session.merge( mergedParent )
		);

		assertUpdateCount( 1, scope );
		assertInsertCount( 1, scope );
		///////////////////////////////////////////////////////////////////////

		cleanup( scope );
	}

	@Test
	@SuppressWarnings({"unchecked", "unused"})
	public void testNoExtraUpdatesOnPersistentMergeVersionedWithCollection(SessionFactoryScope scope) {
		VersionedEntity parent = new VersionedEntity( "parent", "parent" );
		VersionedEntity child = new VersionedEntity( "child", "child" );
		scope.inTransaction(
				session -> {
					parent.getChildren().add( child );
					child.setParent( parent );
					session.persist( parent );
				}
		);

		clearCounts( scope );

		// parent is now detached, but we have made no changes. so attempt to merge it
		// into this new session; this should cause no updates...
		VersionedEntity mergedParent = scope.fromTransaction(
				session -> {
					// load parent so that merge will follow entityIsPersistent path
					VersionedEntity persistentParent = session.get(
							VersionedEntity.class,
							parent.getId()
					);
					// load children
					VersionedEntity persistentChild = (VersionedEntity) persistentParent.getChildren()
							.iterator()
							.next();
					return session.merge( persistentParent ); // <-- This merge leads to failure
				}
		);

		assertUpdateCount( 0, scope );
		assertInsertCount( 0, scope );
		assertThat( "unexpected parent version increment", mergedParent.getVersion(), is( parent.getVersion() ) );
		VersionedEntity mergedChild = (VersionedEntity) mergedParent.getChildren().iterator().next();
		assertThat( "unexpected child version increment", mergedChild.getVersion(), is( child.getVersion() ) );

		///////////////////////////////////////////////////////////////////////
		// as a control measure, now update the node once it is loaded and
		// make sure we get an update as a result...
		scope.inTransaction(
				session -> {
					VersionedEntity persistentParent = session.get(
							VersionedEntity.class,
							parent.getId()
					);
					persistentParent.setName( "new name" );
					persistentParent.getChildren().add( new VersionedEntity( "child2", "new child" ) );
					persistentParent = session.merge( persistentParent );

				}
		);
		assertUpdateCount( 1, scope );
		assertInsertCount( 1, scope );
		///////////////////////////////////////////////////////////////////////

		// cleanup();
	}

	@Test
	public void testPersistThenMergeInSameTxnWithVersion(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {

					VersionedEntity entity = new VersionedEntity( "test", "test" );
					session.persist( entity );
					session.merge( new VersionedEntity( "test", "test-2" ) );

					try {
						// control operation...
						session.persist( new VersionedEntity( "test", "test-3" ) );
						fail( "persist should fail here" );
					}
					catch (EntityExistsException expected) {
						// expected behavior
					}
				}
		);

		cleanup( scope );
	}

	@Test
	public void testPersistThenMergeInSameTxnWithTimestamp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TimestampedEntity entity = new TimestampedEntity( "test", "test" );
					session.persist( entity );
					session.merge( new TimestampedEntity( "test", "test-2" ) );

					try {
						// control operation...
						session.persist( new TimestampedEntity( "test", "test-3" ) );
						fail( "persist should fail here" );
					}
					catch (EntityExistsException expected) {
						// expected behavior
					}
				}
		);

		cleanup( scope );
	}

	@Test
	public void testMergeDeepTree(SessionFactoryScope scope) {
		clearCounts( scope );

		Node root = new Node( "root" );
		Node grandchild = new Node( "grandchild" );
		Node child = new Node( "child" );
		scope.inTransaction(
				session -> {
					root.addChild( child );
					child.addChild( grandchild );
					session.merge( root );
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
						session.merge( root )
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
						session.merge( root )
		);

		assertInsertCount( 2, scope );
		assertUpdateCount( 0, scope );
		clearCounts( scope );

		scope.inTransaction(
				session -> {
					session.remove( grandchild );
					session.remove( grandchild2 );
					session.remove( grandchild3 );
					session.remove( child );
					session.remove( child2 );
					session.remove( root );
				}
		);
	}

	@Test
	public void testMergeDeepTreeWithGeneratedId(SessionFactoryScope scope) {
		clearCounts( scope );

		NumberedNode root = scope.fromTransaction(
				session -> {
					NumberedNode r = new NumberedNode( "root" );
					NumberedNode child = new NumberedNode( "child" );
					NumberedNode grandchild = new NumberedNode( "grandchild" );
					r.addChild( child );
					child.addChild( grandchild );
					return session.merge( r );
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

		NumberedNode node = scope.fromTransaction(
				session ->
						session.merge( root )
		);

		assertInsertCount( 1, scope );
		assertUpdateCount( 1, scope );
		clearCounts( scope );

		scope.getSessionFactory().getCache().evictEntityData( NumberedNode.class );

		NumberedNode child2 = new NumberedNode( "child2" );
		NumberedNode grandchild3 = new NumberedNode( "grandchild3" );
		child2.addChild( grandchild3 );
		node.addChild( child2 );

		scope.inTransaction(
				session ->
						session.merge( node )
		);

		assertInsertCount( 2, scope );
		assertUpdateCount( 0, scope );
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
	public void testMergeTree(SessionFactoryScope scope) {
		clearCounts( scope );

		Node root = new Node( "root" );
		Node child = new Node( "child" );
		scope.inTransaction(
				session -> {
					root.addChild( child );
					session.persist( root );
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
						session.merge( root )
		);

		assertInsertCount( 1, scope );
		assertUpdateCount( 2, scope );

		cleanup( scope );
	}

	@Test
	public void testMergeTreeWithGeneratedId(SessionFactoryScope scope) {
		clearCounts( scope );

		NumberedNode root = new NumberedNode( "root" );
		NumberedNode child = new NumberedNode( "child" );

		scope.inTransaction(
				session -> {
					root.addChild( child );
					session.persist( root );
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
						session.merge( root )
		);

		assertInsertCount( 1, scope );
		assertUpdateCount( 2, scope );

		cleanup( scope );
	}

	@Test
	public void testMergeManaged(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					NumberedNode root = new NumberedNode( "root" );
					session.persist( root );
					session.getTransaction().commit();

					clearCounts( scope );

					session.beginTransaction();
					NumberedNode child = new NumberedNode( "child" );
					root.addChild( child );
					assertSame( root, session.merge( root ) );
					Object mergedChild = root.getChildren().iterator().next();
					assertNotSame( mergedChild, child );
					assertTrue( session.contains( mergedChild ) );
					assertFalse( session.contains( child ) );
					assertThat( root.getChildren().size(), is( 1 ) );
					assertTrue( root.getChildren().contains( mergedChild ) );
					//assertNotSame( mergedChild, s.merge(child) ); //yucky :(
					session.getTransaction().commit();

					assertInsertCount( 1, scope );
					assertUpdateCount( 1, scope );

					assertThat( root.getChildren().size(), is( 1 ) );
					assertTrue( root.getChildren().contains( mergedChild ) );

					session.beginTransaction();
					assertThat(
							getNumberedNodeRowCount( session ),
							is( 2L )
					);
				}
		);

		cleanup( scope );
	}

	private Long getNumberedNodeRowCount(Session s) {
		CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
		CriteriaQuery<Long> criteria = criteriaBuilder.createQuery( Long.class );
		Root<NumberedNode> root = criteria.from( NumberedNode.class );
		criteria.select( criteriaBuilder.count( root ) );
		return s.createQuery( criteria ).uniqueResult();
	}

	@Test
	public void testMergeManagedUninitializedCollection(SessionFactoryScope scope) {
		NumberedNode root = new NumberedNode( "root" );
		scope.inTransaction(
				session -> {
					root.addChild( new NumberedNode( "child" ) );
					session.persist( root );
				}
		);

		clearCounts( scope );

		NumberedNode newRoot = new NumberedNode( "root" );
		newRoot.setId( root.getId() );

		scope.inSession(
				session -> {
					session.getTransaction().begin();
					NumberedNode r = session.get( NumberedNode.class, root.getId() );
					var managedChildren = r.getChildren();
					assertFalse( Hibernate.isInitialized( managedChildren ) );
					newRoot.setChildren( managedChildren );
					assertSame( r, session.merge( newRoot ) );
					assertSame( managedChildren, r.getChildren() );
					assertFalse( Hibernate.isInitialized( managedChildren ) );
					session.getTransaction().commit();

					assertInsertCount( 0, scope );
					assertUpdateCount( 0, scope );
					assertDeleteCount( 0, scope );

					session.getTransaction().begin();
					assertThat(
							getNumberedNodeRowCount( session ),
							is( 2L )
					);
					session.getTransaction().commit();
				}
		);

		cleanup( scope );
	}

	@Test
	public void testMergeManagedInitializedCollection(SessionFactoryScope scope) {
		NumberedNode r = new NumberedNode( "root" );
		scope.inTransaction(
				session -> {
					r.addChild( new NumberedNode( "child" ) );
					session.persist( r );
				}
		);

		clearCounts( scope );

		NumberedNode newRoot = new NumberedNode( "root" );
		newRoot.setId( r.getId() );

		scope.inSession(
				session -> {
					session.getTransaction().begin();
					NumberedNode root = session.get( NumberedNode.class, r.getId() );
					var managedChildren = root.getChildren();
					Hibernate.initialize( managedChildren );
					assertTrue( Hibernate.isInitialized( managedChildren ) );
					newRoot.setChildren( managedChildren );
					assertSame( root, session.merge( newRoot ) );
					assertSame( managedChildren, root.getChildren() );
					assertTrue( Hibernate.isInitialized( managedChildren ) );
					session.getTransaction().commit();

					assertInsertCount( 0, scope );
					assertUpdateCount( 0, scope );
					assertDeleteCount( 0, scope );

					session.beginTransaction();
					assertThat(
							getNumberedNodeRowCount( session ),
							is( 2L )
					);
					session.getTransaction().commit();
				}
		);

		cleanup( scope );
	}

	@Test
	@SuppressWarnings("unchecked")
	@SkipForDialect(dialectClass = HANADialect.class, reason = " HANA doesn't support tables consisting of only a single auto-generated column")
	public void testRecursiveMergeTransient(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Employer jboss = new Employer();
					Employee gavin = new Employee();
					jboss.setEmployees( new ArrayList<>() );
					jboss.getEmployees().add( gavin );
					session.merge( jboss );
					session.flush();
					jboss = (Employer) session.createQuery( "from Employer e join fetch e.employees" ).uniqueResult();
					assertTrue( Hibernate.isInitialized( jboss.getEmployees() ) );
					assertThat( jboss.getEmployees().size(), is( 1 ) );
					session.clear();
					session.merge( jboss.getEmployees().iterator().next() );
				}
		);

		cleanup( scope );
	}

	@Test
	public void testDeleteAndMerge(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Employer jboss = new Employer();
					session.persist( jboss );
					session.getTransaction().commit();
					session.clear();

					session.getTransaction().begin();
					Employer otherJboss;
					otherJboss = session.get( Employer.class, jboss.getId() );
					session.remove( otherJboss );
					session.getTransaction().commit();
					session.clear();
					jboss.setVers( 1 );
					session.getTransaction().begin();
					assertThrows(
							OptimisticLockException.class,
							() -> session.merge( jboss )
					);
				}
		);

		cleanup( scope );
	}

	@SuppressWarnings("unchecked")
	@Test
	@SkipForDialect(dialectClass = HANADialect.class, reason = " HANA doesn't support tables consisting of only a single auto-generated column")
	public void testMergeManyToManyWithCollectionDeference(SessionFactoryScope scope) {
		// setup base data...
		Competition competition = new Competition();
		scope.inTransaction(
				session -> {
					competition.getCompetitors().add( new Competitor( "Name" ) );
					competition.getCompetitors().add( new Competitor() );
					competition.getCompetitors().add( new Competitor() );
					session.persist( competition );
				}
		);

		// the competition graph is now detached:
		//   1) create a new List reference to represent the competitors
		Competition competition2 = scope.fromTransaction(
				session -> {
					var newComp = new ArrayList<>();
					Competitor originalCompetitor = (Competitor) competition.getCompetitors().get( 0 );
					originalCompetitor.setName( "Name2" );
					newComp.add( originalCompetitor );
					newComp.add( new Competitor() );
					//   2) set that new List reference unto the Competition reference
					competition.setCompetitors( newComp );
					//   3) attempt the merge
					return session.merge( competition );
				}
		);

		assertNotSame( competition, competition2 );
		assertNotSame( competition.getCompetitors(), competition2.getCompetitors() );
		assertThat( competition2.getCompetitors().size(), is( 2 ) );

		scope.inTransaction(
				session -> {
					Competition c = session.get( Competition.class, competition.getId() );
					assertThat( c.getCompetitors().size(), is( 2 ) );
					session.remove( c );
				}
		);

		cleanup( scope );
	}

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}
}
