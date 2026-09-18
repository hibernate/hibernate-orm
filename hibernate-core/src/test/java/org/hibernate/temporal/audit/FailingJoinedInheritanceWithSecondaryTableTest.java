/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.temporal.audit;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Audited;
import org.hibernate.audit.AuditLog;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;


@SessionFactory
@DomainModel(annotatedClasses = {
		FailingJoinedInheritanceWithSecondaryTableTest.Base.class,
		FailingJoinedInheritanceWithSecondaryTableTest.Sub.class,
})
@ServiceRegistry(settings = @Setting(name = StateManagementSettings.CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.AuditEntityTest$TxIdSupplier"))
public class FailingJoinedInheritanceWithSecondaryTableTest {
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
	@SecondaryTable(
			name = "secondary_table",
			pkJoinColumns = @PrimaryKeyJoinColumn(name = "base_id")
	)
	static class Base {
		@Id
		long id;

		@jakarta.persistence.Column(name = "str2", table = "secondary_table")
		String str2;
	}

	@Entity(name = "Sub")
	static class Sub extends Base {
	}

	@Test
	public void test(DomainModelScope domainModelScope, SessionFactoryScope scope) {
		var tables = domainModelScope.getDomainModel().collectTableMappings();
//		assertTable( tables, "secondary_table_AUD", table -> {
//			assertTrue( table.containsColumn( new Column( "str2" ) ) );
//		} );

		scope.inTransaction( s -> {
			var baseEntity = new Base();
			baseEntity.id = 0;
			baseEntity.str2 = "w";
			s.persist( baseEntity );
		} );

		scope.inTransaction( s -> {
			var statelessSession = s.getSessionFactory().withStatelessOptions().atChangeset( AuditLog.ALL_CHANGESETS )
					.openStatelessSession();
			var auditedBase = statelessSession.createSelectionQuery("from Base", Base.class).getSingleResult();
			assertNotNull( auditedBase.str2 );
		} );
	}

}
