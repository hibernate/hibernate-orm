package org.hibernate.orm.test.version;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Version;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

/// Verifies that an unsaved version does not allow persist to replace an existing generated identifier.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = {
		PersistExistingGeneratedIdTest.UnversionedItem.class,
		PersistExistingGeneratedIdTest.BoxedVersionItem.class,
		PersistExistingGeneratedIdTest.PrimitiveVersionItem.class
})
@SessionFactory
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSequences.class)
@JiraKey("HHH-4181")
public class PersistExistingGeneratedIdTest {
	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@ParameterizedTest
	@EnumSource(VersionKind.class)
	void testPersistWithOriginalManaged(VersionKind kind, SessionFactoryScope scope) {
		verifyRejected( kind, true, scope );
	}

	@ParameterizedTest
	@EnumSource(VersionKind.class)
	void testPersistInFreshSession(VersionKind kind, SessionFactoryScope scope) {
		verifyRejected( kind, false, scope );
	}

	private void verifyRejected(VersionKind kind, boolean loadOriginal, SessionFactoryScope scope) {
		var original = newItem( kind );
		original.name = "original";
		scope.inTransaction( session -> session.persist( original ) );

		var duplicate = newItem( kind );
		duplicate.id = original.id;
		duplicate.name = "duplicate";
		assertThrows( PersistenceException.class, () -> scope.inTransaction( session -> {
			if ( loadOriginal ) {
				session.find( original.getClass(), original.id );
			}
			session.persist( duplicate );
			fail( "persist must reject the existing generated identifier immediately" );
		} ) );
		assertEquals( original.id, duplicate.id );

		scope.inTransaction( session -> {
			var items = session.createQuery(
					"from " + original.getClass().getName(), original.getClass()
			).getResultList();
			assertEquals( 1, items.size() );
			assertEquals( original.id, items.get( 0 ).id );
			assertEquals( "original", items.get( 0 ).name );
		} );
	}

	private static Item newItem(VersionKind kind) {
		return switch ( kind ) {
			case NONE -> new UnversionedItem();
			case BOXED -> new BoxedVersionItem();
			case PRIMITIVE -> new PrimitiveVersionItem();
		};
	}

	enum VersionKind {
		NONE, BOXED, PRIMITIVE
	}

	@MappedSuperclass
	public static abstract class Item {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		Long id;
		String name;
	}

	@Entity(name = "UnversionedPersistItem")
	public static class UnversionedItem extends Item {
	}

	@Entity(name = "BoxedVersionPersistItem")
	public static class BoxedVersionItem extends Item {
		@Version
		Long version;
	}

	@Entity(name = "PrimitiveVersionPersistItem")
	public static class PrimitiveVersionItem extends Item {
		@Version
		long version;
	}
}
