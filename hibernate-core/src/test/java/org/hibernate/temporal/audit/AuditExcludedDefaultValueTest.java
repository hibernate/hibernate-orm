package org.hibernate.temporal.audit;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Audited;
import org.hibernate.audit.AuditLogFactory;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.type.YesNoConverter;
import org.hibernate.testing.orm.junit.AuditedTest;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AuditedTest
@SessionFactory
@DomainModel(annotatedClasses = {
		AuditExcludedDefaultValueTest.BoxedEntity.class,
		AuditExcludedDefaultValueTest.PrimitiveEntity.class,
		AuditExcludedDefaultValueTest.PropertyEntity.class
})
@ServiceRegistry(settings = @Setting(name = StateManagementSettings.CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.AuditExcludedDefaultValueTest$TxIdSupplier"))
class AuditExcludedDefaultValueTest {
	private static int currentTxId;

	public static class TxIdSupplier implements ChangesetIdentifierSupplier<Integer> {
		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++currentTxId;
		}
	}

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
		currentTxId = 0;
		scope.inTransaction( session -> {
			final var boxed = new BoxedEntity();
			boxed.id = 1;
			boxed.name = "boxed";
			boxed.counter = 9;
			boxed.enabled = true;
			boxed.marker = 'X';
			session.persist( boxed );

			final var primitive = new PrimitiveEntity();
			primitive.id = 2;
			primitive.name = "primitive";
			primitive.counter = 9;
			primitive.enabled = true;
			primitive.marker = 'X';
			session.persist( primitive );

			final var property = new PropertyEntity();
			property.setId( 3 );
			property.setName( "property" );
			session.persist( property );
		} );
	}

	@Test
	void excludedBoxedAttributesAreNullInAuditLog(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			assertBoxedAttributesAreNull( auditLog.find( BoxedEntity.class, 1, 1 ) );
		}
	}

	@Test
	void excludedBoxedAttributesAreNullAtChangeset(SessionFactoryScope scope) {
		try (var session = scope.getSessionFactory().withOptions().atChangeset( 1 ).openSession()) {
			assertBoxedAttributesAreNull( session.find( BoxedEntity.class, 1 ) );
		}
	}

	private void assertBoxedAttributesAreNull(BoxedEntity entity) {
		assertNotNull( entity );
		assertEquals( "boxed", entity.name );
		// No historical values were recorded, and these reference types can represent their absence.
		assertAll(
				() -> assertNull( entity.counter, "excluded Integer" ),
				() -> assertNull( entity.enabled, "excluded Boolean" ),
				() -> assertNull( entity.marker, "excluded Character" )
		);
	}

	@Test
	void excludedPrimitiveAttributesRetainJavaDefaults(SessionFactoryScope scope) {
		// Returning null for every excluded attribute would make primitive assignment fail.
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			final var entity = auditLog.find( PrimitiveEntity.class, 2, 1 );
			assertNotNull( entity );
			assertEquals( "primitive", entity.name );
			assertAll(
					() -> assertEquals( 0, entity.counter ),
					() -> assertFalse( entity.enabled ),
					() -> assertEquals( '\0', entity.marker )
			);
		}
	}

	@Test
	void excludedConvertedPropertiesUseDeclaredTypes(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			assertPropertyDefaults( auditLog.find( PropertyEntity.class, 3, 1 ) );
		}
	}

	@Test
	void excludedConvertedPropertiesUseDeclaredTypesInQuery(SessionFactoryScope scope) {
		try (var session = scope.getSessionFactory().withOptions().atChangeset( 1 ).openSession()) {
			assertPropertyDefaults( session.createSelectionQuery(
					"from AuditPropertyDefaults", PropertyEntity.class ).getSingleResult() );
		}
	}

	private void assertPropertyDefaults(PropertyEntity entity) {
		assertNotNull( entity );
		assertEquals( "property", entity.getName() );
		assertFalse( entity.isEnabled() );
		assertNull( entity.getOptionalEnabled() );
	}

	@Test
	void currentEntitiesRetainExcludedValues(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var boxed = session.find( BoxedEntity.class, 1 );
			assertNotNull( boxed );
			assertEquals( 9, boxed.counter );
			assertTrue( boxed.enabled );
			assertEquals( 'X', boxed.marker );

			final var primitive = session.find( PrimitiveEntity.class, 2 );
			assertNotNull( primitive );
			assertEquals( 9, primitive.counter );
			assertTrue( primitive.enabled );
			assertEquals( 'X', primitive.marker );

			final var property = session.find( PropertyEntity.class, 3 );
			assertNotNull( property );
			assertTrue( property.isEnabled() );
			assertTrue( property.getOptionalEnabled() );
		} );
	}

	@Audited
	@Entity(name = "AuditBoxedDefaults")
	static class BoxedEntity {
		@Id
		Integer id;
		String name;
		@Audited.Excluded
		Integer counter;
		@Audited.Excluded
		Boolean enabled;
		@Audited.Excluded
		Character marker;
	}

	@Audited
	@Entity(name = "AuditPrimitiveDefaults")
	static class PrimitiveEntity {
		@Id
		Integer id;
		String name;
		@Audited.Excluded
		int counter = 7;
		@Audited.Excluded
		boolean enabled = true;
		@Audited.Excluded
		char marker = 'Y';
	}

	@Audited
	@Entity(name = "AuditPropertyDefaults")
	@Access(AccessType.PROPERTY)
	static class PropertyEntity {
		private Integer id;
		private String name;
		// The mapped getter is primitive even though its backing field is boxed.
		private Boolean enabled = true;
		private Boolean optionalEnabled = true;

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Audited.Excluded
		@Convert(converter = YesNoConverter.class)
		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		@Audited.Excluded
		@Convert(converter = YesNoConverter.class)
		public Boolean getOptionalEnabled() {
			return optionalEnabled;
		}

		public void setOptionalEnabled(Boolean optionalEnabled) {
			this.optionalEnabled = optionalEnabled;
		}
	}
}
