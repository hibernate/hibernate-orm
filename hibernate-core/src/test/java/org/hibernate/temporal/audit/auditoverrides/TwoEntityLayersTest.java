/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.temporal.audit.auditoverrides;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.SharedSessionContract;
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
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertTrue;


@SessionFactory
@DomainModel(annotatedClasses = {
		TwoEntityLayersTest.EntityWithExcludedProperty2.class,
		TwoEntityLayersTest.EntityThatRevokes.class,
})
@ServiceRegistry(settings = @Setting(name = StateManagementSettings.CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.AuditEntityTest$TxIdSupplier"))
public class TwoEntityLayersTest {
	private static int currentTxId;

	public static class TxIdSupplier implements ChangesetIdentifierSupplier<Integer> {
		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++currentTxId;
		}
	}


	/**
	 * Entity: @Audited.Excluded
	 * Entity: @Audited.Override.isAudited = true
	 *
	 */

	@Entity
	@Table(name = "EntityWithExcludedProperty2")
	@Audited
	static class EntityWithExcludedProperty2 {
		@Id
		long id;

		@Audited.Excluded
		String str1;
	}

	@Entity
	@Audited.Override(name = "str1", isAudited = true) // <-- revokes initial exclusion of str1
	static class EntityThatRevokes extends EntityWithExcludedProperty2{

	}

	@Test
	public void revocationInSubEntity(DomainModelScope domainModelScope) {
		var tables = domainModelScope.getDomainModel().collectTableMappings();
		assertTable( tables, "EntityWithExcludedProperty2_AUD", table -> {
			assertTrue( table.containsColumn( new Column( "str1" ) ) );
		} );
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
