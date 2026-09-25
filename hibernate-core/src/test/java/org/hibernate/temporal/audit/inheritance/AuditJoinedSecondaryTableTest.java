/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.temporal.audit.inheritance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.SecondaryTable;
import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Audited;
import org.hibernate.audit.AuditLog;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.testing.orm.junit.AfterClassTemplate;
import org.hibernate.testing.orm.junit.AuditedTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@AuditedTest
@SessionFactory
@DomainModel(annotatedClasses = {
		AuditJoinedSecondaryTableTest.JoinedBase.class,
		AuditJoinedSecondaryTableTest.JoinedLeaf.class
})
@ServiceRegistry(settings = @Setting(name = StateManagementSettings.CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.inheritance.AuditJoinedSecondaryTableTest$TxIdSupplier"))
@Jira("https://hibernate.atlassian.net/browse/HHH-20906")
class AuditJoinedSecondaryTableTest {
	private static int currentTxId;

	public static class TxIdSupplier implements ChangesetIdentifierSupplier<Integer> {
		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++currentTxId;
		}
	}

	@BeforeClassTemplate
	void createTestData(SessionFactoryScope scope) {
		currentTxId = 0;
		scope.inTransaction( session -> {
			var entity = new JoinedLeaf();
			entity.id = 1L;
			entity.secondaryProperty = "secondary value";
			entity.leafProperty = "leaf value";
			session.persist( entity );
		} );
	}

	@AfterClassTemplate
	void dropData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void testRootSelection(SessionFactoryScope scope) {
		try (var session = scope.getSessionFactory().withStatelessOptions()
				.atChangeset( AuditLog.ALL_CHANGESETS ).openStatelessSession()) {
			var entity = session.createSelectionQuery( "from JoinedBase", JoinedBase.class )
					.getSingleResult();
			assertThat( entity.secondaryProperty ).isEqualTo( "secondary value" );
		}
	}

	@Test
	void testSubclassSelection(SessionFactoryScope scope) {
		try (var session = scope.getSessionFactory().withOptions()
				.atChangeset( AuditLog.ALL_CHANGESETS ).openSession()) {
			var entity = session.createSelectionQuery( "from JoinedLeaf", JoinedLeaf.class )
					.getSingleResult();
			assertThat( entity.secondaryProperty ).isEqualTo( "secondary value" );
			assertThat( entity.leafProperty ).isEqualTo( "leaf value" );
		}
	}

	@Entity(name = "JoinedBase")
	@Audited
	@Inheritance(strategy = InheritanceType.JOINED)
	@SecondaryTable(name = "joined_base_details")
	@Audited.SecondaryTable(
			secondaryTableName = "joined_base_details",
			secondaryAuditTableName = "joined_base_details_history"
	)
	static class JoinedBase {
		@Id
		long id;

		@Column(table = "joined_base_details")
		String secondaryProperty;
	}

	@Entity(name = "JoinedLeaf")
	static class JoinedLeaf extends JoinedBase {
		String leafProperty;
	}
}
