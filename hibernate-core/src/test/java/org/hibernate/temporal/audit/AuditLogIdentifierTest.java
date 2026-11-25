package org.hibernate.temporal.audit;

import java.util.List;

import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Audited;
import org.hibernate.audit.AuditEntry;
import org.hibernate.audit.AuditLogFactory;
import org.hibernate.audit.ModificationType;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;

import org.hibernate.testing.orm.junit.AuditedTest;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The {@link org.hibernate.audit.AuditLog} operations which take an entity identifier
 * match it against the identifier of the entity, even when the entity has an attribute
 * named {@code id} which is not, or not all of, its identifier.
 */
@AuditedTest
@SessionFactory
@DomainModel(annotatedClasses = {
		AuditLogIdentifierTest.IdClassEntity.class,
		AuditLogIdentifierTest.KeyedEntity.class
})
@ServiceRegistry(settings = @Setting(name = StateManagementSettings.CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.AuditLogIdentifierTest$TxIdSupplier"))
@JiraKey("HHH-21267")
class AuditLogIdentifierTest {
	private static int currentTxId;

	public static class TxIdSupplier implements ChangesetIdentifierSupplier<Integer> {
		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++currentTxId;
		}
	}

	/// An attribute named `id` is part of the identifier.
	@Audited
	@Entity(name = "IdentifierIdClassEntity")
	@IdClass(IdClassEntity.Key.class)
	static class IdClassEntity {
		@Id
		Long id;
		@Id
		@Column(name = "sub_id")
		Long subId;
		String name;

		record Key(Long id, Long subId) {
		}
	}

	/// An attribute named `id` is not the identifier.
	@Audited
	@Entity(name = "IdentifierKeyedEntity")
	static class KeyedEntity {
		@Id
		@Column(name = "entity_key")
		Long key;
		@Column(name = "external_id")
		Long id;
		String name;
	}

	@Test
	void testIdClassWithIdAttribute(SessionFactoryScope scope) {
		currentTxId = 0;
		scope.inTransaction( session -> {
			final var entity = new IdClassEntity();
			entity.id = 1L;
			entity.subId = 2L;
			entity.name = "original";
			session.persist( entity );
		} );
		final var key = new IdClassEntity.Key( 1L, 2L );
		scope.inTransaction( session -> session.find( IdClassEntity.class, key ).name = "updated" );

		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			assertEquals( List.of( 1, 2 ), auditLog.getChangesets( IdClassEntity.class, key ) );
			assertEquals( ModificationType.MOD, auditLog.getModificationType( IdClassEntity.class, key, 2 ) );
			assertEquals(
					List.of( "original", "updated" ),
					auditLog.getHistory( IdClassEntity.class, key ).stream()
							.map( entry -> entry.entity().name )
							.toList()
			);
		}
	}

	@Test
	void testIdAttributeWhichIsNotTheIdentifier(SessionFactoryScope scope) {
		currentTxId = 100;
		scope.inTransaction( session -> {
			// the value of its id attribute is the identifier of the other entity
			final var entity = new KeyedEntity();
			entity.key = 10L;
			entity.id = 20L;
			entity.name = "first";
			session.persist( entity );
			final var other = new KeyedEntity();
			other.key = 20L;
			other.id = 30L;
			other.name = "second";
			session.persist( other );
		} );
		scope.inTransaction( session -> session.find( KeyedEntity.class, 10L ).name = "updated" );

		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			assertEquals( List.of( 101, 102 ), auditLog.getChangesets( KeyedEntity.class, 10L ) );
			assertEquals( List.of( 101 ), auditLog.getChangesets( KeyedEntity.class, 20L ) );
			assertEquals( ModificationType.MOD, auditLog.getModificationType( KeyedEntity.class, 10L, 102 ) );
			assertEquals(
					List.of( ModificationType.ADD, ModificationType.MOD ),
					auditLog.getHistory( KeyedEntity.class, 10L ).stream()
							.map( AuditEntry::modificationType )
							.toList()
			);
		}
	}
}
