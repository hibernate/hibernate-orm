/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.temporal.audit;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Audited;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.temporal.spi.TransactionIdentifierSupplier;
import org.hibernate.testing.orm.junit.AuditedTest;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests that {@link Audited.Table @Audited.Table} customizes
 * the audit table name and column names.
 */
@AuditedTest
@SessionFactory
@DomainModel(annotatedClasses = AuditCustomTableTest.CustomTableEntity.class)
@ServiceRegistry(settings = @Setting(name = StateManagementSettings.TRANSACTION_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.AuditCustomTableTest$TxIdSupplier"))
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuditCustomTableTest {
	private static int currentTxId;

	public static class TxIdSupplier implements TransactionIdentifierSupplier<Integer> {
		@Override
		public Integer generateTransactionIdentifier(SharedSessionContract session) {
			return ++currentTxId;
		}
	}

	@BeforeClassTemplate
	void setupData(SessionFactoryScope scope) {
		currentTxId = 0;

		scope.getSessionFactory().inTransaction( session -> {
			var e = new CustomTableEntity();
			e.id = 1L;
			e.name = "created";
			session.persist( e );
		} );

		scope.getSessionFactory().inTransaction( session -> {
			var e = session.find( CustomTableEntity.class, 1L );
			e.name = "updated";
		} );
	}

	@AfterAll
	void cleanupData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test

	void testCustomAuditTableName(SessionFactoryScope scope) {
		final var auditTable = scope.getMetadataImplementor()
				.getEntityBinding( CustomTableEntity.class.getName() )
				.getAuxiliaryTable();
		assertNotNull( auditTable, "Audit table should exist" );
		assertEquals( "MY_AUDIT_LOG", auditTable.getName() );
	}

	@Test
	void testCustomColumnNames(SessionFactoryScope scope) {
		final var auditTable = scope.getMetadataImplementor()
				.getEntityBinding( CustomTableEntity.class.getName() )
				.getAuxiliaryTable();
		assertNotNull(
				auditTable.getColumn( Identifier.toIdentifier( "TX_ID" ) ),
				"Custom transaction id column TX_ID should exist"
		);
		assertNotNull(
				auditTable.getColumn( Identifier.toIdentifier( "MOD_TYPE" ) ),
				"Custom modification type column MOD_TYPE should exist"
		);
	}

	@Test
	void testAuditReadWithCustomTable(SessionFactoryScope scope) {
		try (var s = scope.getSessionFactory().withOptions().atTransaction( 1 ).open()) {
			var e = s.find( CustomTableEntity.class, 1L );
			assertNotNull( e );
			assertEquals( "created", e.name );
		}

		try (var s = scope.getSessionFactory().withOptions().atTransaction( 2 ).open()) {
			var e = s.find( CustomTableEntity.class, 1L );
			assertNotNull( e );
			assertEquals( "updated", e.name );
		}
	}

	@Test
	void testAuditDeleteWithCustomTable(SessionFactoryScope scope) {
		currentTxId = 100;

		scope.getSessionFactory().inTransaction( session -> {
			var e = new CustomTableEntity();
			e.id = 99L;
			e.name = "to-delete";
			session.persist( e );
		} );

		scope.getSessionFactory().inTransaction( session -> {
			var e = session.find( CustomTableEntity.class, 99L );
			session.remove( e );
		} );

		try (var s = scope.getSessionFactory().withOptions().atTransaction( 101 ).open()) {
			var e = s.find( CustomTableEntity.class, 99L );
			assertNotNull( e );
			assertEquals( "to-delete", e.name );
		}

		try (var s = scope.getSessionFactory().withOptions().atTransaction( 102 ).open()) {
			var e = s.find( CustomTableEntity.class, 99L );
			assertNull( e );
		}
	}

	// ---- Entity ----

	@Audited
	@Audited.Table(name = "MY_AUDIT_LOG", transactionId = "TX_ID", modificationType = "MOD_TYPE")
	@Entity(name = "CustomTableEntity")
	static class CustomTableEntity {
		@Id
		long id;
		String name;
	}
}
