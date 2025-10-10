/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.engine.spi;

import org.hibernate.LockMode;
import org.hibernate.engine.internal.EntityEntryImpl;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
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
		assertThat( entityEntry.getLockMode() ).isEqualTo( LockMode.OPTIMISTIC );
		assertThat( entityEntry.getStatus() ).isEqualTo( Status.MANAGED );
		assertThat( entityEntry.isExistsInDatabase() ).isTrue();
		assertThat( entityEntry.isBeingReplicated() ).isTrue();
	}

	@Test
	public void testLockModeCanBeSetAndDoesNotAffectOtherPackedAttributes() {
		// Given
		EntityEntry entityEntry = createEntityEntry();
		assertThat( entityEntry.getLockMode() ).isEqualTo( LockMode.OPTIMISTIC );
		assertThat( entityEntry.getStatus() ).isEqualTo( Status.MANAGED );
		assertThat( entityEntry.isExistsInDatabase() ).isTrue();
		assertThat( entityEntry.isBeingReplicated() ).isTrue();
		// When
		entityEntry.setLockMode( LockMode.PESSIMISTIC_READ );
		// Then
		assertThat( entityEntry.getLockMode() ).isEqualTo( LockMode.PESSIMISTIC_READ );
		assertThat( entityEntry.getStatus() ).isEqualTo( Status.MANAGED );
		assertThat( entityEntry.isExistsInDatabase() ).isTrue();
		assertThat( entityEntry.isBeingReplicated() ).isTrue();
	}

	@Test
	public void testStatusCanBeSetAndDoesNotAffectOtherPackedAttributes() {
		// Given
		EntityEntry entityEntry = createEntityEntry();
		// When
		entityEntry.setStatus( Status.DELETED );
		// Then
		assertThat( entityEntry.getLockMode() ).isEqualTo( LockMode.OPTIMISTIC );
		assertThat( entityEntry.getStatus() ).isEqualTo( Status.DELETED );
		assertThat( entityEntry.isExistsInDatabase() ).isTrue();
		assertThat( entityEntry.isBeingReplicated() ).isTrue();
	}

	@Test
	public void testPostDeleteSetsStatusAndExistsInDatabaseWithoutAffectingOtherPackedAttributes() {
		// Given
		EntityEntry entityEntry = createEntityEntry();
		// When
		entityEntry.postDelete();
		// Then
		assertThat( entityEntry.getLockMode() ).isEqualTo( LockMode.OPTIMISTIC );
		assertThat( entityEntry.getStatus() ).isEqualTo( Status.GONE );
		assertThat( entityEntry.isExistsInDatabase() ).isFalse();
		assertThat( entityEntry.isBeingReplicated() ).isTrue();
	}

	@Test
	public void testSerializationAndDeserializationKeepCorrectPackedAttributes() throws Exception {
		EntityEntry entityEntry = createEntityEntry();

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream( baos );
		entityEntry.serialize( oos );
		oos.flush();

		InputStream is = new ByteArrayInputStream( baos.toByteArray() );
		EntityEntry deserializedEntry =
				EntityEntryImpl.deserialize( new ObjectInputStream( is ),
						getPersistenceContextMock() );

		assertThat( deserializedEntry.getLockMode() ).isEqualTo( LockMode.OPTIMISTIC );
		assertThat( deserializedEntry.getStatus() ).isEqualTo( Status.MANAGED );
		assertThat( deserializedEntry.isExistsInDatabase() ).isTrue();
		assertThat( deserializedEntry.isBeingReplicated() ).isTrue();
	}

	private EntityEntry createEntityEntry() {
		return new EntityEntryImpl(
				Status.MANAGED,
				new Object[] {},
				1L,
				42L,
				23L,
				LockMode.OPTIMISTIC,
				true,
				null,
				true,
				getPersistenceContextMock()
		);
	}

	private PersistenceContext getPersistenceContextMock() {
		SessionImplementor sessionMock = mock( SessionImplementor.class );
		PersistenceContext persistenceContextMock = mock( PersistenceContext.class );
		when( persistenceContextMock.getSession() ).thenReturn( sessionMock );
		return persistenceContextMock;
	}
}
