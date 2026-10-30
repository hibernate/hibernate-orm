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
	}

	@Test
	public void versionCanBeReplacedWithoutChangingOtherEntryState() {
		final var entry = createEntityEntry();
		final var snapshot = entry.getLoadedState();
		final var deletedState = new Object[] { "deleted" };
		entry.setDeletedState( deletedState );
		entry.setVersion( 24L );
		assertThat( entry.getVersion() ).isEqualTo( 24L );
		entry.setVersion( null );
		assertThat( entry.getVersion() ).isNull();
		assertThat( entry.getLoadedState() ).isSameAs( snapshot ).containsExactly( "loaded", 23L );
		assertThat( entry.getDeletedState() ).isSameAs( deletedState );
		assertThat( entry.getId() ).isEqualTo( 42L );
		assertThat( entry.getRowId() ).isEqualTo( 1L );
		assertThat( entry.getLockMode() ).isEqualTo( LockMode.OPTIMISTIC );
		assertThat( entry.getStatus() ).isEqualTo( Status.MANAGED );
		assertThat( entry.isExistsInDatabase() ).isTrue();
	}

	@Test
	public void versionCanBeReplacedWithoutCreatingAReadOnlySnapshot() {
		final var entry = createEntityEntry();
		entry.setStatus( Status.READ_ONLY );
		entry.setVersion( 24L );
		assertThat( entry.getVersion() ).isEqualTo( 24L );
		assertThat( entry.getLoadedState() ).isNull();
		assertThat( entry.getStatus() ).isEqualTo( Status.READ_ONLY );
		assertThat( entry.getLockMode() ).isEqualTo( LockMode.OPTIMISTIC );
	}

	@Test
	public void testLockModeCanBeSetAndDoesNotAffectOtherPackedAttributes() {
		// Given
		EntityEntry entityEntry = createEntityEntry();
		assertThat( entityEntry.getLockMode() ).isEqualTo( LockMode.OPTIMISTIC );
		assertThat( entityEntry.getStatus() ).isEqualTo( Status.MANAGED );
		assertThat( entityEntry.isExistsInDatabase() ).isTrue();
		// When
		entityEntry.setLockMode( LockMode.PESSIMISTIC_READ );
		// Then
		assertThat( entityEntry.getLockMode() ).isEqualTo( LockMode.PESSIMISTIC_READ );
		assertThat( entityEntry.getStatus() ).isEqualTo( Status.MANAGED );
		assertThat( entityEntry.isExistsInDatabase() ).isTrue();
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
	}

	@Test
	public void testSerializationAndDeserializationKeepCorrectPackedAttributes() throws Exception {
		EntityEntry entityEntry = createEntityEntry();

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream( baos );
		entityEntry.serialize( oos );
		oos.flush();

		InputStream is = new ByteArrayInputStream( baos.toByteArray() );
		final var entityEntryClass =
				Class.forName( "org.hibernate.engine.internal.EntityEntryImpl" );

		final var deserializeMethod =
				entityEntryClass.getDeclaredMethod( "deserialize",
						ObjectInputStream.class, PersistenceContext.class );
		EntityEntry deserializedEntry =
				(EntityEntry) deserializeMethod.invoke( null, new ObjectInputStream( is ),
						getPersistenceContextMock() );

		assertThat( deserializedEntry.getLockMode() ).isEqualTo( LockMode.OPTIMISTIC );
		assertThat( deserializedEntry.getStatus() ).isEqualTo( Status.MANAGED );
		assertThat( deserializedEntry.isExistsInDatabase() ).isTrue();
	}

	private EntityEntry createEntityEntry() {
		return new EntityEntryImpl(
				Status.MANAGED,
				new Object[] { "loaded", 23L },
				1L,
				42L,
				23L,
				LockMode.OPTIMISTIC,
				true,
				null,
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
