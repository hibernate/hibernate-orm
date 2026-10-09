/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.temporal.audit.auditoverrides.inheritance;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SessionFactory
@DomainModel(annotatedClasses = {
		SingleTableInheritanceTest.Base.class,
		SingleTableInheritanceTest.Sub.class,
		SingleTableInheritanceTest.SubSub.class,
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

	@Entity(name = "Base")
	@Table(name = "Base")
	@Audited
	static class Base {
		@Id
		long id;
		@Audited.Excluded
		String str1;

		String str2;

	}
	@Entity(name = "Sub")
	@Audited.Overrides( {
			@Audited.Override(name = "str1", isAudited = true), // <-- revokes initial exclusion of str1
			@Audited.Override(name = "str2", isAudited = false) // <-- revokes initial inclusion of str2
	} )
	static class Sub extends Base {
		@Audited.Excluded
		String str3;
	}

	@Entity(name = "SubSub")
	@Audited.Overrides( {
			@Audited.Override(name = "str3", isAudited = true),
	} )
	static class SubSub extends Sub {
	}

	@Test
	public void twoGroups(DomainModelScope domainModelScope, SessionFactoryScope scope) {
		var tables = domainModelScope.getDomainModel().collectTableMappings();
		assertTable( tables, "Base_AUD", table -> {
			assertTrue( table.containsColumn( new Column( "str1" ) ) );
			assertTrue( table.containsColumn( new Column( "str2" ) ) );
		} );
		scope.inTransaction( s -> {
			var baseEntity = new Base();
			baseEntity.id = 0;
			baseEntity.str1 = "v";
			baseEntity.str2 = "w";
			s.persist( baseEntity );

			var subEntity = new Sub();
			subEntity.id = 1;
			subEntity.str1 = "v";
			subEntity.str2 = "w";
			s.persist( subEntity );

			var subSubEntity = new SubSub();
			subSubEntity.id = 2;
			subSubEntity.str1 = "v";
			subSubEntity.str2 = "w";
			subSubEntity.str3 = "x";
			s.persist( subSubEntity );
		} );

		scope.inTransaction( s -> {
			var statelessSession = s.getSessionFactory().withStatelessOptions().atChangeset( AuditLog.ALL_CHANGESETS )
					.openStatelessSession();
			var auditedBase = statelessSession.createSelectionQuery("from Base b where Type(b) = Base", Base.class).getSingleResult();
			assertNull( auditedBase.str1 );
			assertNotNull( auditedBase.str2 );

			var auditedSub = statelessSession.createSelectionQuery("from Sub s where Type(s) = Sub", Sub.class).getSingleResult();
			assertNotNull( auditedSub.str1 );
			assertNull( auditedSub.str2 );
			assertNull( auditedSub.str3 );

			var auditedSubSub = statelessSession.createSelectionQuery("from SubSub s where Type(s) = SubSub", SubSub.class).getSingleResult();
			assertNotNull( auditedSubSub.str1 );
			assertNull( auditedSubSub.str2 );
			assertNotNull( auditedSubSub.str3 );
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
