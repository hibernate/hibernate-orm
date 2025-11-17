/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.version;


import org.hibernate.Hibernate;
import org.hibernate.internal.util.MutableObject;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Max Rydahl Andersen
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(xmlMappings = "org/hibernate/orm/test/version/PersonThing.hbm.xml")
@SessionFactory
public class VersionTest {
	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testVersionShortCircuitFlush(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			Person gavin = new Person("Gavin");
			new Thing("Passport", gavin);
			session.persist(gavin);
		} );

		var passp = factoryScope.fromTransaction( (session) -> {
			Thing loaded = session.find( Thing.class, "Passport" );
			loaded.setLongDescription("blah blah blah");
			session.createQuery("from Person").list();
			session.createQuery("from Person").list();
			session.createQuery("from Person").list();
			return loaded;
		} );

		assertThat( passp.getVersion() ).isEqualTo( 1 );
	}

	@Test
	@JiraKey( value = "HHH-11549")
	public void testMetamodelContainsHbmVersion(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.getMetamodel().entity( Person.class ).getAttribute( "version" );
		} );
	}

	@Test
	public void testCollectionVersion(SessionFactoryScope factoryScope) {
		final MutableObject<Person> gavinRef = new MutableObject<>();

		factoryScope.inTransaction( (session) -> {
			Person gavin = new Person("Gavin");
			new Thing("Passport", gavin);
			session.persist(gavin);
			gavinRef.set( gavin );
		} );

		assertThat( gavinRef.get().getVersion() ).isEqualTo( 0 );

		factoryScope.inTransaction( (session) -> {
			final Person gavin = session.find( Person.class, "Gavin" );
			gavinRef.set( gavin );
			new Thing("Laptop", gavin);
		} );

		assertThat( gavinRef.get().getVersion() ).isEqualTo( 1 );
		assertFalse( Hibernate.isInitialized( gavinRef.get().getThings() ) );

		factoryScope.inTransaction( (session) -> {
			final Person gavin = session.find( Person.class, "Gavin" );
			gavinRef.set( gavin );
			gavin.getThings().clear();
		} );

		assertThat( gavinRef.get().getVersion() ).isEqualTo( 2 );
		assertTrue( Hibernate.isInitialized( gavinRef.get().getThings() ) );
	}

	@Test
	public void testCollectionNoVersion(SessionFactoryScope factoryScope) {
		final MutableObject<Person> gavinRef = new MutableObject<>();

		factoryScope.inTransaction( (session) -> {
			Person gavin = new Person("Gavin");
			new Task("Code", gavin);
			session.persist(gavin);
			gavinRef.set( gavin );
		} );

		assertThat( gavinRef.get().getVersion() ).isEqualTo( 0 );

		factoryScope.inTransaction( (session) -> {
			final Person gavin = session.find( Person.class, "Gavin" );
			gavinRef.set( gavin );
			new Task("Document", gavin);
		} );

		assertThat( gavinRef.get().getVersion() ).isEqualTo( 0 );
		assertFalse( Hibernate.isInitialized( gavinRef.get().getTasks() ) );

		factoryScope.inTransaction( (session) -> {
			final Person gavin = session.find( Person.class, "Gavin" );
			gavinRef.set( gavin );
			gavin.getTasks().clear();
		} );

		assertThat( gavinRef.get().getVersion() ).isEqualTo( 0 );
		assertTrue( Hibernate.isInitialized( gavinRef.get().getTasks() ) );
	}
}
