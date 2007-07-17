package org.hibernate.test.generatedkeys.identity;

import junit.framework.Test;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.junit.functional.DatabaseSpecificFunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;

/**
 * @author Steve Ebersole
 */
public class IdentityGeneratedKeysTest extends DatabaseSpecificFunctionalTestCase {
	public IdentityGeneratedKeysTest(String name) {
		super( name );
	}

	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
	}

	public String[] getMappings() {
		return new String[] { "generatedkeys/identity/MyEntity.hbm.xml" };
	}

	public boolean appliesTo(Dialect dialect) {
		return dialect.supportsIdentityColumns();
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( IdentityGeneratedKeysTest.class );
	}

	public void testIdentityColumnGeneratedIds() {
		Session s = openSession();
		s.beginTransaction();
		MyEntity myEntity = new MyEntity( "test" );
		Long id = ( Long ) s.save( myEntity );
		assertNotNull( "identity column did not force immediate insert", id );
		assertEquals( id, myEntity.getId() );
		s.delete( myEntity );
		s.getTransaction().commit();
		s.close();
	}

	public void testPersistOutsideTransaction() {
		Session s = openSession();

		// first test save() which should force an immediate insert...
		MyEntity myEntity1 = new MyEntity( "test-save" );
		Long id = ( Long ) s.save( myEntity1 );
		assertNotNull( "identity column did not force immediate insert", id );
		assertEquals( id, myEntity1.getId() );

		// next test persist() which should cause a delayed insert...
		long initialInsertCount = sfi().getStatistics().getEntityInsertCount();
		MyEntity myEntity2 = new MyEntity( "test-persist");
		s.persist( myEntity2 );
		assertEquals( "persist on identity column not delayed", initialInsertCount, sfi().getStatistics().getEntityInsertCount() );
		assertNull( myEntity2.getId() );

		// an explicit flush should cause execution of the delayed insertion
		s.flush();
		assertEquals( "delayed persist insert not executed on flush", initialInsertCount + 1, sfi().getStatistics().getEntityInsertCount() );
		s.close();

		s = openSession();
		s.beginTransaction();
		s.delete( myEntity1 );
		s.delete( myEntity2 );
		s.getTransaction().commit();
		s.close();
	}

	public void testPersistOutsideTransactionCascadedToNonInverseCollection() {
		long initialInsertCount = sfi().getStatistics().getEntityInsertCount();
		Session s = openSession();
		MyEntity myEntity = new MyEntity( "test-persist");
		myEntity.getNonInverseChildren().add( new MyChild( "test-child-persist-non-inverse" ) );
		s.persist( myEntity );
		assertEquals( "persist on identity column not delayed", initialInsertCount, sfi().getStatistics().getEntityInsertCount() );
		assertNull( myEntity.getId() );
		s.flush();
		assertEquals( "delayed persist insert not executed on flush", initialInsertCount + 2, sfi().getStatistics().getEntityInsertCount() );
		s.close();

		s = openSession();
		s.beginTransaction();
		s.createQuery( "delete MyChild" ).executeUpdate();
		s.createQuery( "delete MyEntity" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	public void testPersistOutsideTransactionCascadedToInverseCollection() {
		long initialInsertCount = sfi().getStatistics().getEntityInsertCount();
		Session s = openSession();
		MyEntity myEntity2 = new MyEntity( "test-persist-2");
		MyChild child = new MyChild( "test-child-persist-inverse" );
		myEntity2.getInverseChildren().add( child );
		child.setInverseParent( myEntity2 );
		s.persist( myEntity2 );
		assertEquals( "persist on identity column not delayed", initialInsertCount, sfi().getStatistics().getEntityInsertCount() );
		assertNull( myEntity2.getId() );
		s.flush();
		assertEquals( "delayed persist insert not executed on flush", initialInsertCount + 2, sfi().getStatistics().getEntityInsertCount() );
		s.close();

		s = openSession();
		s.beginTransaction();
		s.createQuery( "delete MyChild" ).executeUpdate();
		s.createQuery( "delete MyEntity" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	public void testPersistOutsideTransactionCascadedToManyToOne() {
		long initialInsertCount = sfi().getStatistics().getEntityInsertCount();
		Session s = openSession();
		MyEntity myEntity = new MyEntity( "test-persist");
		myEntity.setSibling( new MySibling( "test-persist-sibling-out" ) );
		s.persist( myEntity );
		assertEquals( "persist on identity column not delayed", initialInsertCount, sfi().getStatistics().getEntityInsertCount() );
		assertNull( myEntity.getId() );
		s.flush();
		assertEquals( "delayed persist insert not executed on flush", initialInsertCount + 2, sfi().getStatistics().getEntityInsertCount() );
		s.close();

		s = openSession();
		s.beginTransaction();
		s.createQuery( "delete MyEntity" ).executeUpdate();
		s.createQuery( "delete MySibling" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	public void testPersistOutsideTransactionCascadedFromManyToOne() {
		long initialInsertCount = sfi().getStatistics().getEntityInsertCount();
		Session s = openSession();
		MyEntity myEntity2 = new MyEntity( "test-persist-2");
		MySibling sibling = new MySibling( "test-persist-sibling-in" );
		sibling.setEntity( myEntity2 );
		s.persist( sibling );
		assertEquals( "persist on identity column not delayed", initialInsertCount, sfi().getStatistics().getEntityInsertCount() );
		assertNull( myEntity2.getId() );
		s.flush();
		assertEquals( "delayed persist insert not executed on flush", initialInsertCount + 2, sfi().getStatistics().getEntityInsertCount() );
		s.close();

		s = openSession();
		s.beginTransaction();
		s.createQuery( "delete MySibling" ).executeUpdate();
		s.createQuery( "delete MyEntity" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}
}
