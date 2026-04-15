/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.temporal.audit;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Audited;
import org.hibernate.audit.AuditLogFactory;
import org.hibernate.audit.ModificationType;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.testing.orm.junit.AuditedTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.temporal.spi.TransactionIdentifierSupplier;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that {@link Audited.Excluded @Audited.Excluded} properties
 * are correctly excluded from audit tables on both the write-side
 * (column not in audit table) and read-side (entity loads without
 * error, excluded property is null).
 */
@AuditedTest
@SessionFactory
@DomainModel(annotatedClasses = {
		AuditExcludedPropertyTest.MyEntity.class
})
@ServiceRegistry(settings = @Setting(name = StateManagementSettings.TRANSACTION_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.AuditExcludedPropertyTest$TxIdSupplier"))
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuditExcludedPropertyTest {
	private static int currentTxId;

	public static class TxIdSupplier implements TransactionIdentifierSupplier<Integer> {
		@Override
		public Integer generateTransactionIdentifier(SharedSessionContract session) {
			return ++currentTxId;
		}
	}

	@Audited
	@Entity(name = "MyEntity")
	static class MyEntity {
		@Id
		long id;
		String name;

		@Audited.Excluded
		String secret;

		@Audited.Excluded
		@Embedded
		Address address;

		@Audited.Excluded
		@ElementCollection
		List<String> tags = new ArrayList<>();
	}

	@Embeddable
	static class Address {
		String city;
		String street;
	}

	@BeforeClassTemplate
	void initData(SessionFactoryScope scope) {
		currentTxId = 0;
		// Rev 1: create entity
		scope.getSessionFactory().inTransaction( session -> {
			final var entity = new MyEntity();
			entity.id = 1L;
			entity.name = "visible";
			entity.secret = "hidden";
			final var addr = new Address();
			addr.city = "London";
			addr.street = "Baker St";
			entity.address = addr;
			entity.tags.add( "java" );
			entity.tags.add( "hibernate" );
			session.persist( entity );
		} );
		// Rev 2: update entity
		scope.getSessionFactory().inTransaction( session -> {
			final var entity = session.find( MyEntity.class, 1L );
			entity.name = "updated";
			entity.secret = "changed-secret";
			entity.address.city = "Paris";
		} );
	}

	private static final int revCreate = 1;
	private static final int revUpdate = 2;

	@Test
	@Order(0)
	void testNoCollectionAuditTableForExcludedCollection(DomainModelScope scope) {
		for ( var table : scope.getDomainModel().collectTableMappings() ) {
			assertFalse( table.getName().contains( "tags_AUD" ),
					"Excluded @ElementCollection should not have an audit table" );
		}
	}

	@Test
	@Order(1)
	void testAuditTableHasNoExcludedColumns(SessionFactoryScope scope) {
		// Read audit data via AuditLog.find (uses AuditEntityLoader / LoaderSelectBuilder)
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			final var entity = auditLog.find( MyEntity.class, 1L, revCreate );
			assertNotNull( entity );
			assertEquals( "visible", entity.name );
			// Excluded properties should be null/empty when loaded from audit table
			assertNull( entity.secret );
			assertNull( entity.address );
			assertNull( entity.tags );
		}
	}

	@Test
	@Order(2)
	void testExcludedPropertiesNullAfterUpdate(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			final var entity = auditLog.find( MyEntity.class, 1L, revUpdate );
			assertNotNull( entity );
			assertEquals( "updated", entity.name );
			assertNull( entity.secret );
			assertNull( entity.address );
			assertNull( entity.tags );
		}
	}

	@Test
	@Order(3)
	void testPointInTimeReadViaAtTransaction(SessionFactoryScope scope) {
		try (var s = scope.getSessionFactory().withOptions()
				.atTransaction( revCreate ).openSession()) {
			final var entity = s.find( MyEntity.class, 1L );
			assertNotNull( entity );
			assertEquals( "visible", entity.name );
			assertNull( entity.secret );
			assertNull( entity.address );
			assertNull( entity.tags );
		}
	}

	@Test
	@Order(4)
	void testGetHistoryWithExcludedProperties(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			final var history = auditLog.getHistory( MyEntity.class, 1L );
			assertEquals( 2, history.size() );
			assertEquals( ModificationType.ADD, history.get( 0 ).modificationType() );
			assertNull( history.get( 0 ).entity().secret );
			assertNull( history.get( 0 ).entity().address );
			assertNull( history.get( 0 ).entity().tags );
			assertEquals( ModificationType.MOD, history.get( 1 ).modificationType() );
			assertEquals( "updated", history.get( 1 ).entity().name );
			assertNull( history.get( 1 ).entity().secret );
			assertNull( history.get( 1 ).entity().tags );
		}
	}

	@Test
	@Order(5)
	void testCurrentDataStillHasExcludedProperties(SessionFactoryScope scope) {
		// Verify the current (non-audit) data still has the excluded properties
		scope.inSession( session -> {
			final var entity = session.find( MyEntity.class, 1L );
			assertNotNull( entity );
			assertEquals( "updated", entity.name );
			assertEquals( "changed-secret", entity.secret );
			assertNotNull( entity.address );
			assertEquals( "Paris", entity.address.city );
			assertEquals( 2, entity.tags.size() );
			assertTrue( entity.tags.contains( "java" ) );
		} );
	}
}
