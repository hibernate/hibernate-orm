/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.ops;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Gavin King
 * @author Hardy Ferentschik
 */
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsNoColumnInsert.class)
@Jpa(
		annotatedClasses = {
				Workload.class
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
public class GetLoadTest {
	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					entityManager.createQuery( "delete from Employer" ).executeUpdate();
					entityManager.createQuery( "delete from Workload" ).executeUpdate();
					entityManager.createQuery( "update Node set parent = null" ).executeUpdate();
					entityManager.createQuery( "delete from Node" ).executeUpdate();
				}
		);
	}

	@Test
	public void testGet(EntityManagerFactoryScope scope) {
		clearCounts(scope);
		String nodeName = "foo";

		Integer empId = scope.fromTransaction(
				entityManager -> {
					Session s = ( Session ) entityManager.getDelegate();

					Employer emp = new Employer();
					s.persist( emp );
					Node node = new Node( nodeName );
					Node parent = new Node( "bar" );
					parent.addChild( node );
					s.persist( parent );
					return emp.getId();
				}
		);

		scope.inTransaction(
				entityManager -> {
					Session s = ( Session ) entityManager.getDelegate();
					Employer emp = s.get( Employer.class, empId );
					assertTrue( Hibernate.isInitialized( emp ) );
					assertFalse( Hibernate.isInitialized( emp.getEmployees() ) );
					Node node = s.get( Node.class, nodeName );
					assertTrue( Hibernate.isInitialized( node ) );
					assertFalse( Hibernate.isInitialized( node.getChildren() ) );
					assertFalse( Hibernate.isInitialized( node.getParent() ) );
					assertNull( s.get( Node.class, "xyz" ) );
				}
		);

		scope.inTransaction(
				entityManager -> {
					Session s = ( Session ) entityManager.getDelegate();
					Employer emp = ( Employer ) s.get( Employer.class.getName(), empId );
					assertTrue( Hibernate.isInitialized( emp ) );
					Node node = ( Node ) s.get( Node.class.getName(), nodeName );
					assertTrue( Hibernate.isInitialized( node ) );
				}
		);

		assertFetchCount( scope, 0 );
	}

	@Test
	public void testLoad(EntityManagerFactoryScope scope) {
		clearCounts(scope);
		String nodeName = "foo";

		Integer empId = scope.fromTransaction(
				entityManager -> {
					Session s = ( Session ) entityManager.getDelegate();

					Employer emp = new Employer();
					s.persist( emp );
					Node node = new Node( nodeName );
					Node parent = new Node( "bar" );
					parent.addChild( node );
					s.persist( parent );
					return emp.getId();
				}
		);

		scope.inTransaction(
				entityManager -> {
					Session s = ( Session ) entityManager.getDelegate();
					Employer emp = s.load( Employer.class, empId );
					emp.getId();
					assertFalse( Hibernate.isInitialized( emp ) );
					Node node = s.load( Node.class, nodeName );
					assertEquals( node.getName(), nodeName );
					assertFalse( Hibernate.isInitialized( node ) );
				}
		);

		scope.inTransaction(
				entityManager -> {
					Session s = ( Session ) entityManager.getDelegate();
					Employer emp = ( Employer ) s.load( Employer.class.getName(), empId );
					emp.getId();
					assertFalse( Hibernate.isInitialized( emp ) );
					Node node = ( Node ) s.load( Node.class.getName(), nodeName );
					assertEquals( node.getName(), nodeName );
					assertFalse( Hibernate.isInitialized( node ) );
				}
		);

		assertFetchCount( scope, 0 );
	}

	private void clearCounts(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class ).getStatistics().clear();
	}

	private void assertFetchCount(EntityManagerFactoryScope scope, int count) {
		int fetches = ( int ) scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class ).getStatistics().getEntityFetchCount();
		assertEquals( count, fetches );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9856" )
	public void testNonEntity(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					entityManager.getTransaction().begin();
					try {
						entityManager.getReference( String.class, 1 );
						fail( "Expecting a failure" );
					}
					catch (IllegalArgumentException ignore) {
						// expected
					}
					finally {
						entityManager.getTransaction().rollback();
					}
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11838")
	public void testLoadGetId(EntityManagerFactoryScope scope) {
		Workload workload = scope.fromTransaction(
				entityManager -> {
					Session s = ( Session ) entityManager.getDelegate();
					Workload _workload = new Workload();
					s.persist(_workload);
					return _workload;
				}
		);

		scope.inTransaction(
				entityManager -> {
					Session s = ( Session ) entityManager.getDelegate();

					Workload proxy = s.load(Workload.class, workload.id);
					proxy.getId();

					assertFalse( Hibernate.isInitialized( proxy ) );

					proxy.getName();

					assertTrue( Hibernate.isInitialized( proxy ) );
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12034")
	public void testLoadIdNotFound_FieldBasedAccess(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					try {
						entityManager.getTransaction().begin();
						Session s = (Session) entityManager.getDelegate();

						assertNull( s.get( Workload.class, 999 ) );

						Workload proxy = s.load( Workload.class, 999 );
						assertFalse( Hibernate.isInitialized( proxy ) );

						proxy.getId();
					}
					finally {
						entityManager.getTransaction().rollback();
					}
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12034")
	public void testReferenceIdNotFound_FieldBasedAccess(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					try {
						entityManager.getTransaction().begin();

						assertNull( entityManager.find( Workload.class, 999 ) );

						Workload proxy = entityManager.getReference( Workload.class, 999 );
						assertFalse( Hibernate.isInitialized( proxy ) );

						proxy.getId();
					}
					finally {
						entityManager.getTransaction().rollback();
					}
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12034")
	public void testLoadIdNotFound_PropertyBasedAccess(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					try {
						entityManager.getTransaction().begin();
						Session s = (Session) entityManager.getDelegate();

						assertNull( s.get( Employee.class, 999 ) );

						Employee proxy = s.load( Employee.class, 999 );
						assertFalse( Hibernate.isInitialized( proxy ) );

						proxy.getId();
					}
					finally {
						entityManager.getTransaction().rollback();
					}
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12034")
	public void testReferenceIdNotFound_PropertyBasedAccess(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					try {
						entityManager.getTransaction().begin();

						assertNull( entityManager.find( Employee.class, 999 ) );

						Employee proxy = entityManager.getReference( Employee.class, 999 );
						assertFalse( Hibernate.isInitialized( proxy ) );

						proxy.getId();
					}
					finally {
						entityManager.getTransaction().rollback();
					}
				}
		);
	}
}
