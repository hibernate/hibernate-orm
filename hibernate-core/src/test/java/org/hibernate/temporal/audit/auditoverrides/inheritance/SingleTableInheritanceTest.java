/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.temporal.audit.auditoverrides.inheritance;

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

import java.util.Collection;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SessionFactory
@DomainModel(annotatedClasses = {
		SingleTableInheritanceTest.EntityThatInheritsAnExcludedProperty.class,
		SingleTableInheritanceTest.EntityThatInheritsTheRevokedProperty.class,
		SingleTableInheritanceTest.EntityWithExcludedProperty.class,
		SingleTableInheritanceTest.EntityThatOverridesTheProperty3.class,
})
@ServiceRegistry(settings = @Setting(name = StateManagementSettings.CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.AuditEntityTest$TxIdSupplier"))
public class SingleTableInheritanceTest {
	private static int currentTxId;

	public static class TxIdSupplier implements ChangesetIdentifierSupplier<Integer> {
		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++currentTxId;
		}
	}

	/**
	 * MSC: @Audited.Excluded
	 * MSC: @Audited.Override.isAudited = false
	 * Entity: -
	 *
	 * MSC: @Audited.Override.isAudited = true
	 * MSC: -
	 * Entity: -
	 *
	 */

	@MappedSuperclass
	@Audited
	static class UpperMSCThatAuditsAProperty {

		@Id
		long id;
		@Audited.Excluded
		String str1;

	}
	@MappedSuperclass
	@Audited.Overrides( @Audited.Override(name = "str1", isAudited = false) )
	static class LowerMSCThatExcludesTheProperty extends UpperMSCThatAuditsAProperty {


	}
	@Entity
	@Table(name = "EntityThatInheritsAnExcludedProperty")
	static class EntityThatInheritsAnExcludedProperty extends LowerMSCThatExcludesTheProperty {

	}
	@MappedSuperclass
	@Audited.Overrides(@Audited.Override(name = "str1", isAudited = true)) // <-- revocation of str1
	static class UpperSecondMSCThatRevokesTheExclusion extends EntityThatInheritsAnExcludedProperty {

	}
	@MappedSuperclass
	static class LowerSecondMSCThatDoesNothing extends UpperSecondMSCThatRevokesTheExclusion{

	}
	@Entity
	static class EntityThatInheritsTheRevokedProperty extends LowerSecondMSCThatDoesNothing {

	}
	@Test
	public void twoGroups(DomainModelScope domainModelScope) {
		var tables = domainModelScope.getDomainModel().collectTableMappings();
		assertTable( tables, "EntityThatInheritsAnExcludedProperty_AUD", table -> {
			assertTrue( table.containsColumn( new Column( "str1" ) ) );
		} );
	}

	/**
	 * Entity: @Audited.Excluded
	 * MSC: @Audited.Override.isAudited = true
	 * Entity: @Audited.Override.isAudited = false
	 *
	 */
	@Entity
	@Table(name = "EntityWithExcludedProperty")
	@Audited
	static class EntityWithExcludedProperty {

		@Id
		long id;
		@Audited.Excluded
		String str1;

	}
	@MappedSuperclass
	@Audited.Override(name = "str1", isAudited = true)
	static class MSC5 extends EntityWithExcludedProperty{

	}
	@Entity
	@Audited.Override(name = "str1", isAudited = false)
	static class EntityThatOverridesTheProperty3 extends MSC5{


	}
	@Test
	public void entityUnderTwoMSCes5(DomainModelScope domainModelScope) {
		var tables = domainModelScope.getDomainModel().collectTableMappings();
		assertTable( tables, "EntityWithExcludedProperty_AUD", table -> {
			assertFalse( table.containsColumn( new Column( "str1" ) ) );
		} );
	}


	public static void assertTable(Collection<org.hibernate.mapping.Table> tables, String tableName, Consumer<org.hibernate.mapping.Table> consumer) {
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
