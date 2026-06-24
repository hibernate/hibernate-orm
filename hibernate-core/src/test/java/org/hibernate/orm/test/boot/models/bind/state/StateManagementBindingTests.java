/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind.state;

import org.hibernate.annotations.Audited;
import org.hibernate.annotations.Temporal;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.orm.test.boot.models.bind.BindingTestingHelper;
import org.hibernate.persister.state.internal.AuditStateManagement;
import org.hibernate.persister.state.internal.TemporalStateManagement;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@BaseUnitTest
class StateManagementBindingTests {
	@Test
	void testEntityLevelTemporalBinding() {
		try ( var serviceRegistry = new StandardServiceRegistryBuilder()
				.applySetting( StateManagementSettings.TEMPORAL_TABLE_STRATEGY, "SINGLE_TABLE" )
				.build() ) {
			BindingTestingHelper.checkDomainModel(
					(context) -> {
						final var rootClass = context.getMetadataCollector()
								.getEntityBinding( TemporalEntity.class.getName() )
								.getRootClass();
						final var rowStart = rootClass.getAuxiliaryColumn( "rowStart" );
						final var rowEnd = rootClass.getAuxiliaryColumn( "rowEnd" );

						assertThat( rootClass.getStateManagementType() ).isEqualTo( TemporalStateManagement.class );
						assertThat( rootClass.getAuxiliaryTable() ).isSameAs( rootClass.getRootTable() );
						assertThat( rootClass.isAuxiliaryColumnInPrimaryKey() ).isTrue();
						assertThat( rowStart ).isNotNull();
						assertThat( rowStart.getName() ).isEqualTo( "valid_from" );
						assertThat( rowStart.isNullable() ).isFalse();
						assertThat( rowEnd ).isNotNull();
						assertThat( rowEnd.getName() ).isEqualTo( "valid_to" );
						assertThat( rowEnd.isNullable() ).isTrue();
					},
					serviceRegistry,
					TemporalEntity.class
			);
		}
	}

	@Test
	void testTemporalExcludedPropertyBinding() {
		try ( var serviceRegistry = new StandardServiceRegistryBuilder()
				.applySetting( StateManagementSettings.TEMPORAL_TABLE_STRATEGY, "SINGLE_TABLE" )
				.build() ) {
			BindingTestingHelper.checkDomainModel(
					(context) -> {
						final var entityBinding = context.getMetadataCollector()
								.getEntityBinding( TemporalEntity.class.getName() );
						final var included = entityBinding.getProperty( "included" );
						final var excluded = entityBinding.getProperty( "excluded" );

						assertThat( included.isTemporalExcluded() ).isFalse();
						assertThat( included.isOptimisticLocked() ).isTrue();
						assertThat( excluded.isTemporalExcluded() ).isTrue();
						assertThat( excluded.isOptimisticLocked() ).isFalse();
					},
					serviceRegistry,
					TemporalEntity.class
			);
		}
	}

	@Test
	void testAuditedExcludedPropertyBinding() {
		try ( var serviceRegistry = new StandardServiceRegistryBuilder()
				.applySetting( StateManagementSettings.TEMPORAL_TABLE_STRATEGY, "SINGLE_TABLE" )
				.build() ) {
			BindingTestingHelper.checkDomainModel(
					(context) -> {
						final var entityBinding = context.getMetadataCollector()
								.getEntityBinding( TemporalEntity.class.getName() );
						final var included = entityBinding.getProperty( "included" );
						final var excluded = entityBinding.getProperty( "auditedExcluded" );

						assertThat( included.isAuditedExcluded() ).isFalse();
						assertThat( included.isOptimisticLocked() ).isTrue();
						assertThat( excluded.isAuditedExcluded() ).isTrue();
						assertThat( excluded.isOptimisticLocked() ).isFalse();
					},
					serviceRegistry,
					TemporalEntity.class
			);
		}
	}

	@Test
	void testEntityLevelAuditedBinding() {
		try ( var serviceRegistry = new StandardServiceRegistryBuilder().build() ) {
			BindingTestingHelper.checkDomainModel(
					(context) -> {
						final var rootClass = context.getMetadataCollector()
								.getEntityBinding( AuditedEntity.class.getName() )
								.getRootClass();
						final var changesetId = rootClass.getAuxiliaryColumn( "changesetId" );
						final var modificationType = rootClass.getAuxiliaryColumn( "modificationType" );

						assertThat( rootClass.getStateManagementType() ).isEqualTo( AuditStateManagement.class );
						assertThat( rootClass.getAuxiliaryTable() ).isNotSameAs( rootClass.getRootTable() );
						assertThat( rootClass.getAuxiliaryTable().getName() ).isEqualTo( "audited_entities_AUD" );
						assertThat( changesetId ).isNotNull();
						assertThat( changesetId.getName() ).isEqualTo( "REV" );
						assertThat( modificationType ).isNotNull();
						assertThat( modificationType.getName() ).isEqualTo( "REVTYPE" );
					},
					serviceRegistry,
					AuditedEntity.class
			);
		}
	}

	@Entity
	@Table(name = "temporal_entities")
	@Temporal(rowStart = "valid_from", rowEnd = "valid_to")
	static class TemporalEntity {
		@Id
		private Long id;

		private String included;

		@Temporal.Excluded
		private String excluded;

		@Audited.Excluded
		private String auditedExcluded;
	}

	@Entity
	@Table(name = "audited_entities")
	@Audited
	static class AuditedEntity {
		@Id
		private Long id;

		private String name;
	}
}
