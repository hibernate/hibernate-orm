/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.temporal.audit.auditoverrides;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
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
		AuditOverrideTableConstructionWithMappedSuperClassesTest.EntityThatRevokesTheProperty.class,
		AuditOverrideTableConstructionWithMappedSuperClassesTest.EntityThatExcludesTheProperty.class,
		AuditOverrideTableConstructionWithMappedSuperClassesTest.EntityThatRevokesTheProperty2.class,
		AuditOverrideTableConstructionWithMappedSuperClassesTest.EntityThatRevokesTheProperty3.class,
		AuditOverrideTableConstructionWithMappedSuperClassesTest.EntityThatRevokesTheProperty4.class,
		AuditOverrideTableConstructionWithMappedSuperClassesTest.EntityThatInherits.class,
		AuditOverrideTableConstructionWithMappedSuperClassesTest.EntityThatInheritsAnExcludedProperty.class,
		AuditOverrideTableConstructionWithMappedSuperClassesTest.EntityThatInheritsTheRevokedProperty.class,
		AuditOverrideTableConstructionWithMappedSuperClassesTest.EntityWithExcludedProperty.class,
		AuditOverrideTableConstructionWithMappedSuperClassesTest.EntityThatOverridesTheProperty3.class,
})
@ServiceRegistry(settings = @Setting(name = StateManagementSettings.CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.AuditEntityTest$TxIdSupplier"))
public class AuditOverrideTableConstructionWithMappedSuperClassesTest {
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
	 * Entity: @Audited
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
	 * MSC: @Audited
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
	 * Case 4: Two Groups in a hierarchy
	 * MSC: @Audited.Excluded
	 * MSC: @Audited.Override.isAudited = false
	 * Entity: -
	 *
	 * MSC: @Audited
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
	 * Case 5:
	 * MSC: @Audited.Excluded, but with a property that has multiple columns: an @Embeddable
	 * Entity: -
	 * Entity: @Audited
	 *
	 */

	@MappedSuperclass
	@Audited
	static class MSC3 {
		@Id
		long id;

		@Audited.Excluded
		@Embedded
		ContactPerson contactPerson;
	}

	@Entity
	@Table(name = "EntityThatRevokesTheProperty3")
	static class EntityThatRevokesTheProperty3 extends MSC3{

	}

	@Embeddable
	public static class ContactPerson {
		String firstName;
		String lastName;
	}

	@Entity
	@Audited.Override(name = "contactPerson", isAudited = true)
	static class EntityThatRevokesTheProperty4 extends EntityThatRevokesTheProperty3 {
	}

	@Test
	public void withEmbeddable(DomainModelScope domainModelScope) {
		var tables = domainModelScope.getDomainModel().collectTableMappings();
		assertTable( tables, "EntityThatRevokesTheProperty3_AUD", table -> {
			assertTrue( table.containsColumn( new Column( "firstName" ) ) );
			assertTrue( table.containsColumn( new Column( "lastName" ) ) );
		} );
	}

	/**
	 * Case 6:
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

}
