//$Id$
package org.hibernate.ejb.test.cascade;

import java.util.ArrayList;
import java.util.Date;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.hibernate.Hibernate;
import org.hibernate.ejb.HibernateEntityManager;
import org.hibernate.ejb.test.TestCase;

/**
 * @author Emmanuel Bernard
 */
public class FetchTest extends TestCase {

	public void testCascadeAndFetchCollection() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Troop disney = new Troop();
		disney.setName( "Disney" );
		Soldier mickey = new Soldier();
		mickey.setName( "Mickey" );
		disney.addSoldier( mickey );
		em.persist( disney );
		em.getTransaction().commit();
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Troop troop = em.find( Troop.class, disney.getId() );
		assertFalse( Hibernate.isInitialized( troop.getSoldiers() ) );
		em.getTransaction().commit();
		assertFalse( Hibernate.isInitialized( troop.getSoldiers() ) );
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		troop = em.find( Troop.class, disney.getId() );
		em.remove( troop );
		//Fail because of HHH-1187
		em.getTransaction().commit();
		em.close();
	}

	public void testCascadeAndFetchEntity() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Troop disney = new Troop();
		disney.setName( "Disney" );
		Soldier mickey = new Soldier();
		mickey.setName( "Mickey" );
		disney.addSoldier( mickey );
		em.persist( disney );
		em.getTransaction().commit();
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Soldier soldier = em.find( Soldier.class, mickey.getId() );
		assertFalse( Hibernate.isInitialized( soldier.getTroop() ) );
		em.getTransaction().commit();
		assertFalse( Hibernate.isInitialized( soldier.getTroop() ) );
		em.close();
		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Troop troop = em.find( Troop.class, disney.getId() );
		em.remove( troop );
		//Fail because of HHH-1187
		em.getTransaction().commit();
		em.close();
	}

	public void testTwoLevelDeepPersist() throws Exception {
		EntityTransaction tx;

		EntityManager em = getOrCreateEntityManager();
		tx = em.getTransaction();
		tx.begin();
		Conference jbwBarcelona = new Conference();
		jbwBarcelona.setDate( new Date() );
		ExtractionDocumentInfo info = new ExtractionDocumentInfo();
		info.setConference( jbwBarcelona );
		jbwBarcelona.setExtractionDocument( info );
		info.setLastModified( new Date() );
		ExtractionDocument doc = new ExtractionDocument();
		doc.setDocumentInfo( info );
		info.setDocuments( new ArrayList<ExtractionDocument>() );
		info.getDocuments().add( doc );
		doc.setBody( new byte[]{'c', 'f'} );
		em.persist( jbwBarcelona );
		tx.commit();
		em.close();

		em = getOrCreateEntityManager();
		tx = em.getTransaction();
		tx.begin();
		jbwBarcelona = em.find( Conference.class, jbwBarcelona.getId() );
		assertTrue( Hibernate.isInitialized( jbwBarcelona ) );
		assertTrue( Hibernate.isInitialized( jbwBarcelona.getExtractionDocument() ) );
		assertFalse( Hibernate.isInitialized( jbwBarcelona.getExtractionDocument().getDocuments() ) );
		em.flush();
		assertTrue( Hibernate.isInitialized( jbwBarcelona ) );
		assertTrue( Hibernate.isInitialized( jbwBarcelona.getExtractionDocument() ) );
		assertFalse( Hibernate.isInitialized( jbwBarcelona.getExtractionDocument().getDocuments() ) );
		em.remove( jbwBarcelona );
		tx.commit();
		em.close();
	}

	public void testTwoLevelDeepPersistOnManyToOne() throws Exception {
		EntityTransaction tx;
		EntityManager em = getOrCreateEntityManager();
		tx = em.getTransaction();
		tx.begin();
		Grandson gs = new Grandson();
		gs.setParent( new Son() );
		gs.getParent().setParent( new Parent() );
		em.persist( gs );
		tx.commit();
		em.close();
		em = getOrCreateEntityManager();
		tx = em.getTransaction();
		tx.begin();
		gs = em.find( Grandson.class, gs.getId() );
		em.flush();
		assertTrue( Hibernate.isInitialized( gs.getParent() ) );
		assertFalse( Hibernate.isInitialized( gs.getParent().getParent() ) );
		em.remove( gs );
		tx.commit();
		em.close();
	}

	public void testPerfCascadeAndFetchEntity() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		//init data
		em.getTransaction().begin();
		int loop = 50;
		for ( int i = 0; i < loop ; i++ ) {
			Troop disney = new Troop();
			disney.setName( "Disney" );
			Soldier mickey = new Soldier();
			mickey.setName( "Mickey" );
			disney.addSoldier( mickey );
			em.persist( disney );
		}
		em.getTransaction().commit();
		em.close();

		//Warm up loop
		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		for ( int i = 0; i < loop ; i++ ) {
			//Soldier soldier = em.find( Soldier.class, new Integer(i) );
			Troop troop = em.find( Troop.class, new Integer( i ) );
			//( ( HibernateEntityManager ) em ).getSession().evict(soldier);
		}
		long l11 = System.currentTimeMillis();
		Query query = em.createQuery( "SELECT count(t) FROM Soldier t" );
		query.getSingleResult();
		long l2 = System.currentTimeMillis();
		System.out.println( "Query1 " + ( l2 - l11 ) );
		em.getTransaction().commit();
		em.close();

		//do not evict
		for ( int j = 0; j < 10 ; j++ ) {
			em = getOrCreateEntityManager();
			em.getTransaction().begin();
			for ( int i = 0; i < loop ; i++ ) {
				Troop troop = em.find( Troop.class, new Integer( i ) );
				( (HibernateEntityManager) em ).getSession().evict( troop );
			}
			l11 = System.currentTimeMillis();
			query = em.createQuery( "SELECT count(t) FROM Soldier t" );
			query.getSingleResult();
			l2 = System.currentTimeMillis();
			System.out.println( "Query " + ( l2 - l11 ) );
			em.getTransaction().commit();
			em.close();

			//evict
			em = getOrCreateEntityManager();
			em.getTransaction().begin();
			for ( int i = 0; i < loop ; i++ ) {
				//Soldier soldier = em.find( Soldier.class, new Integer(i) );
				Troop troop = em.find( Troop.class, new Integer( i ) );

				//( ( HibernateEntityManager ) em ).getSession().evict(troop);
			}
			l11 = System.currentTimeMillis();
			query = em.createQuery( "SELECT count(t) FROM Soldier t" );
			query.getSingleResult();
			l2 = System.currentTimeMillis();
			System.out.println( "Query " + ( l2 - l11 ) );
			em.getTransaction().commit();
		}
		em.close();
	}


	public Class[] getAnnotatedClasses() {
		return new Class[]{
				Troop.class,
				Soldier.class,
				Conference.class,
				ExtractionDocument.class,
				ExtractionDocumentInfo.class,
				Parent.class,
				Son.class,
				Grandson.class
		};
	}
}
