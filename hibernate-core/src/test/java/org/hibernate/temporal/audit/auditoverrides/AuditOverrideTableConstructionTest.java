/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.temporal.audit.auditoverrides;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


//@AuditedTest
@SessionFactory
@DomainModel(annotatedClasses = {
//		AuditOverrideTableConstructionTest.RootEntity.class,
//		AuditOverrideTableConstructionTest.SubClass.class,
//		AuditOverrideTableConstructionTest.MSC.class,
//		AuditOverrideTableConstructionTest.SubClassOfMSC.class,
//		AuditOverrideTableConstructionTest.ClassUnderTwoMSCes.class,
//		AuditOverrideTableConstructionTest.MSCWithoutAuditing.class,
//		AuditOverrideTableConstructionTest.EnablesAuditingOfMSC.class,
		AuditOverrideTableConstructionTest.NotAuditedRootEntity.class,
		AuditOverrideTableConstructionTest.AuditedSubEntity.class,

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

	@Audited
	@Entity
	@Table(name = "RootEntity")
	static class RootEntity {
		@Id
		long id;

		String str1;
	}

	@Entity
	@AuditOverride(name = "str1", isAudited = false) //TODO with AuditOverrideS aswell
	static class SubClass extends RootEntity {
		String str2;
	}

	@Test
	public void entityInheritance(DomainModelScope domainModelScope) {
		var tables = domainModelScope.getDomainModel().collectTableMappings();
		assertTable( tables, "RootEntity_AUD", table -> {
			assertTrue( table.containsColumn( new Column( "str1" ) ) );
			assertTrue( table.containsColumn( new Column( "str2" ) ) );
		} );
	}

	@MappedSuperclass
	@Audited
	static class MSC {
		@Id
		long id;

		String str1;
	}

	@Entity
	@AuditOverride(name = "str1", isAudited = false)
	@Table(name = "SubClassOfMSC")
	static class SubClassOfMSC extends MSC {
		String str2;
	}

	@Test
	public void mappedSuperclassInheritance(DomainModelScope domainModelScope) {
		var tables = domainModelScope.getDomainModel().collectTableMappings();
		assertTable( tables, "SubClassOfMSC_AUD", table -> {
			assertFalse( table.containsColumn( new Column( "str1" ) ) );
			assertTrue( table.containsColumn( new Column( "str2" ) ) );
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


	@MappedSuperclass
	static class MSCChild extends MSC {
		String str2;
	}

	@Entity
	@AuditOverride(name = "str1", isAudited = false)
	@Table(name = "ClassUnderTwoMSCes")
	static class ClassUnderTwoMSCes extends MSCChild {
		String str3;
	}

	@Test
	public void underTwoMSCs(DomainModelScope domainModelScope) {
		var tables = domainModelScope.getDomainModel().collectTableMappings();
		assertTable( tables, "ClassUnderTwoMSCes_AUD", table -> {
			assertFalse( table.containsColumn( new Column( "str1" ) ) );
			assertTrue( table.containsColumn( new Column( "str2" ) ) );
			assertTrue( table.containsColumn( new Column( "str3" ) ) );
		} );
	}

	@MappedSuperclass
	static class MSCWithoutAuditing {
		@Id
		long id;

		String str1;
	}

	@Entity
	@Table(name = "EnablesAuditingOfMSC")
	@AuditOverride(name = "str1", isAudited = true)
	//TODO add an exception when there is an @AuditOverride enabling auditing on a @MappedSuperClass that isn't audited? But what if it was inherited?
	@Audited
	static class EnablesAuditingOfMSC extends MSCWithoutAuditing {

	}

	@Test
	public void enableAuditingOfPropertyInheritedFromAMSC(DomainModelScope domainModelScope) {
		var tables = domainModelScope.getDomainModel().collectTableMappings();
		assertTable( tables, "EnablesAuditingOfMSC_AUD", table -> {
			assertTrue( table.containsColumn( new Column( "str1" ) ) );
		} );
	}

	@Entity
	@Table(name = "NotAuditedRootEntity")
	static class NotAuditedRootEntity {
		@Id
		long id;

		String str1;
	}

	@Entity
	@Audited
	@AuditOverride(name = "str1")
	static class AuditedSubEntity extends NotAuditedRootEntity {
		String str2;
	}

	@Test
	@Disabled(value = "AUD tables are missing completely, due to a bug: https://github.com/hibernate/hibernate-orm/pull/13047")
	public void auditedSubEntity(DomainModelScope domainModelScope) {
		var tables = domainModelScope.getDomainModel().collectTableMappings();
		assertTable( tables, "NotAuditedRootEntity_AUD", table -> {
			assertTrue( table.containsColumn( new Column( "str1" ) ) );
			assertTrue( table.containsColumn( new Column( "str2" ) ) );
		} );
	}


	//
	//
	// Parent @Entity has an Audit Exclude, will we allow enabling it?

}
