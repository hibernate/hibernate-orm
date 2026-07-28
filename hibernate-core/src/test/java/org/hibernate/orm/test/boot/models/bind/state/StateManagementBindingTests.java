/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind.state;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Audited;
import org.hibernate.annotations.Changelog;
import org.hibernate.annotations.Temporal;
import org.hibernate.audit.spi.ChangelogSupplier;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.SchemaToolingSettings;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.orm.test.boot.models.bind.BindingTestingHelper;
import org.hibernate.persister.state.internal.AuditStateManagement;
import org.hibernate.persister.state.internal.TemporalStateManagement;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.SessionFactoryUtil;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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

	@Test
	void testChangelogBinding() {
		try ( var serviceRegistry = new StandardServiceRegistryBuilder().build() ) {
			BindingTestingHelper.checkDomainModel(
					(context) -> {
						final var supplier = ChangelogSupplier.resolve( serviceRegistry );
						final var entityBinding = context.getMetadataCollector()
								.getEntityBinding( ChangelogEntity.class.getName() );
						final var revision = entityBinding.getProperty( "revision" );

						assertThat( supplier ).isNotNull();
						assertThat( supplier.getChangelogClass() ).isEqualTo( ChangelogEntity.class );
						assertThat( supplier.getChangesetIdProperty() ).isEqualTo( "revision" );
						assertThat( supplier.getTimestampProperty() ).isEqualTo( "createdAt" );
						assertThat( supplier.getModifiedEntitiesProperty() ).isNull();
						assertThat( revision.getColumns().get( 0 ).isUnique() ).isTrue();
					},
					serviceRegistry,
					ChangelogEntity.class
			);
		}
	}

	@Test
	void testEntityRuntimeMappings() {
		try ( var serviceRegistry = new StandardServiceRegistryBuilder()
				.applySetting( StateManagementSettings.TEMPORAL_TABLE_STRATEGY, "SINGLE_TABLE" )
				.build() ) {
			BindingTestingHelper.checkDomainModel(
					(context) -> {
						try ( var sessionFactory = SessionFactoryUtil.buildSessionFactory( context.getMetadata() ) ) {
							final var mappingMetamodel = sessionFactory.getMappingMetamodel();

							final var temporalEntity = mappingMetamodel.getEntityDescriptor( TemporalEntity.class );
							final var temporalMapping = temporalEntity.getTemporalMapping();
							assertThat( temporalMapping ).isNotNull();
							assertThat( temporalMapping.getStartingColumnMapping().getSelectionExpression() )
									.isEqualTo( "valid_from" );
							assertThat( temporalMapping.getEndingColumnMapping().getSelectionExpression() )
									.isEqualTo( "valid_to" );

							final var auditedEntity = mappingMetamodel.getEntityDescriptor( AuditedEntity.class );
							final var auditMapping = auditedEntity.getAuditMapping();
							assertThat( auditMapping ).isNotNull();
							assertThat( auditMapping.getChangesetIdMapping( "audited_entities" ).getSelectionExpression() )
									.isEqualTo( "REV" );
							assertThat( auditMapping.getModificationTypeMapping( "audited_entities" ).getSelectionExpression() )
									.isEqualTo( "REVTYPE" );
						}
					},
					serviceRegistry,
					TemporalEntity.class,
					AuditedEntity.class
			);
		}
	}

	@Test
	void testCollectionRuntimeMappings() {
		try ( var serviceRegistry = new StandardServiceRegistryBuilder()
				.applySetting( StateManagementSettings.TEMPORAL_TABLE_STRATEGY, "SINGLE_TABLE" )
				.build() ) {
			BindingTestingHelper.checkDomainModel(
					(context) -> {
						try ( var sessionFactory = SessionFactoryUtil.buildSessionFactory( context.getMetadata() ) ) {
							final var mappingMetamodel = sessionFactory.getMappingMetamodel();

							final var temporalEntity = mappingMetamodel.getEntityDescriptor( TemporalEntity.class );
							final var temporalValues =
									(PluralAttributeMapping) temporalEntity.findAttributeMapping( "values" );
							assertThat( temporalValues.getTemporalMapping() ).isNotNull();
							assertThat(
									temporalValues.getTemporalMapping()
											.getStartingColumnMapping()
											.getContainingTableExpression()
							).isEqualTo( "temporal_entity_values" );

							final var auditedEntity = mappingMetamodel.getEntityDescriptor( AuditedEntity.class );
							final var auditedValues =
									(PluralAttributeMapping) auditedEntity.findAttributeMapping( "values" );
							assertThat( auditedValues.getAuditMapping() ).isNotNull();
							assertThat(
									auditedValues.getAuditMapping()
											.getChangesetIdMapping( "audited_entity_values" )
											.getContainingTableExpression()
							).isEqualTo( "audited_entity_values_AUD" );
						}
					},
					serviceRegistry,
					TemporalEntity.class,
					AuditedEntity.class
			);
		}
	}

	@Test
	void testRuntimeMutationHandoff() {
		try ( var serviceRegistry = new StandardServiceRegistryBuilder()
				.applySetting( StateManagementSettings.TEMPORAL_TABLE_STRATEGY, "SINGLE_TABLE" )
				.applySetting(
						StateManagementSettings.CHANGESET_ID_SUPPLIER,
						TestChangesetIdentifierSupplier.class.getName()
				)
				.applySetting( SchemaToolingSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, "create-drop" )
				.build() ) {
			BindingTestingHelper.checkDomainModel(
					(context) -> {
						try ( var sessionFactory = SessionFactoryUtil.buildSessionFactory( context.getMetadata() ) ) {
							sessionFactory.inTransaction( (session) -> {
								final var temporal = new TemporalEntity();
								temporal.id = 1L;
								temporal.included = "initial";
								temporal.values.add( "one" );
								session.persist( temporal );

								final var audited = new AuditedEntity();
								audited.id = 1L;
								audited.name = "initial";
								audited.values.add( "one" );
								session.persist( audited );
							} );

							sessionFactory.inTransaction( (session) -> {
								final var temporal = session.find( TemporalEntity.class, 1L );
								temporal.included = "updated";
								temporal.values.add( "two" );

								final var audited = session.find( AuditedEntity.class, 1L );
								audited.name = "updated";
								audited.values.add( "two" );
							} );

							sessionFactory.inTransaction( (session) -> {
								final var temporal = session.find( TemporalEntity.class, 1L );
								assertThat( temporal.included ).isEqualTo( "updated" );
								assertThat( temporal.values ).containsExactlyInAnyOrder( "one", "two" );

								final var audited = session.find( AuditedEntity.class, 1L );
								assertThat( audited.name ).isEqualTo( "updated" );
								assertThat( audited.values ).containsExactlyInAnyOrder( "one", "two" );

								final Number temporalRows = (Number) session
										.createNativeQuery( "select count(*) from temporal_entities" )
										.getSingleResult();
								final Number auditRows = (Number) session
										.createNativeQuery( "select count(*) from audited_entities_AUD" )
										.getSingleResult();

								assertThat( temporalRows.longValue() ).isEqualTo( 2L );
								assertThat( auditRows.longValue() ).isEqualTo( 2L );
							} );
						}
					},
					serviceRegistry,
					TemporalEntity.class,
					AuditedEntity.class
			);
		}
	}

	public static class TestChangesetIdentifierSupplier implements ChangesetIdentifierSupplier<Long> {
		private static final AtomicLong SEQUENCE = new AtomicLong();

		@Override
		public Long generateIdentifier(SharedSessionContract session) {
			return SEQUENCE.incrementAndGet();
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

		@ElementCollection
		@CollectionTable(name = "temporal_entity_values", joinColumns = @JoinColumn(name = "entity_id"))
		@Column(name = "state_value")
		private Set<String> values = new HashSet<>();
	}

	@Entity
	@Table(name = "audited_entities")
	@Audited
	static class AuditedEntity {
		@Id
		private Long id;

		private String name;

		@ElementCollection
		@CollectionTable(name = "audited_entity_values", joinColumns = @JoinColumn(name = "entity_id"))
		@Column(name = "state_value")
		private Set<String> values = new HashSet<>();
	}

	@Entity
	@Changelog
	static class ChangelogEntity {
		@Id
		private Long id;

		@Changelog.ChangesetId
		private Long revision;

		@Changelog.Timestamp
		private Instant createdAt;
	}
}
