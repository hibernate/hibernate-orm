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

import static org.hibernate.temporal.audit.auditoverrides.inheritance.SingleTableInheritanceTest.assertTable;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SessionFactory
@DomainModel(annotatedClasses = {
		JoinedInheritanceWithTwoPersistentClassesTest.Base.class,
		JoinedInheritanceWithTwoPersistentClassesTest.Sub.class,
		JoinedInheritanceWithTwoPersistentClassesTest.SubSub.class,
})
@ServiceRegistry(settings = @Setting(name = StateManagementSettings.CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.AuditEntityTest$TxIdSupplier"))
public class JoinedInheritanceWithTwoPersistentClassesTest {
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
	@Inheritance(strategy = InheritanceType.JOINED)
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
			@Audited.Override(name = "str2", isAudited = false)
	} )
	static class Sub extends Base {
		@Audited.Excluded
		String str3;
	}

	@Entity(name = "SubSub")
	@Audited.Overrides( {
			@Audited.Override(name = "str3", isAudited = true), // <-- revokes initial exclusion of str3
	} )
	static class SubSub extends Sub {
	}

	@Test
	public void test(DomainModelScope domainModelScope, SessionFactoryScope scope) {
		var tables = domainModelScope.getDomainModel().collectTableMappings();
		assertTable( tables, "Base_AUD", table -> {
			assertTrue( table.containsColumn( new Column( "str1" ) ) );
			assertTrue( table.containsColumn( new Column( "str2" ) ) );
		} );

		assertTable( tables, "Sub_AUD", table -> {
			assertTrue( table.containsColumn( new Column( "str3" ) ) );
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

			var subsub = new SubSub();
			subsub.id = 2;
			subsub.str1 = "v";
			subsub.str2 = "w";
			subsub.str3 = "x";
			s.persist( subsub );
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

			var auditedSubSub = statelessSession.createSelectionQuery("from SubSub", SubSub.class).getSingleResult();
			assertNotNull( auditedSubSub.str1 );
			assertNull( auditedSubSub.str2 );
			assertNotNull( auditedSubSub.str3 );
		} );
	}

}
