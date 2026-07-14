/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.temporal.audit.auditoverrides;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.AuditOverride;
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
//		AuditOverrideTableConstructionTest.ExcludingInsideGroupEntity.class,
//		AuditOverrideTableConstructionTest.RevokingInsideGroupEntity.class,
//		AuditOverrideTableConstructionTest.EntityUnderTwoMSCes.class,
//		AuditOverrideTableConstructionTest.RootEntity.class,
//		AuditOverrideTableConstructionTest.SubClass.class,
//		AuditOverrideTableConstructionTest.NotAuditedRootEntity.class,
//		AuditOverrideTableConstructionTest.AuditedSubEntity.class,
//		AuditOverrideTableConstructionTest.ExcludingEntity.class,
//		AuditOverrideTableConstructionTest.RevokingEntity.class,
//		AuditOverrideTableConstructionTest.EmptyAuditedRootEntity.class,
//		AuditOverrideTableConstructionTest.DeclaringAndExcludingSubClass.class,
//		AuditOverrideTableConstructionTest.RevokingEntity2.class,
})
@ServiceRegistry(settings = @Setting(name = StateManagementSettings.CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.AuditEntityTest$TxIdSupplier"))
public class AuditOverrideTableConstructionTest {
	private static int currentTxId;

	public static class TxIdSupplier implements ChangesetIdentifierSupplier<Integer> {
		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++currentTxId;
		}
	}


	//TODO add Non-Effective exclusions within a group in order to prove that inter-group exclusions are calculated correctly

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

	/**
	 * Revocation over multiple groups
	 * ExcludingEntity declares and excludes str1 from auditing
	 * RevokingEntity revokes ExcludingEntity's exclusion
	 */


	@Audited
	@Entity
	@AuditOverride(name = "str2", isAudited = false)
	@Table(name = "ExcludingEntity")
	static class ExcludingEntity {
		@Id
		long id;

		@Audited.Excluded
		String str2;
		String str1;

	}

	@Entity
	@AuditOverride(name = "str2", isAudited = true) //revokes exclusion
	static class RevokingEntity extends ExcludingEntity {

	}

	@Test
	public void revokingEntity(DomainModelScope domainModelScope) {
		var tables = domainModelScope.getDomainModel().collectTableMappings();
		assertTable( tables, "ExcludingEntity_AUD", table -> {
			assertTrue( table.containsColumn( new Column( "str1" ) ) );
			assertTrue( table.containsColumn( new Column( "str2" ) ) );
		} );
	}


	// Parent @Entity has an Audit Exclude, will we allow enabling it?
	//TODO for collections and @Embedded?
	//TODO Hierarchy under RootClass is analyzed. But there might be additional hierarchies under another MSC on top of the Root Class

}

//@AuditedTest
