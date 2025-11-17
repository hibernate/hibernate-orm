/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.cascade;

import java.util.ArrayList;
import java.util.Date;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Emmanuel Bernard
 */
@Jpa(annotatedClasses = {
		Troop.class,
		Soldier.class,
		Conference.class,
		ExtractionDocument.class,
		ExtractionDocumentInfo.class,
		Parent.class,
		Son.class,
		Grandson.class
})
public class FetchTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testCascadeAndFetchCollection(EntityManagerFactoryScope scope) {
		Troop disney = new Troop();
		Soldier mickey = new Soldier();
		disney.setName( "Disney" );
		mickey.setName( "Mickey" );
		disney.addSoldier( mickey );

		scope.inTransaction(
				entityManager -> entityManager.persist( disney )
		);

		Troop troop2 = scope.fromTransaction(
				entityManager -> {
					Troop troop = entityManager.find( Troop.class, disney.getId() );
					assertFalse( Hibernate.isInitialized( troop.getSoldiers() ) );
					return troop;
				}
		);
		assertFalse( Hibernate.isInitialized( troop2.getSoldiers() ) );
	}

	@Test
	public void testCascadeAndFetchEntity(EntityManagerFactoryScope scope) {
		Troop disney = new Troop();
		Soldier mickey = new Soldier();
		disney.setName( "Disney" );
		mickey.setName( "Mickey" );
		disney.addSoldier( mickey );

		scope.inTransaction(
				entityManager -> entityManager.persist( disney )
		);

		Soldier soldier2 = scope.fromTransaction(
				entityManager -> {
					Soldier soldier = entityManager.find( Soldier.class, mickey.getId() );
					assertFalse( Hibernate.isInitialized( soldier.getTroop() ) );
					return soldier;
				}
		);
		assertFalse( Hibernate.isInitialized( soldier2.getTroop() ) );
	}

	@Test
	public void testTwoLevelDeepPersist(EntityManagerFactoryScope scope) {
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
				entityManager -> entityManager.persist( jbwBarcelona )
		);

		scope.inTransaction(
				entityManager -> {
					Conference _jbwBarcelona = entityManager.find( Conference.class, jbwBarcelona.getId() );
					assertTrue( Hibernate.isInitialized( _jbwBarcelona ) );
					assertTrue( Hibernate.isInitialized( _jbwBarcelona.getExtractionDocument() ) );
					assertFalse( Hibernate.isInitialized( _jbwBarcelona.getExtractionDocument().getDocuments() ) );
					entityManager.flush();
					assertTrue( Hibernate.isInitialized( _jbwBarcelona ) );
					assertTrue( Hibernate.isInitialized( _jbwBarcelona.getExtractionDocument() ) );
					assertFalse( Hibernate.isInitialized( _jbwBarcelona.getExtractionDocument().getDocuments() ) );
				}
		);
	}

	@Test
	public void testTwoLevelDeepPersistOnManyToOne(EntityManagerFactoryScope scope) {
		Grandson gs = new Grandson();
		gs.setParent( new Son() );
		gs.getParent().setParent( new Parent() );

		scope.inTransaction(
				entityManager -> entityManager.persist( gs )
		);

		scope.inTransaction(
				entityManager -> {
					Grandson _gs = entityManager.find( Grandson.class, gs.getId() );
					entityManager.flush();
					assertTrue( Hibernate.isInitialized( _gs.getParent() ) );
					assertFalse( Hibernate.isInitialized( _gs.getParent().getParent() ) );
				}
		);
	}
}
