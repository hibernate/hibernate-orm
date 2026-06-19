/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind.locking;

import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.models.bind.BindingTestingHelper.checkDomainModel;

/**
 * @author Steve Ebersole
 */
public class OptimisticLockingTests {
	@Test
	@ServiceRegistry
	void testVersionAttribute(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final var rootType = context.getCategorizedDomainModel().getEntityHierarchies().iterator().next().getRoot();
					final var versionContribution = context.getBindingState()
							.getBootBindingModel()
							.getVersionContributionView( rootType );
					assertThat( versionContribution ).isNotNull();
					assertThat( versionContribution.attributeName() ).isEqualTo( "version" );
					assertThat( versionContribution.valueIntent().columnSource() ).isNull();

					var metadataCollector = context.getMetadataCollector();
					final PersistentClass entityBinding = metadataCollector.getEntityBinding( VersionedEntity.class.getName() );
					assertThat( entityBinding.getVersion() ).isNotNull();
					final BasicValue value = (BasicValue) entityBinding.getVersion().getValue();
					final org.hibernate.mapping.Column column = (org.hibernate.mapping.Column) value.getColumn();
					assertThat( column ).isNotNull();
					assertThat( column.getName() ).isEqualTo( "version" );
				},
				scope.getRegistry(),
				VersionedEntity.class
		);
	}

	@Test
	@ServiceRegistry
	void testVersionAttributeWithColumn(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final var rootType = context.getCategorizedDomainModel().getEntityHierarchies().iterator().next().getRoot();
					final var versionContribution = context.getBindingState()
							.getBootBindingModel()
							.getVersionContributionView( rootType );
					assertThat( versionContribution ).isNotNull();
					assertThat( versionContribution.attributeName() ).isEqualTo( "version" );
					assertThat( versionContribution.valueIntent().columnSource().nonEmptyName() ).isEqualTo( "revision" );

					var metadataCollector = context.getMetadataCollector();
					final PersistentClass entityBinding = metadataCollector.getEntityBinding( VersionedEntityWithColumn.class.getName() );
					assertThat( entityBinding.getVersion() ).isNotNull();
					final BasicValue value = (BasicValue) entityBinding.getVersion().getValue();
					final org.hibernate.mapping.Column column = (org.hibernate.mapping.Column) value.getColumn();
					assertThat( column ).isNotNull();
					assertThat( column.getName() ).isEqualTo( "revision" );
				},
				scope.getRegistry(),
				VersionedEntityWithColumn.class
		);
	}

	@Test
	@ServiceRegistry
	void testDirtyVersioning(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					var metadataCollector = context.getMetadataCollector();
					final PersistentClass entityBinding = metadataCollector.getEntityBinding( DirtyVersionedEntity.class.getName() );
					assertThat( entityBinding.getOptimisticLockStyle() ).isEqualTo( OptimisticLockStyle.DIRTY );
					assertThat( entityBinding.getVersion() ).isNull();
				},
				scope.getRegistry(),
				DirtyVersionedEntity.class
		);
	}

	@Entity(name = "VersionedEntity")
	@Table(name = "versioned")
	public static class VersionedEntity {
		@Id
		private Integer id;

		private String name;

		@Version
		private int version;
	}

	@Entity(name = "VersionedEntity")
	@Table(name = "versioned2")
	public static class VersionedEntityWithColumn {
		@Id
		private Integer id;

		private String name;

		@Version
		@Column(name = "revision")
		private int version;
	}

	@Entity(name = "DirtyVersionedEntity")
	@Table(name = "versioned3")
	@OptimisticLocking(type = OptimisticLockType.DIRTY)
	public static class DirtyVersionedEntity {
		@Id
		private Integer id;

		private String name;
	}
}
