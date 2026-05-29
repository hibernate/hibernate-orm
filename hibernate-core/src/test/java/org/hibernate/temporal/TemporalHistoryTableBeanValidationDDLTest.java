/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.temporal;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.Temporal;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.cfg.ValidationSettings;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SessionFactory
@DomainModel(annotatedClasses = {
		TemporalHistoryTableBeanValidationDDLTest.Parent.class,
		TemporalHistoryTableBeanValidationDDLTest.TemporalEntity.class
})
@ServiceRegistry(settings = {
		@Setting(name = StateManagementSettings.TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"),
		@Setting(name = ValidationSettings.JAKARTA_VALIDATION_MODE, value = "DDL")
})
class TemporalHistoryTableBeanValidationDDLTest {
	@BeforeAll
	static void beforeAll(SessionFactoryScope scope) {
		// Build the SessionFactory before inspecting the boot model so Bean Validation DDL integration has run.
		scope.getSessionFactory();
	}

	@Test
	void beanValidationNotNullPropagatesToHistoryTable(SessionFactoryScope scope) {
		final PersistentClass entityBinding =
				scope.getMetadataImplementor().getEntityBinding( TemporalEntity.class.getName() );
		final Table historyTable = entityBinding.getAuxiliaryTable();

		assertHistoryColumnIsNotNull( entityBinding, historyTable, "status" );
		assertHistoryColumnIsNotNull( entityBinding, historyTable, "parent" );
	}

	private static void assertHistoryColumnIsNotNull(
			PersistentClass entityBinding,
			Table historyTable,
			String propertyName) {
		final Column sourceColumn = entityBinding.getProperty( propertyName ).getColumns().get( 0 );
		final Column historyColumn = historyTable.getColumn( sourceColumn );

		assertThat( sourceColumn.isNullable() ).isFalse();
		assertThat( historyColumn ).isNotNull();
		assertThat( historyColumn.isNullable() ).isFalse();
	}

	enum Status {
		ACTIVE,
		INACTIVE
	}

	@Entity(name = "TemporalHistoryValidationParent")
	static class Parent {
		@Id
		Long id;
	}

	@Temporal
	@Entity(name = "TemporalHistoryValidationEntity")
	static class TemporalEntity {
		@Id
		Long id;

		@NotNull
		@Enumerated(EnumType.STRING)
		Status status;

		@NotNull
		@ManyToOne
		Parent parent;
	}
}
