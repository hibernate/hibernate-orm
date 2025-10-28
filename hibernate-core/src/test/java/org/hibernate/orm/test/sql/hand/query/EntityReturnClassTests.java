/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.hand.query;

import org.hibernate.orm.test.sql.hand.Speech;
import org.hibernate.orm.test.sql.hand.SpeechInterface;
import org.hibernate.query.NativeQuery;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {SpeechInterface.class, Speech.class})
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-18864")
public class EntityReturnClassTests {

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// no return-class -> no problem

	@Test
	@JiraKey("HHH-18864")
	public void testAddEntityNoReturn(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			//noinspection unchecked,deprecation
			NativeQuery<Speech> query = session.createNativeQuery( "select {s.*} from Speech s" );
			query.addEntity("s", Speech.class);
			List<Speech> l = query.list();
			assertEquals( l.size(), 1 );
		} );
	}

	@Test
	@JiraKey("HHH-18864")
	public void testAddRootNoReturn(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			//noinspection unchecked,deprecation
			NativeQuery<Speech> query = session.createNativeQuery( "select {s.*} from Speech s" );
			query.addRoot("s", Speech.class);
			List<Speech> l = query.list();
			assertEquals( l.size(), 1 );
		} );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// entity return-class -> problems with 2 `ResultBuilder` refs

	@Test
	@JiraKey("HHH-18864")
	public void testAddEntityWithEntityReturn(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			NativeQuery<Speech> query = session.createNativeQuery( "select {s.*} from Speech s", Speech.class );
			query.addEntity("s", Speech.class);
			List<Speech> l = query.list();
			assertEquals( l.size(), 1 );
		} );
	}

	@Test
	@JiraKey("HHH-18864")
	public void testAddRootWithEntityReturn(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			NativeQuery<Speech> query = session.createNativeQuery( "select {s.*} from Speech s", Speech.class );
			query.addRoot("s", Speech.class);
			List<Speech> l = query.list();
			assertEquals( l.size(), 1 );
		} );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// entity-interface return-class -> problems with `JdbcType` determination

	@Test
	@JiraKey("HHH-18864")
	public void testAddEntityWithInterfaceReturn(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			NativeQuery<SpeechInterface> query = session.createNativeQuery( "select {s.*} from Speech s", SpeechInterface.class );
			query.addEntity("s", Speech.class);
			List<SpeechInterface> l = query.list();
			assertEquals( l.size(), 1 );
		} );
	}

	@Test
	@JiraKey("HHH-18864")
	public void testAddRootWithInterfaceReturn(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			NativeQuery<SpeechInterface> query = session.createNativeQuery( "select {s.*} from Speech s", SpeechInterface.class );
			query.addRoot("s", Speech.class);
			List<SpeechInterface> l = query.list();
			assertEquals( l.size(), 1 );
		} );
	}


	@BeforeEach
	void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			Speech speech = new Speech();
			speech.setLength( 23d );
			speech.setName( "Mine" );
			session.persist( speech );
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}
}
