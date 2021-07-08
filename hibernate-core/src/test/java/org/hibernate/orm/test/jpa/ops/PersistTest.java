/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

// $Id$

package org.hibernate.orm.test.jpa.ops;

import java.util.ArrayList;
import java.util.Collection;
import javax.persistence.PersistenceException;
import javax.persistence.RollbackException;

import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Gavin King
 * @author Hardy Ferentschik
 */
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsNoColumnInsert.class)
@Jpa(
		annotatedClasses = {
				Node.class
		},
		integrationSettings = {
				@Setting(name = Environment.GENERATE_STATISTICS, value = "true"),
				@Setting(name = Environment.STATEMENT_BATCH_SIZE, value = "0")
		},
		xmlMappings = {
				"org/hibernate/orm/test/jpa/ops/Node.hbm.xml",
				"org/hibernate/orm/test/jpa/ops/Employer.hbm.xml"
		}
)
public class PersistTest {
	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					entityManager.createQuery( "update Node set parent = null" ).executeUpdate();
					entityManager.createQuery( "delete from Node" ).executeUpdate();
					entityManager.createQuery( "update NumberedNode set parent = null" ).executeUpdate();
					entityManager.createQuery( "delete from NumberedNode" ).executeUpdate();
				}
		);
	}

	@Test
	public void testCreateTree(EntityManagerFactoryScope scope) {
		clearCounts( scope );

		scope.inTransaction(
				entityManager -> {
					Node root = new Node( "root" );
					Node child = new Node( "child" );
					root.addChild( child );
					entityManager.persist( root );
				}
		);

		assertInsertCount( scope, 2 );
		assertUpdateCount( scope, 0 );

		scope.inTransaction(
				entityManager -> {
					Node _root = entityManager.find( Node.class, "root" );
					Node child2 = new Node( "child2" );
					_root.addChild( child2 );
				}
		);

		assertInsertCount( scope, 3 );
		assertUpdateCount( scope, 0 );
	}

	@Test
	public void testCreateTreeWithGeneratedId(EntityManagerFactoryScope scope) {
		clearCounts( scope );

		Long rootId = scope.fromTransaction(
				entityManager -> {
					NumberedNode root = new NumberedNode( "root" );
					NumberedNode child = new NumberedNode( "child" );
					root.addChild( child );
					entityManager.persist( root );
					return root.getId();
				}
		);

		assertInsertCount( scope, 2 );
		assertUpdateCount( scope, 0 );

		scope.inTransaction(
				entityManager -> {
					NumberedNode _root = entityManager.find( NumberedNode.class, rootId );
					NumberedNode child2 = new NumberedNode( "child2" );
					_root.addChild( child2 );
				}
		);

		assertInsertCount( scope, 3 );
		assertUpdateCount( scope, 0 );
	}

	@Test
	public void testCreateException(EntityManagerFactoryScope scope) {
		Node dupe = scope.fromTransaction(
				entityManager -> {
					Node _dupe = new Node( "dupe" );
					entityManager.persist( _dupe );
					entityManager.persist( _dupe );
					return _dupe;
				}
		);

		scope.inEntityManager(
				entityManager -> {
					entityManager.getTransaction().begin();
					entityManager.persist( dupe );
					try {
						entityManager.getTransaction().commit();
						fail( "Cannot persist() twice the same entity" );
					}
					catch (Exception cve) {
						//verify that an exception is thrown!
					}
					finally {
						if ( entityManager.getTransaction().isActive() ) {
							entityManager.getTransaction().rollback();
						}
					}
				}
		);

		Node nondupe = new Node( "nondupe" );
		nondupe.addChild( dupe );

		scope.inEntityManager(
				entityManager -> {
					entityManager.getTransaction().begin();
					entityManager.persist( nondupe );
					try {
						entityManager.getTransaction().commit();
						fail();
					}
					catch (RollbackException e) {
						//verify that an exception is thrown!
					}
					finally {
						if ( entityManager.getTransaction().isActive() ) {
							entityManager.getTransaction().rollback();
						}
					}
				}
		);
	}

	@Test
	public void testCreateExceptionWithGeneratedId(EntityManagerFactoryScope scope) {
		NumberedNode dupe = scope.fromTransaction(
				entityManager -> {
					NumberedNode _dupe = new NumberedNode( "dupe" );
					entityManager.persist( _dupe );
					entityManager.persist( _dupe );
					return _dupe;
				}
		);

		scope.inEntityManager(
				entityManager -> {
					entityManager.getTransaction().begin();
					try {
						entityManager.persist( dupe );
						fail();
					}
					catch (PersistenceException poe) {
						//verify that an exception is thrown!
					}
					finally {
						if ( entityManager.getTransaction().isActive() ) {
							entityManager.getTransaction().rollback();
						}
					}
				}
		);

		NumberedNode nondupe = new NumberedNode( "nondupe" );
		nondupe.addChild( dupe );

		scope.inEntityManager(
				entityManager -> {
					entityManager.getTransaction().begin();
					try {
						entityManager.persist( nondupe );
						fail();
					}
					catch (PersistenceException poe) {
						//verify that an exception is thrown!
					}
					finally {
						if ( entityManager.getTransaction().isActive() ) {
							entityManager.getTransaction().rollback();
						}
					}
				}
		);
	}

	@Test
	public void testBasic(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					try {
						entityManager.getTransaction().begin();
						Employer er = new Employer();
						Employee ee = new Employee();
						entityManager.persist( ee );
						Collection<Employee> erColl = new ArrayList<>();
						Collection<Employer> eeColl = new ArrayList<>();
						erColl.add( ee );
						eeColl.add( er );
						er.setEmployees( erColl );
						ee.setEmployers( eeColl );
						entityManager.getTransaction().commit();
						entityManager.close();

						entityManager = scope.getEntityManagerFactory().createEntityManager();
						entityManager.getTransaction().begin();
						er = entityManager.find( Employer.class, er.getId() );
						assertNotNull( er );
						assertNotNull( er.getEmployees() );
						assertEquals( 1, er.getEmployees().size() );
						Employee eeFromDb = ( Employee ) er.getEmployees().iterator().next();
						assertEquals( ee.getId(), eeFromDb.getId() );
						entityManager.getTransaction().commit();
					}
					catch (Exception e) {
						if ( entityManager.getTransaction().isActive() ) {
							entityManager.getTransaction().rollback();
						}
						throw e;
					}
				}
		);
	}

	private void clearCounts(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class ).getStatistics().clear();
	}

	private void assertInsertCount(EntityManagerFactoryScope scope, int count) {
		int inserts = (int) scope.getEntityManagerFactory()
				.unwrap( SessionFactoryImplementor.class )
				.getStatistics()
				.getEntityInsertCount();
		assertEquals( count, inserts );
	}

	private void assertUpdateCount(EntityManagerFactoryScope scope, int count) {
		int updates = (int) scope.getEntityManagerFactory()
				.unwrap( SessionFactoryImplementor.class )
				.getStatistics()
				.getEntityUpdateCount();
		assertEquals( count, updates );
	}
}
