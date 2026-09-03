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
import org.hibernate.audit.AuditLog;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.mapping.Column;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SessionFactory
@DomainModel(annotatedClasses = {
		SingleTableInheritanceTest.SingleInheritanceBase.class,
		SingleTableInheritanceTest.SingleInheritanceSub.class,
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

		String str2;

	}
	@MappedSuperclass
	@Audited.Overrides( @Audited.Override(name = "str1", isAudited = false) )
	static class LowerMSCThatExcludesTheProperty extends UpperMSCThatAuditsAProperty {


	}
	@Entity(name = "SingleInheritanceBase")
	@Table
	static class SingleInheritanceBase extends LowerMSCThatExcludesTheProperty {

	}
	@MappedSuperclass
	@Audited.Overrides(
			{@Audited.Override(name = "str1", isAudited = true)} // <-- revocation of str1
	)
	static class UpperSecondMSCThatRevokesTheExclusion extends SingleInheritanceBase {

	}
	@MappedSuperclass
	@Audited.Overrides(
			{@Audited.Override(name = "str2", isAudited = false)} // <-- exclusion of str2
	)

	static class LowerSecondMSCThatDoesNothing extends UpperSecondMSCThatRevokesTheExclusion{

	}
	@Entity(name = "SingleInheritanceSub")
	static class SingleInheritanceSub extends LowerSecondMSCThatDoesNothing {

	}
	@Test
	public void twoGroups(DomainModelScope domainModelScope, SessionFactoryScope scope) {
		var tables = domainModelScope.getDomainModel().collectTableMappings();
		assertTable( tables, "SingleInheritanceBase_AUD", table -> {
			assertTrue( table.containsColumn( new Column( "str1" ) ) );
			assertTrue( table.containsColumn( new Column( "str2" ) ) );
		} );
		scope.inTransaction( s -> {
			var baseEntity = new SingleInheritanceBase();
			baseEntity.id = 0;
			baseEntity.str1 = "v";
			baseEntity.str2 = "w";
			s.persist( baseEntity );

			var subEntity = new SingleInheritanceSub();
			subEntity.id = 1;
			subEntity.str1 = "v";
			subEntity.str2 = "w";
			s.persist( subEntity );
		} );

		scope.inTransaction( s -> {
			var statelessSession = s.getSessionFactory().withStatelessOptions().atChangeset( AuditLog.ALL_CHANGESETS )
					.openStatelessSession();
			var auditedBase = statelessSession.createSelectionQuery("from SingleInheritanceBase b where Type(b) = SingleInheritanceBase", SingleInheritanceBase.class).getSingleResult();
			assertNull( auditedBase.str1 );
			assertNotNull( auditedBase.str2 );

			var auditedSub = statelessSession.createSelectionQuery("from SingleInheritanceSub", SingleInheritanceSub.class).getSingleResult();
			assertNotNull( auditedSub.str1 );
			assertNull( auditedSub.str2 );
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
