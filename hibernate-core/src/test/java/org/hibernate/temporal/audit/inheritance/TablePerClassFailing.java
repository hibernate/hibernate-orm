/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.temporal.audit.inheritance;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Audited;
import org.hibernate.audit.AuditLog;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;


@SessionFactory
@DomainModel(annotatedClasses = {
		TablePerClassFailing.Base.class,
		TablePerClassFailing.Sub.class,
})
@ServiceRegistry(settings = @Setting(name = StateManagementSettings.CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.AuditEntityTest$TxIdSupplier"))
@Jira( "HHH-20857" )
public class TablePerClassFailing {
	private static int currentTxId;

	public static class TxIdSupplier implements ChangesetIdentifierSupplier<Integer> {
		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++currentTxId;
		}
	}

	@Entity(name = "Base")
	@Audited
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	static class Base {
		@Id
		long id;

		@Audited.Excluded
		String str1;

	}

	@Entity(name = "Sub")
	static class Sub extends Base {
		@Audited.Excluded
		String str2;
	}

	@Test
	public void test(DomainModelScope domainModelScope, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var b = new Base();
			b.id = 1;
			b.str1 = "v";
			session.persist( b );

			var s = new Sub();
			s.id = 2;
			s.str1 = "sub_str1";
			s.str2 = "sub_str2";
			session.persist( s );
		} );

		scope.inTransaction( s -> {
			var statelessSession = s.getSessionFactory().withStatelessOptions().atChangeset( AuditLog.ALL_CHANGESETS )
					.openStatelessSession();
			var fromBase = statelessSession.createSelectionQuery( "from Base", Base.class ).getResultList();
			for ( var base : fromBase ) {
				assertNull( base.str1 );
				if ( base instanceof Sub sub) {
					assertNull( sub.str2 );
				}
			}
		} );
	}

}
