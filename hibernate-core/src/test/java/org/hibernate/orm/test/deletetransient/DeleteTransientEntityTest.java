/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.deletetransient;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry
@DomainModel(xmlMappings = "org/hibernate/orm/test/deletetransient/Person.xml")
@SessionFactory
public class DeleteTransientEntityTest {
	@AfterEach
	public void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testTransientEntityDeletionNoCascades(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (s) -> {
			s.remove( new Address() );
		} );
	}

	@Test
	public void testTransientEntityDeletionCascadingToTransientAssociation(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (s) -> {
			Person p = new Person();
			p.getAddresses().add( new Address() );
			s.remove( p );
		} );
	}

	@Test
	public void testTransientEntityDeleteCascadingToCircularity(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (s) -> {
			Person p1 = new Person();
			Person p2 = new Person();
			p1.getFriends().add( p2 );
			p2.getFriends().add( p1 );
			s.remove( p1 );
		} );
	}

	@Test
	public void testTransientEntityDeletionCascadingToDetachedAssociation(SessionFactoryScope factoryScope) {
		Address detachedAddress = factoryScope.fromTransaction( (s) -> {
			Address address = new Address();
			address.setInfo( "123 Main St." );
			s.persist( address );
			return address;
		} );

		factoryScope.inTransaction( (s) -> {
			Person p = new Person();
			p.getAddresses().add( detachedAddress );
			s.remove( p );
		} );

		factoryScope.inTransaction( (s) -> {
			Long count = ( Long ) s.createQuery( "select count(*) from Address" ).list().get( 0 );
			assertEquals( 0, count.longValue(), "delete not cascaded properly across transient entity" );
		} );
	}

	@Test
	public void testTransientEntityDeletionCascadingToPersistentAssociation(SessionFactoryScope factoryScope) {
		Address detachedAddress = factoryScope.fromTransaction( (s) -> {
			Address address = new Address();
			address.setInfo( "123 Main St." );
			s.persist( address );
			return address;
		} );

		factoryScope.inTransaction( (s) -> {
			Address address = s.find( Address.class, detachedAddress.getId() );
			Person p = new Person();
			p.getAddresses().add( address );
			s.remove( p );
		} );

		factoryScope.inTransaction( (s) -> {
			Long count = ( Long ) s.createQuery( "select count(*) from Address" ).list().get( 0 );
			assertEquals( 0, count.longValue(), "delete not cascaded properly across transient entity" );
		} );
	}

	@Test
	public void testCascadeAllFromClearedPersistentAssnToTransientEntity(SessionFactoryScope factoryScope) {
		final Person detachedPerson = factoryScope.fromTransaction( (s) -> {
			Person person = new Person();
			Address address = new Address();
			address.setInfo( "123 Main St." );
			person.getAddresses().add( address );
			s.persist( person );
			return person;
		} );

		final Person detachedPerson2 = factoryScope.fromTransaction( (s) -> {
			Suite suite = new Suite();
			assertThat( detachedPerson.getAddresses() ).hasSize( 1 );
			Address address = detachedPerson.getAddresses().iterator().next();;
			address.getSuites().add( suite );
			detachedPerson.getAddresses().clear();
			return s.merge( detachedPerson );
		} );

		factoryScope.inTransaction( (s) -> {
			Person p = s.find( Person.class, detachedPerson2.getId() );
			assertEquals( 0, p.getAddresses().size(), "persistent collection not cleared" );
			Long count = ( Long ) s.createQuery( "select count(*) from Address" ).list().get( 0 );
			assertEquals( 1, count.longValue() );
			count = ( Long ) s.createQuery( "select count(*) from Suite" ).list().get( 0 );
			assertEquals( 0, count.longValue() );
			s.remove( p );
		} );
	}

	@Test
	public void testCascadeAllDeleteOrphanFromClearedPersistentAssnToTransientEntity(SessionFactoryScope factoryScope) {
		final Address detachedAddress = factoryScope.fromTransaction( (s) -> {
			Address address = new Address();
			address.setInfo( "123 Main St." );
			Suite suite = new Suite();
			address.getSuites().add( suite );
			s.persist( address );
			return address;
		} );

		final Address detachedAddress2 = factoryScope.fromTransaction( (s) -> {
			Suite suite = detachedAddress.getSuites().iterator().next();
			Note note = new Note();
			note.setDescription( "a description" );
			suite.getNotes().add( note );
			detachedAddress.getSuites().clear();
			return s.merge( detachedAddress );
		} );

		factoryScope.inTransaction( (s) -> {
			Long count = ( Long ) s.createQuery( "select count(*) from Suite" ).list().get( 0 );
			assertEquals(
					0,
					count.longValue(),
					"all-delete-orphan not cascaded properly to cleared persistent collection entities"
			);
			count = ( Long ) s.createQuery( "select count(*) from Note" ).list().get( 0 );
			assertEquals( 0, count.longValue() );
			s.remove( detachedAddress2 );
		} );
	}
}
