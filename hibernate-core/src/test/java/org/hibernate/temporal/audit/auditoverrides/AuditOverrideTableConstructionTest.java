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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SessionFactory
@DomainModel(annotatedClasses = {
//		AuditOverrideTableConstructionTest.ExcludingInsideGroupEntity.class,
//		AuditOverrideTableConstructionTest.RevokingInsideGroupEntity.class,
//		AuditOverrideTableConstructionTest.EntityUnderTwoMSCes.class,
//		AuditOverrideTableConstructionTest.RootEntity.class,
//		AuditOverrideTableConstructionTest.SubClass.class,
		AuditOverrideTableConstructionTest.EntityUnderTwoMSCsThatHasAnAuditOverride.class,
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

	/**
	 * 2-Layered Audited Group which covers two cases:
	 * 1)
	 * MappedSuperClass which declares a property str1
	 * Entity which @AuditOverride str1 = false the property
	 * Expected: str1 is effectively excluded from the AUD table
	 *
	 * 2)
	 * MappedSuperClass which declares an @Audited.Excluded property str2
	 * Entity which does not @AuditOverride str2
	 * Expected: str2 is effectively excluded from the AUD table
	 */
	@MappedSuperclass
	static class WTF {
		@Id
		long id;

		String str1;

		@Audited.Excluded
		String str2;

	}


	@Entity
	@Audited
	@Table(name = "ExcludingInsideGroupEntity")
	@AuditOverrides( @AuditOverride(name = "str1", isAudited = false) )
	static class ExcludingInsideGroupEntity extends WTF{

	}

	@Test
	public void basicGroup(DomainModelScope domainModelScope) {
		var tables = domainModelScope.getDomainModel().collectTableMappings();
		assertTable( tables, "ExcludingInsideGroupEntity_AUD", table -> {
			assertFalse( table.containsColumn( new Column( "str1" ) ) );
			assertFalse( table.containsColumn( new Column( "str2" ) ) );
		} );
	}

	/**
	 * Case 3)
	 * MappedSuperClass which declares an @Audited.Excluded property str2
	 * Entity which does @AuditOverride str2 = true ==> Revocation!
	 * Expected: str2 is included in the AUD table
	 */

	@Entity
	@Audited
	@Table(name = "RevokingInsideGroupEntity")
	@AuditOverride(name = "str2", isAudited = true)
	static class RevokingInsideGroupEntity extends WTF{

	}

	@Test
	public void basicGroup2(DomainModelScope domainModelScope) {
		var tables = domainModelScope.getDomainModel().collectTableMappings();
		assertTable( tables, "RevokingInsideGroupEntity_AUD", table -> {
			assertTrue( table.containsColumn( new Column( "str1" ) ) );
			assertTrue( table.containsColumn( new Column( "str2" ) ) );
		} );
	}




	@MappedSuperclass
	@Audited
	static class AuditedMSC {
		@Id
		long id;

		String str1;
	}

	/**
	 * 3-Layered Group Test: str1 is excluded via an @AuditOverride on the Entity,
	 * 	but with an additional @MappedSuperClass (AuditedMSCChild) in between
	 */

	@MappedSuperclass
	static class AuditedMSCChild extends AuditedMSC {
		String str2;
	}

	@Entity
	@AuditOverride(name = "str1", isAudited = false)
	@Table(name = "ClassUnderTwoMSCes")
	static class EntityUnderTwoMSCes extends AuditedMSCChild {
	}

	@Test
	public void entityUnderTwoMSCes(DomainModelScope domainModelScope) {
		var tables = domainModelScope.getDomainModel().collectTableMappings();
		assertTable( tables, "ClassUnderTwoMSCes_AUD", table -> {
			assertFalse( table.containsColumn( new Column( "str1" ) ) );
			assertTrue( table.containsColumn( new Column( "str2" ) ) );
		} );
	}

	/**
	 * 3-Layered Group Test:Group Test: str1 is excluded via an @AuditOverride on a @MappedSuperclass of Entity,
	 * MSC @Audit str1
	 * MSC @AuditOverride false
	 * Entity
	 */

	@MappedSuperclass
	@AuditOverrides(@AuditOverride(name = "str1", isAudited = false))
	static class MSCChildWithDisablingAuditOverride extends AuditedMSC {
		String str2;
	}

	@Entity
	@Table(name = "MSCCWDAO")
	static class EntityUnderTwoMSCsThatHasAnAuditOverride extends MSCChildWithDisablingAuditOverride {
	}

	@Test
	public void entityUnderTwoMSCsThatHasAnAuditOverride(DomainModelScope domainModelScope) {
		var tables = domainModelScope.getDomainModel().collectTableMappings();
		assertTable( tables, "MSCCWDAO_AUD", table -> {
			assertFalse( table.containsColumn( new Column( "str1" ) ) );
			assertTrue( table.containsColumn( new Column( "str2" ) ) );
		} );
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
	 * Transitive Exclusion: str1 is excluded somewhere in the hierarchy, but the exclusion does not become effective,
	 * because another layer (RootEntity) includes the property into the AUD table
	 */


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

	/**
	 * Revocation:
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

	/**
	 * Special case: Ordering
	 * Actually, due to do the revocation of RevokingEntity2, str1 should be contained
	 * inside the AUD table. But because RevokingEntity2 is processed first ( revocation = noop)
	 * and DeclaringAndExcludingSubClass is afterward, the exclusion becomes mistakenly effective
	 */

	@Audited
	@Entity
	@Table(name = "EmptyAuditedRootEntity")
	static class EmptyAuditedRootEntity {
		@Id
		long id;
	}

	@Entity
	static class DeclaringAndExcludingSubClass extends EmptyAuditedRootEntity {
		@Audited.Excluded
		String str1;
	}

	@Entity
	@AuditOverride(name = "str1", isAudited = true) //revokes exclusion
	static class RevokingEntity2 extends DeclaringAndExcludingSubClass {
	}

	@Test
	public void revokingEntity2(DomainModelScope domainModelScope) {
		var tables = domainModelScope.getDomainModel().collectTableMappings();
		assertTable( tables, "EmptyAuditedRootEntity_AUD", table -> {
			assertTrue( table.containsColumn( new Column( "str1" ) ) );
		} );
	}

	/**
	 * Bug reproducer
	 */


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
	@Disabled(
			value = "AUD tables are missing completely, due to a bug: https://github.com/hibernate/hibernate-orm/pull/13047")
	public void auditedSubEntity(DomainModelScope domainModelScope) {
		var tables = domainModelScope.getDomainModel().collectTableMappings();
		assertTable( tables, "NotAuditedRootEntity_AUD", table -> {
			assertTrue( table.containsColumn( new Column( "str1" ) ) );
			assertTrue( table.containsColumn( new Column( "str2" ) ) );
		} );
	}

	// Parent @Entity has an Audit Exclude, will we allow enabling it?
	//TODO for collections and @Embedded?
	//TODO Hierarchy under RootClass is analyzed. But there might be additional hierarchies under another MSC on top of the Root Class

}

//@AuditedTest
