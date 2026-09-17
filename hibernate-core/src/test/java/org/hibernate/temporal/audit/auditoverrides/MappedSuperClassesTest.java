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

import static org.hibernate.temporal.audit.auditoverrides.inheritance.SingleTableInheritanceTest.assertTable;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SessionFactory
@DomainModel(annotatedClasses = {
		MappedSuperClassesTest.EntityThatRevokesTheProperty.class,
		MappedSuperClassesTest.EntityThatExcludesTheProperty.class,
		MappedSuperClassesTest.EntityThatRevokesTheProperty2.class,
		MappedSuperClassesTest.EntityThatInherits.class,
})
@ServiceRegistry(settings = @Setting(name = StateManagementSettings.CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.AuditEntityTest$TxIdSupplier"))
public class MappedSuperClassesTest {
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
	 * Entity: @Audited.Override.isAudited = true
	 *
	 */
	@MappedSuperclass
	@Audited
	static class MSCWithoutProperties {
		@Id
		long id;
	}

	@MappedSuperclass
	static class MSCWithExcludedProperty extends MSCWithoutProperties {
		@Audited.Excluded
		String str1;
	}

	@Entity
	@Table(name = "EntityThatRevokesTheProperty")
	@Audited.Override(name = "str1", isAudited = true)
	static class EntityThatRevokesTheProperty extends MSCWithExcludedProperty {
	}

	@Test
	public void entityUnderTwoMSCes(DomainModelScope domainModelScope) {
		var tables = domainModelScope.getDomainModel().collectTableMappings();
		assertTable( tables, "EntityThatRevokesTheProperty_AUD", table -> {
			assertTrue( table.containsColumn( new Column( "str1" ) ) );
		} );
	}

	/**
	 * Case 1.1:
	 * MSC: @Audited
	 * Entity: @Audited.Override.isAudited = false
	 *
	 */

	@MappedSuperclass
	@Audited
	static class MSCWithProperty {
		@Id
		long id;

		String str1;
	}

	@Entity
	@Table(name = "EntityThatExcludesTheProperty")
	@Audited.Override(name = "str1", isAudited = false)
	static class EntityThatExcludesTheProperty extends MSCWithProperty {
	}

	@Test
	public void test2(DomainModelScope domainModelScope) {
		var tables = domainModelScope.getDomainModel().collectTableMappings();
		assertTable( tables, "EntityThatExcludesTheProperty_AUD", table -> {
			assertFalse( table.containsColumn( new Column( "str1" ) ) );
		} );
	}


	/**
	 * Case 2:
	 * MSC: @Audited.Excluded
	 * MSC: -
	 * Entity: @Audited.Override.isAudited = true
	 *
	 */
	@MappedSuperclass
	@Audited
	static class RootMSCWithExcludedProperty {
		@Id
		long id;

		@Audited.Excluded
		String str1;
	}

	@MappedSuperclass
	static class IntermediateMSCWithoutAnything extends RootMSCWithExcludedProperty {

	}

	@Entity
	@Table(name = "EntityThatRevokesTheProperty2")
	@Audited.Override(name = "str1", isAudited = true)
	static class EntityThatRevokesTheProperty2 extends IntermediateMSCWithoutAnything {
	}

	@Test
	public void entityUnderTwoMSCes2(DomainModelScope domainModelScope) {
		var tables = domainModelScope.getDomainModel().collectTableMappings();
		assertTable( tables, "EntityThatRevokesTheProperty2_AUD", table -> {
			assertTrue( table.containsColumn( new Column( "str1" ) ) );
		} );
	}

	/**
	 * Case 3:
	 * MSC: @Audited.Excluded
	 * MSC: @Audited.Override.isAudited = true
	 * Entity: -
	 *
	 */


	@MappedSuperclass
	@Audited.Override(name = "str1", isAudited = true)
	static class IntermediateMSCWithAuditOverrideTrue extends MSCWithExcludedProperty {

	}

	@Entity
	@Table(name = "EntityThatInherits")
	static class EntityThatInherits extends IntermediateMSCWithAuditOverrideTrue {
	}

	@Test
	public void entityUnderTwoMSCes3(DomainModelScope domainModelScope) {
		var tables = domainModelScope.getDomainModel().collectTableMappings();
		assertTable( tables, "EntityThatInherits_AUD", table -> {
			assertTrue( table.containsColumn( new Column( "str1" ) ) );
		} );
	}

}
