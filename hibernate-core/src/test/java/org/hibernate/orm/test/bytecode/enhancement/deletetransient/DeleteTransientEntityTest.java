/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.deletetransient;

import org.hibernate.orm.test.bytecode.enhancement.lazy.NoDirtyCheckingContext;
import org.hibernate.orm.test.bytecode.enhancement.lazy.proxy.inlinedirtychecking.DirtyCheckEnhancementContext;
import org.hibernate.orm.test.deletetransient.Address;
import org.hibernate.orm.test.deletetransient.Note;
import org.hibernate.orm.test.deletetransient.Person;
import org.hibernate.orm.test.deletetransient.Suite;

import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
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
@BytecodeEnhanced
@CustomEnhancementContext({ NoDirtyCheckingContext.class, DirtyCheckEnhancementContext.class })
public class DeleteTransientEntityTest {

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testTransientEntityDeletionNoCascades(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.remove( new Address() );
		} );
	}

	@Test
	public void testTransientEntityDeletionCascadingToTransientAssociation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Person p = new Person( 1, "steve" );
			p.getAddresses().add( new Address( 1, "123 Main St." ) );
			session.persist( p );
		} );
	}

	@Test
	public void testTransientEntityDeleteCascadingToCircularity(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			Person p1 = new Person( 1, "steve" );
			Person p2 = new Person( 2, "john" );
			p1.getFriends().add( p2 );
			p2.getFriends().add( p1 );
			session.persist( p1 );
		} );
	}

	@Test
	public void testTransientEntityDeletionCascadingToDetachedAssociation(SessionFactoryScope scope) {
		Address address = new Address( 1, "123 Main St." );

		scope.inTransaction(session -> {
			session.persist( address );
		} );

		scope.inTransaction( session -> {
			Person p = new Person( 1, "steve");
			p.getAddresses().add( address );
			session.remove( p );
		} );

		scope.inTransaction(session -> {
			var count = session.createQuery( "select count(*) from Address", Long.class ).uniqueResult();
			assertEquals( 0, count.longValue(), "delete not cascaded properly across transient entity" );
		} );
	}

	@Test
	public void testTransientEntityDeletionCascadingToPersistentAssociation(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			var address = new Address( 1, "123 Main St." );
			session.persist( address );
		} );

		scope.inTransaction(session -> {
			var address = session.find( Address.class, 1 );
			var p = new Person( 1, "steve");
			p.getAddresses().add( address );
			session.remove( p );
		} );

		scope.inTransaction(session -> {
			var count = session.createQuery( "select count(*) from Address", Long.class ).uniqueResult();
			assertEquals( 0, count.longValue(), "delete not cascaded properly across transient entity" );
		} );
	}

	@Test
	public void testCascadeAllFromClearedPersistentAssnToTransientEntity(SessionFactoryScope scope) {
		final Person p = new Person( 1, "steve" );
		Address address = new Address( 1, "123 Main St." );

		scope.inTransaction(session -> {
			p.getAddresses().add( address );
			session.persist( p );
		} );

		scope.inTransaction(session -> {
			Suite suite = new Suite();
			address.getSuites().add( suite );
			p.getAddresses().clear();
			session.merge( p );
		} );

		scope.inTransaction(session -> {
			Person person = session.find( p.getClass(), p.getId() );
			assertEquals( 0, person.getAddresses().size(), "persistent collection not cleared" );
			Long count = (Long) session.createQuery( "select count(*) from Address" ).list().get( 0 );
			assertEquals( 1, count.longValue() );
			count = (Long) session.createQuery( "select count(*) from Suite" ).list().get( 0 );
			assertEquals( 0, count.longValue() );
		} );
	}

	@Test
	@SuppressWarnings({ "unchecked" })
	public void testCascadeAllDeleteOrphanFromClearedPersistentAssnToTransientEntity(SessionFactoryScope scope) {
		var address = new Address( 1, "123 Main St." );
		var suite = new Suite( 1, "Colorado" );
		address.getSuites().add( suite );

		scope.inTransaction(session -> {
			session.persist( address );
		} );

		scope.inTransaction( session -> {
			Note note = new Note( 1, "a description" );
			suite.getNotes().add( note );
			address.getSuites().clear();
			session.merge( address );
		} );

		scope.inTransaction(session -> {
			Long count = (Long) session.createQuery( "select count(*) from Suite" ).list().get( 0 );
			assertEquals(
					0,
					count.longValue(),
					"all-delete-orphan not cascaded properly to cleared persistent collection entities"
			);
			count = (Long) session.createQuery( "select count(*) from Note" ).list().get( 0 );
			assertEquals( 0, count.longValue() );
		} );
	}
}
