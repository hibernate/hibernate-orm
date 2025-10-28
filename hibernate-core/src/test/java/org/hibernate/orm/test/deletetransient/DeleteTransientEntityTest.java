/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.deletetransient;

import org.hibernate.internal.util.MutableObject;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {Person.class, Address.class, Suite.class, Note.class})
@SessionFactory
public class DeleteTransientEntityTest {
	@AfterEach
	void dropTestData(SessionFactoryScope sessions) {
		sessions.dropData();
	}

	@Test
	public void testTransientEntityDeletionNoCascades(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			session.remove( new Address() );
		} );
	}

	@Test
	public void testTransientEntityDeletionCascadingToTransientAssociation(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			Person p = new Person();
			p.getAddresses().add( new Address() );
			session.remove( p );
		} );
	}

	@Test
	public void testTransientEntityDeleteCascadingToCircularity(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			Person p1 = new Person();
			Person p2 = new Person();
			p1.getFriends().add( p2 );
			p2.getFriends().add( p1 );
			session.remove( p1 );
		} );
	}

	@Test
	public void testTransientEntityDeletionCascadingToDetachedAssociation(SessionFactoryScope sessions) {
		var address = sessions.fromTransaction( (session) -> {
			Address created = new Address( 1, "123 Main St." );
			session.persist( created );
			return created;
		} );

		sessions.inTransaction( (session) -> {
			Person p = new Person();
			p.getAddresses().add( address );
			session.remove( p );
		} );

		sessions.inTransaction( (session) -> {
			var count = session.createQuery( "select count(*) from Address", Long.class ).list().get( 0 );
			assertEquals( 0, count, "delete not cascaded properly across transient entity" );
		} );
	}

	@Test
	public void testTransientEntityDeletionCascadingToPersistentAssociation(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			Address address = new Address( 1, "123 Main St." );
			session.persist( address );
		} );

		sessions.inTransaction( (session) -> {
			var address = session.find( Address.class, 1 );
			Person p = new Person();
			p.getAddresses().add( address );
			session.remove( p );
		} );

		sessions.inTransaction( (session) -> {
			var count = session.createQuery( "select count(*) from Address", Long.class ).list().get( 0 );
			assertEquals( 0, count, "delete not cascaded properly across transient entity" );
		} );
	}

	@Test
	public void testCascadeAllFromClearedPersistentAssnToTransientEntity(SessionFactoryScope sessions) {
		final MutableObject<Person> personRef = new MutableObject<>();
		final MutableObject<Address> addressRef = new MutableObject<>();

		sessions.inTransaction( (session) -> {
			var person = new Person( 1, "steve" );
			var address = new Address( 1, "123 Main St." );
			person.getAddresses().add( address );
			session.persist( person );
			personRef.set( person );
			addressRef.set( address );
		} );

		sessions.inTransaction( (session) -> {
			var person = personRef.get();
			var address = addressRef.get();
			var suite = new Suite( 1, "Colorado" );
			address.getSuites().add( suite );
			person.getAddresses().clear();
			personRef.set( session.merge( person ) );
		} );

		sessions.inTransaction( (session) -> {
			var person = personRef.get();
			var address = addressRef.get();
			var suite = new Suite( 2, "Utah" );
			address.getSuites().add( suite );
			person.getAddresses().clear();
			personRef.set( session.merge( person ) );
		} );

		sessions.inTransaction( (session) -> {
			var person = session.find( Person.class, 1 );
			assertEquals( 0, person.getAddresses().size(), "persistent collection not cleared" );
			var count = session.createQuery( "select count(*) from Address", Long.class ).list().get( 0 );
			assertEquals( 1, count.longValue() );
			count = session.createQuery( "select count(*) from Suite", Long.class ).list().get( 0 );
			assertEquals( 0, count.longValue() );
		} );
	}

	@Test
	@SuppressWarnings( {"unchecked"})
	public void testCascadeAllDeleteOrphanFromClearedPersistentAssnToTransientEntity(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			var address = new Address( 1, "123 Main St." );
			var suite = new Suite( 1, "Colorado" );
			address.getSuites().add( suite );
			session.persist( address );
		} );

		sessions.inTransaction( (session) -> {
			var address = session.find( Address.class, 1 );
			var suite = session.find( Suite.class, 1 );
			var note = new Note( 1, "a description" );
			suite.getNotes().add( note );
			address.getSuites().clear();
			session.merge( address );
		} );

		sessions.inTransaction( (session) -> {
			var count = session.createQuery( "select count(*) from Suite", Long.class ).uniqueResult();
			assertEquals( 0, count.longValue(),
					"all-delete-orphan not cascaded properly to cleared persistent collection entities" );
			count = session.createQuery( "select count(*) from Note", Long.class ).uniqueResult();
			assertEquals( 0, count.longValue() );
		} );
	}
}
