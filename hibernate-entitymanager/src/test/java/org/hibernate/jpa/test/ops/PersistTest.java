/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

// $Id$

package org.hibernate.jpa.test.ops;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.RollbackException;

import org.junit.Test;

import org.hibernate.cfg.Environment;
import org.hibernate.jpa.internal.EntityManagerFactoryImpl;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @author Gavin King
 * @author Hardy Ferentschik
 */
public class PersistTest extends BaseEntityManagerFunctionalTestCase {
	@Test
	public void testCreateTree() {

		clearCounts();
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Node root = new Node( "root" );
		Node child = new Node( "child" );
		root.addChild( child );
		em.persist( root );
		em.getTransaction().commit();
		em.close();

		assertInsertCount( 2 );
		assertUpdateCount( 0 );

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		root = em.find( Node.class, "root" );
		Node child2 = new Node( "child2" );
		root.addChild( child2 );
		em.getTransaction().commit();
		em.close();

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
	}

	@Test
	public void testCreateTreeWithGeneratedId() {
		clearCounts();

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		NumberedNode root = new NumberedNode( "root" );
		NumberedNode child = new NumberedNode( "child" );
		root.addChild( child );
		em.persist( root );
		em.getTransaction().commit();
		em.close();

		assertInsertCount( 2 );
		assertUpdateCount( 0 );

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		root = em.find( NumberedNode.class, root.getId() );
		NumberedNode child2 = new NumberedNode( "child2" );
		root.addChild( child2 );
		em.getTransaction().commit();
		em.close();

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
	}

	@Test
	public void testCreateException() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Node dupe = new Node( "dupe" );
		em.persist( dupe );
		em.persist( dupe );
		em.getTransaction().commit();
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( dupe );
		try {
			em.getTransaction().commit();
			fail( "Cannot persist() twice the same entity" );
		}
		catch ( Exception cve ) {
			//verify that an exception is thrown!
		}
		em.close();

		Node nondupe = new Node( "nondupe" );
		nondupe.addChild( dupe );

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( nondupe );
		try {
			em.getTransaction().commit();
			assertFalse( true );
		}
		catch ( RollbackException e ) {
			//verify that an exception is thrown!
		}
		em.close();
	}

	@Test
	public void testCreateExceptionWithGeneratedId() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		NumberedNode dupe = new NumberedNode( "dupe" );
		em.persist( dupe );
		em.persist( dupe );
		em.getTransaction().commit();
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		try {
			em.persist( dupe );
			fail();
		}
		catch ( PersistenceException poe ) {
			//verify that an exception is thrown!
		}
		em.getTransaction().rollback();
		em.close();

		NumberedNode nondupe = new NumberedNode( "nondupe" );
		nondupe.addChild( dupe );

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		try {
			em.persist( nondupe );
			fail();
		}
		catch ( PersistenceException poe ) {
			//verify that an exception is thrown!
		}
		em.getTransaction().rollback();
		em.close();
	}

	@Test
	public void testBasic() throws Exception {

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Employer er = new Employer();
		Employee ee = new Employee();
		em.persist( ee );
		Collection<Employee> erColl = new ArrayList<Employee>();
		Collection<Employer> eeColl = new ArrayList<Employer>();
		erColl.add( ee );
		eeColl.add( er );
		er.setEmployees( erColl );
		ee.setEmployers( eeColl );
		em.getTransaction().commit();
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		er = em.find( Employer.class, er.getId() );
		assertNotNull( er );
		assertNotNull( er.getEmployees() );
		assertEquals( 1, er.getEmployees().size() );
		Employee eeFromDb = ( Employee ) er.getEmployees().iterator().next();
		assertEquals( ee.getId(), eeFromDb.getId() );
		em.getTransaction().commit();
		em.close();
	}

	private void clearCounts() {
		( ( EntityManagerFactoryImpl ) entityManagerFactory() ).getSessionFactory().getStatistics().clear();
	}

	private void assertInsertCount(int count) {
		int inserts = ( int ) ( ( EntityManagerFactoryImpl ) entityManagerFactory() ).getSessionFactory()
				.getStatistics()
				.getEntityInsertCount();
		assertEquals( count, inserts );
	}

	private void assertUpdateCount(int count) {
		int updates = ( int ) ( ( EntityManagerFactoryImpl ) entityManagerFactory() ).getSessionFactory()
				.getStatistics()
				.getEntityUpdateCount();
		assertEquals( count, updates );
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	protected void addConfigOptions(Map options) {
		options.put( Environment.GENERATE_STATISTICS, "true" );
		options.put( Environment.STATEMENT_BATCH_SIZE, "0" );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Node.class };
	}

	@Override
	protected String[] getMappings() {
		return new String[] {
				"org/hibernate/jpa/test/ops/Node.hbm.xml",
				"org/hibernate/jpa/test/ops/Employer.hbm.xml"
		};
	}
}

