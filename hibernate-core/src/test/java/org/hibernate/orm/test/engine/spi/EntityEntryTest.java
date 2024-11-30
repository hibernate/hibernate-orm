/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.engine.spi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.hibernate.LockMode;
import org.hibernate.engine.internal.MutableEntityEntry;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for setting and getting the enum/boolean values stored in the compressed state int.
 *
 * @author Gunnar Morling
 */
public class EntityEntryTest {

	@Test
	public void packedAttributesAreSetByConstructor() {
		EntityEntry entityEntry = createEntityEntry();

		assertEquals( LockMode.OPTIMISTIC, entityEntry.getLockMode() );
		Assert.assertEquals( Status.MANAGED, entityEntry.getStatus() );
		assertEquals( true, entityEntry.isExistsInDatabase() );
		assertEquals( true, entityEntry.isBeingReplicated() );
	}

	@Test
	public void testLockModeCanBeSetAndDoesNotAffectOtherPackedAttributes() {
		// Given
		EntityEntry entityEntry = createEntityEntry();

		assertEquals( LockMode.OPTIMISTIC, entityEntry.getLockMode() );
		assertEquals( Status.MANAGED, entityEntry.getStatus() );
		assertEquals( true, entityEntry.isExistsInDatabase() );
		assertEquals( true, entityEntry.isBeingReplicated() );

		// When
		entityEntry.setLockMode( LockMode.PESSIMISTIC_READ );

		// Then
		assertEquals( LockMode.PESSIMISTIC_READ, entityEntry.getLockMode() );
		assertEquals( Status.MANAGED, entityEntry.getStatus() );
		assertEquals( true, entityEntry.isExistsInDatabase() );
		assertEquals( true, entityEntry.isBeingReplicated() );
	}

	@Test
	public void testStatusCanBeSetAndDoesNotAffectOtherPackedAttributes() {
		// Given
		EntityEntry entityEntry = createEntityEntry();

		// When
		entityEntry.setStatus( Status.DELETED );

		// Then
		assertEquals( LockMode.OPTIMISTIC, entityEntry.getLockMode() );
		assertEquals( Status.DELETED, entityEntry.getStatus() );
		assertEquals( true, entityEntry.isExistsInDatabase() );
		assertEquals( true, entityEntry.isBeingReplicated() );
	}

	@Test
	public void testPostDeleteSetsStatusAndExistsInDatabaseWithoutAffectingOtherPackedAttributes() {
		// Given
		EntityEntry entityEntry = createEntityEntry();

		// When
		entityEntry.postDelete();

		// Then
		assertEquals( LockMode.OPTIMISTIC, entityEntry.getLockMode() );
		assertEquals( Status.GONE, entityEntry.getStatus() );
		assertEquals( false, entityEntry.isExistsInDatabase() );
		assertEquals( true, entityEntry.isBeingReplicated() );
	}

	@Test
	public void testSerializationAndDeserializationKeepCorrectPackedAttributes() throws Exception {
		EntityEntry entityEntry = createEntityEntry();

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream( baos );
		entityEntry.serialize(oos);
		oos.flush();

		InputStream is = new ByteArrayInputStream( baos.toByteArray() );
		EntityEntry deserializedEntry = MutableEntityEntry.deserialize(new ObjectInputStream( is ), getPersistenceContextMock() );

		assertEquals( LockMode.OPTIMISTIC, deserializedEntry.getLockMode() );
		assertEquals( Status.MANAGED, deserializedEntry.getStatus() );
		assertEquals( true, deserializedEntry.isExistsInDatabase() );
		assertEquals( true, deserializedEntry.isBeingReplicated() );
	}

	private EntityEntry createEntityEntry() {

		return new MutableEntityEntry(
				// status
				Status.MANAGED,
				// loadedState
				new Object[]{},
				// rowId
				1L,
				// id
				42L,
				// version
				23L,
				// lockMode
				LockMode.OPTIMISTIC,
				// existsInDatabase
				true,
				// persister
				null,
				// disableVersionIncrement
				true,
				getPersistenceContextMock()
		);
	}

	private final PersistenceContext getPersistenceContextMock() {
		SessionImplementor sessionMock = mock( SessionImplementor.class );
		PersistenceContext persistenceContextMock = mock( PersistenceContext.class );
		when( persistenceContextMock.getSession() ).thenReturn( sessionMock );

		return persistenceContextMock;
	}
}
