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

import static org.hibernate.temporal.audit.auditoverrides.inheritance.SingleTableInheritanceTest.assertTable;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SessionFactory
@DomainModel(annotatedClasses = {
		EmbeddableTest.EntityThatRevokesTheProperty3.class,
		EmbeddableTest.EntityThatRevokesTheProperty4.class,
})
@ServiceRegistry(settings = @Setting(name = StateManagementSettings.CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.AuditEntityTest$TxIdSupplier"))
public class EmbeddableTest {
	private static int currentTxId;

	public static class TxIdSupplier implements ChangesetIdentifierSupplier<Integer> {
		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++currentTxId;
		}
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
	public void test(DomainModelScope domainModelScope) {
		var tables = domainModelScope.getDomainModel().collectTableMappings();
		assertTable( tables, "EntityThatRevokesTheProperty3_AUD", table -> {
			assertTrue( table.containsColumn( new Column( "firstName" ) ) );
			assertTrue( table.containsColumn( new Column( "lastName" ) ) );
		} );
	}

}
