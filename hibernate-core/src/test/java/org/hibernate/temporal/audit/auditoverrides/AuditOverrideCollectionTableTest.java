/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.temporal.audit.auditoverrides;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Table;
import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.AuditOverride;
import org.hibernate.annotations.AuditOverrides;
import org.hibernate.annotations.Audited;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.mapping.Column;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SessionFactory
@DomainModel(annotatedClasses = {
		AuditOverrideCollectionTableTest.EntityWithOverrides.class,

})
@ServiceRegistry(settings = @Setting(name = StateManagementSettings.CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.AuditEntityTest$TxIdSupplier"))
public class AuditOverrideCollectionTableTest {
	private static int currentTxId;

	public static class TxIdSupplier implements ChangesetIdentifierSupplier<Integer> {
		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++currentTxId;
		}
	}

	/**
	 * Case 1:
	 * MSC: -
	 * MSC: @Audited.Excluded
	 * Entity: @Audited
	 *
	 */

	@MappedSuperclass
	@Audited
	static class MSCWithExcludedCollectionProperty {
		@Id
		long id;

		@ElementCollection
		@Audited.Excluded
		List<String> firstCollection;

		@ElementCollection
		List<String> secondCollection;
	}

	@Entity
	@Table(name = "EntityWithOverrides")
	@AuditOverrides(
			{@AuditOverride(name = "firstCollection", isAudited = true),
					@AuditOverride(name = "secondCollection", isAudited = false)})
	static class EntityWithOverrides extends MSCWithExcludedCollectionProperty{
	}

	@Test
	public void elementCollection(DomainModelScope domainModelScope) {
		var tables = domainModelScope.getDomainModel().collectTableMappings();
		var tableNames = tables.stream().map( org.hibernate.mapping.Table::getName ).collect( Collectors.toSet() );
		assertTrue( tableNames.contains( "AuditOverrideCollectionTableTest$EntityWithOverrides_firstCollection" ) );
		assertTrue( tableNames.contains( "AuditOverrideCollectionTableTest$EntityWithOverrides_firstCollection_AUD" ) );
		assertTrue( tableNames.contains( "AuditOverrideCollectionTableTest$EntityWithOverrides_secondCollection" ) );
		assertFalse( tableNames.contains( "AuditOverrideCollectionTableTest$EntityWithOverrides_secondCollection_AUD" ) );
	}

	private static void assertTable(Collection<org.hibernate.mapping.Table> tables, String tableName, Consumer<org.hibernate.mapping.Table> consumer) {
		var tableFound = false;
		for ( var table : tables ) {
			if ( table.getName().equals( tableName ) ) {
				tableFound = true;
				consumer.accept( table );
			}
		}
		assertTrue( tableFound, () -> "Table %s not found. Available tables: %s".formatted( tableName, tables ) );
	}

}
