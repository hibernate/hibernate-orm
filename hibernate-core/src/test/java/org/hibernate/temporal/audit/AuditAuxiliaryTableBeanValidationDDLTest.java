/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.temporal.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SecondaryTable;
import jakarta.validation.constraints.NotNull;

import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Audited;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.cfg.ValidationSettings;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SessionFactory
@DomainModel(annotatedClasses = {
		AuditAuxiliaryTableBeanValidationDDLTest.Parent.class,
		AuditAuxiliaryTableBeanValidationDDLTest.AuditedEntity.class
})
@ServiceRegistry(settings = {
		@Setting(name = StateManagementSettings.CHANGESET_ID_SUPPLIER,
				value = "org.hibernate.temporal.audit.AuditAuxiliaryTableBeanValidationDDLTest$TxIdSupplier"),
		@Setting(name = ValidationSettings.JAKARTA_VALIDATION_MODE, value = "DDL")
})
class AuditAuxiliaryTableBeanValidationDDLTest {
	@Test
	void beanValidationNotNullPropagatesToAuditTables(SessionFactoryScope scope) {
		scope.getSessionFactory();

		final PersistentClass entityBinding =
				scope.getMetadataImplementor().getEntityBinding( AuditedEntity.class.getName() );

		assertAuxiliaryColumnIsNotNull( entityBinding, entityBinding.getAuxiliaryTable(), "status" );
		assertAuxiliaryColumnIsNotNull( entityBinding, entityBinding.getAuxiliaryTable(), "parent" );
		assertAuxiliaryColumnIsNotNull( entityBinding, secondaryAuditTable( entityBinding, "secondaryStatus" ),
				"secondaryStatus" );
		assertAuxiliaryColumnIsNotNull( entityBinding, secondaryAuditTable( entityBinding, "secondaryParent" ),
				"secondaryParent" );
		assertAuxiliaryColumnIsAbsent( entityBinding, entityBinding.getAuxiliaryTable(), "excluded" );
	}

	private static Table secondaryAuditTable(PersistentClass entityBinding, String propertyName) {
		final var sourceColumn = entityBinding.getProperty( propertyName ).getColumns().get( 0 );
		for ( var join : entityBinding.getJoins() ) {
			if ( join.getTable().getColumn( sourceColumn ) != null ) {
				return join.getAuxiliaryTable();
			}
		}
		throw new AssertionError( "No secondary audit table found for property " + propertyName );
	}

	private static void assertAuxiliaryColumnIsNotNull(
			PersistentClass entityBinding,
			Table auxiliaryTable,
			String propertyName) {
		final var sourceColumn = entityBinding.getProperty( propertyName ).getColumns().get( 0 );
		final var auxiliaryColumn = auxiliaryTable.getColumn( sourceColumn );

		assertThat( sourceColumn.isNullable() ).isFalse();
		assertThat( auxiliaryColumn ).isNotNull();
		assertThat( auxiliaryColumn.isNullable() ).isFalse();
	}

	private static void assertAuxiliaryColumnIsAbsent(
			PersistentClass entityBinding,
			Table auxiliaryTable,
			String propertyName) {
		final var sourceColumn = entityBinding.getProperty( propertyName ).getColumns().get( 0 );

		assertThat( sourceColumn.isNullable() ).isFalse();
		assertThat( auxiliaryTable.getColumn( sourceColumn ) ).isNull();
	}

	public static class TxIdSupplier implements ChangesetIdentifierSupplier<Integer> {
		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return 1;
		}
	}

	enum Status {
		ACTIVE,
		INACTIVE
	}

	@Audited
	@Entity(name = "AuditValidationParent")
	static class Parent {
		@Id
		Long id;
	}

	@Audited
	@Entity(name = "AuditValidationEntity")
	@SecondaryTable(name = "audit_validation_details")
	static class AuditedEntity {
		@Id
		Long id;

		@NotNull
		@Enumerated(EnumType.STRING)
		Status status;

		@NotNull
		@ManyToOne
		Parent parent;

		@NotNull
		@Column(table = "audit_validation_details")
		@Enumerated(EnumType.STRING)
		Status secondaryStatus;

		@NotNull
		@ManyToOne
		@JoinColumn(table = "audit_validation_details")
		Parent secondaryParent;

		@Audited.Excluded
		@NotNull
		String excluded;
	}
}
