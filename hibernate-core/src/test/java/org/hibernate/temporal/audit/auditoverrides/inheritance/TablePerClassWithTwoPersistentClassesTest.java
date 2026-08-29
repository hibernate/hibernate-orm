/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.temporal.audit.auditoverrides.inheritance;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
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
		TablePerClassWithTwoPersistentClassesTest.Base.class,
		TablePerClassWithTwoPersistentClassesTest.Sub.class,
		TablePerClassWithTwoPersistentClassesTest.SubSub.class,
})
@ServiceRegistry(settings = @Setting(name = StateManagementSettings.CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.AuditEntityTest$TxIdSupplier"))
public class TablePerClassWithTwoPersistentClassesTest {
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
	@Table(name = "Base")
	@Audited
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	static class Base {
		@Id
		long id;

		@Audited.Excluded
		String str1;

		String str2;
	}

	@Entity
	@Audited.Overrides( {
			@Audited.Override(name = "str1", isAudited = true), // <-- revokes initial exclusion of str1
			@Audited.Override(name = "str2", isAudited = false) // <-- revokes initial inclusion of str2
	} )
	static class Sub extends Base {
		@Audited.Excluded
		String str3;
	}

	@Entity
	@Audited.Overrides( {
			@Audited.Override(name = "str3", isAudited = true),
	} )
	static class SubSub extends Sub {
	}

	@Test
	public void test(DomainModelScope domainModelScope) {
		var tables = domainModelScope.getDomainModel().collectTableMappings();
		assertTable( tables, "Base_AUD", table -> {
			assertFalse( table.containsColumn( new Column( "str1" ) ) );
			assertTrue( table.containsColumn( new Column( "str2" ) ) );
		} );

		assertTable( tables, "TablePerClassWithTwoPersistentClassesTest$Sub_AUD", table -> {
			assertTrue( table.containsColumn( new Column( "str1" ) ) );
			assertFalse( table.containsColumn( new Column( "str2" ) ) );
			assertFalse( table.containsColumn( new Column( "str3" ) ) );
		} );

		assertTable( tables, "TablePerClassWithTwoPersistentClassesTest$SubSub_AUD", table -> {
			assertTrue( table.containsColumn( new Column( "str1" ) ) );
			assertFalse( table.containsColumn( new Column( "str2" ) ) );
			assertTrue( table.containsColumn( new Column( "str3" ) ) );
		} );

	}

}
