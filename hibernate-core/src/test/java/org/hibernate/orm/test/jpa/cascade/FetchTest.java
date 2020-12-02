/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.cascade;

import java.util.ArrayList;
import java.util.Date;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Emmanuel Bernard
 */
@DomainModel(annotatedClasses = {
		Troop.class,
		Soldier.class,
		Conference.class,
		ExtractionDocument.class,
		ExtractionDocumentInfo.class,
		Parent.class,
		Son.class,
		Grandson.class
})
@SessionFactory
public class FetchTest {
	@Test
	public void testCascadeAndFetchCollection(SessionFactoryScope scope) {
		Troop disney = new Troop();
		Soldier mickey = new Soldier();
		disney.setName( "Disney" );
		mickey.setName( "Mickey" );
		disney.addSoldier( mickey );

		scope.inTransaction(
				session -> session.persist( disney )
		);

		Troop troop2 = scope.fromTransaction(
				session -> {
					Troop troop = session.find( Troop.class, disney.getId() );
					assertFalse( Hibernate.isInitialized( troop.getSoldiers() ) );
					return troop;
				}
		);
		assertFalse( Hibernate.isInitialized( troop2.getSoldiers() ) );

		scope.inTransaction(
				session -> {
					Troop troop = session.find( Troop.class, disney.getId() );
					session.remove( troop );
				}
		);
	}

	@Test
	public void testCascadeAndFetchEntity(SessionFactoryScope scope) {
		Troop disney = new Troop();
		Soldier mickey = new Soldier();
		disney.setName( "Disney" );
		mickey.setName( "Mickey" );
		disney.addSoldier( mickey );

		scope.inTransaction(
				session -> session.persist( disney )
		);

		Soldier soldier2 = scope.fromTransaction(
				session -> {
					Soldier soldier = session.find( Soldier.class, mickey.getId() );
					assertFalse( Hibernate.isInitialized( soldier.getTroop() ) );
					return soldier;
				}
		);
		assertFalse( Hibernate.isInitialized( soldier2.getTroop() ) );

		scope.inTransaction(
				session -> {
					Troop troop = session.find( Troop.class, disney.getId() );
					session.remove( troop );
				}
		);
	}

	@Test
	public void testTwoLevelDeepPersist(SessionFactoryScope scope) {
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

		scope.inTransaction(
				session -> session.persist( jbwBarcelona )
		);

		scope.inSession(
				session -> {
					session.getTransaction().begin();
					Conference _jbwBarcelona = session.find( Conference.class, jbwBarcelona.getId() );
					assertTrue( Hibernate.isInitialized( _jbwBarcelona ) );
					assertTrue( Hibernate.isInitialized( _jbwBarcelona.getExtractionDocument() ) );
					assertFalse( Hibernate.isInitialized( _jbwBarcelona.getExtractionDocument().getDocuments() ) );
					session.flush();
					assertTrue( Hibernate.isInitialized( _jbwBarcelona ) );
					assertTrue( Hibernate.isInitialized( _jbwBarcelona.getExtractionDocument() ) );
					assertFalse( Hibernate.isInitialized( _jbwBarcelona.getExtractionDocument().getDocuments() ) );
					session.remove( _jbwBarcelona );
					session.getTransaction().commit();
				}
		);
	}

	@Test
	public void testTwoLevelDeepPersistOnManyToOne(SessionFactoryScope scope) {
		Grandson gs = new Grandson();
		gs.setParent( new Son() );
		gs.getParent().setParent( new Parent() );

		scope.inTransaction(
				session -> session.persist( gs )
		);

		scope.inSession(
				session -> {
					session.getTransaction().begin();
					Grandson _gs = session.find( Grandson.class, gs.getId() );
					session.flush();
					assertTrue( Hibernate.isInitialized( _gs.getParent() ) );
					assertFalse( Hibernate.isInitialized( _gs.getParent().getParent() ) );
					session.remove( _gs );
					session.getTransaction().commit();
				}
		);
	}
}
