/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind.tenancy;

import org.hibernate.annotations.TenantId;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.InitCommand;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.boot.models.bind.internal.binders.TenantIdBinder;
import org.hibernate.boot.models.bind.internal.view.TenantIdContributionView;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.rowsecurity.RowLevelSecurity;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.models.bind.BindingTestingHelper.checkDomainModel;

/**
 * @author Steve Ebersole
 */
public class SimpleTenancyTests {
	@Test
	@ServiceRegistry
	void testSimpleTenancy(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					var metadataCollector = context.getMetadataCollector();

					final var filterDefinition = metadataCollector.getFilterDefinition( TenantIdBinder.FILTER_NAME );
					assertThat( filterDefinition ).isNotNull();
					assertThat( filterDefinition.isAppliedToLoadByKey() ).isTrue();

					final PersistentClass entityBinding = metadataCollector.getEntityBinding( ProtectedEntity.class.getName() );
					final TenantIdContributionView tenantIdContribution = context.getBindingState()
							.getBootBindingModel()
							.getTenantIdContributionView( entityType( context, ProtectedEntity.class ) );
					assertThat( tenantIdContribution ).isNotNull();
					assertThat( tenantIdContribution.attributeName() ).isEqualTo( "tenant" );
					assertThat( tenantIdContribution.member().getName() ).isEqualTo( "tenant" );

					assertTenantFilter( entityBinding, "tenant = :tenantId" );
					final Property tenantProperty = entityBinding.getProperty( "tenant" );
					final BasicValue value = (BasicValue) tenantProperty.getValue();
					final org.hibernate.mapping.Column column = (org.hibernate.mapping.Column) value.getColumn();

					assertThat( tenantProperty.isUpdatable() ).isFalse();
					assertThat( tenantProperty.isOptional() ).isFalse();

					assertThat( value.getEnumerationStyle() ).isEqualTo( EnumType.ORDINAL );

					assertThat( column.getName() ).isEqualTo( "tenant" );
				},
				scope.getRegistry(),
				ProtectedEntity.class
		);
	}

	@Test
	@ServiceRegistry
	void testTenancyWithColumn(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					var metadataCollector = context.getMetadataCollector();

					final var filterDefinition = metadataCollector.getFilterDefinition( TenantIdBinder.FILTER_NAME );
					assertThat( filterDefinition ).isNotNull();
					assertThat( filterDefinition.isAppliedToLoadByKey() ).isTrue();

					final PersistentClass entityBinding = metadataCollector.getEntityBinding( ProtectedEntityWithColumn.class.getName() );
					assertTenantFilter( entityBinding, "customer = :tenantId" );
					final Property tenantProperty = entityBinding.getProperty( "tenant" );
					final BasicValue value = (BasicValue) tenantProperty.getValue();
					final org.hibernate.mapping.Column column = (org.hibernate.mapping.Column) value.getColumn();

					assertThat( tenantProperty.isUpdatable() ).isFalse();
					assertThat( tenantProperty.isOptional() ).isFalse();

					assertThat( value.getEnumerationStyle() ).isEqualTo( EnumType.STRING );

					assertThat( column.getName() ).isEqualTo( "customer" );
				},
				scope.getRegistry(),
				ProtectedEntityWithColumn.class
		);
	}

	@Test
	@ServiceRegistry(settings = @Setting(
			name = JdbcSettings.DIALECT,
			value = "org.hibernate.orm.test.boot.models.bind.tenancy.SimpleTenancyTests$RlsDialect"
	))
	void testTenancyRegistersRowLevelSecurity(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final var metadata = context.getMetadata();
					final var table = metadata.getEntityBinding( ProtectedEntityWithStringTenant.class.getName() )
							.getTable();

					assertThat( table.getInitCommands( null ) )
							.extracting( command -> command.initCommands()[0] )
							.containsExactly( "rls:protected_entity:tenant_id:SESSION" );
				},
				scope.getRegistry(),
				ProtectedEntityWithStringTenant.class
		);
	}

	private static void assertTenantFilter(PersistentClass entityBinding, String expectedCondition) {
		assertThat( entityBinding.getFilters() ).hasSize( 1 );
		final var filter = entityBinding.getFilters().get( 0 );
		assertThat( filter.getName() ).isEqualTo( TenantIdBinder.FILTER_NAME );
		assertThat( filter.getCondition() ).isEqualTo( expectedCondition );
		assertThat( filter.useAutoAliasInjection() ).isTrue();
	}

	private static EntityTypeMetadata entityType(
			org.hibernate.orm.test.boot.models.bind.BindingTestingHelper.DomainModelCheckContext context,
			Class<?> entityClass) {
		for ( EntityHierarchy hierarchy : context.getCategorizedDomainModel().getEntityHierarchies() ) {
			if ( hierarchy.getRoot().getClassDetails().getClassName().equals( entityClass.getName() ) ) {
				return hierarchy.getRoot();
			}
		}
		throw new AssertionError( "Could not locate entity type for " + entityClass.getName() );
	}

	enum Tenant { ACME, SPACELY }

	@Entity(name = "ProtectedEntity")
	@Table(name = "protected_entity")
	public static class ProtectedEntity {
		@Id
		private Integer id;

		private String name;

		@TenantId
		@Enumerated
		private Tenant tenant;
	}

	@Entity(name = "ProtectedEntity")
	@Table(name = "protected_entity")
	public static class ProtectedEntityWithColumn {
		@Id
		private Integer id;

		private String name;

		@TenantId
		@Enumerated(EnumType.STRING)
		@Column(name = "customer")
		private Tenant tenant;
	}

	@Entity(name = "ProtectedEntity")
	@Table(name = "protected_entity")
	public static class ProtectedEntityWithStringTenant {
		@Id
		private Integer id;

		private String name;

		@TenantId
		@Column(name = "tenant_id")
		private String tenant;
	}

	public static class RlsDialect extends H2Dialect {
		@Override
		public RowLevelSecurity getRowLevelSecurity() {
			return new TestRowLevelSecurity();
		}
	}

	public static class TestRowLevelSecurity implements RowLevelSecurity {
		@Override
		public boolean supportsRowLevelSecurity() {
			return true;
		}

		@Override
		public void addTenantIdTableInitCommands(
				InFlightMetadataCollector collector,
				org.hibernate.mapping.Table table,
				org.hibernate.mapping.Column tenantIdentifierColumn,
				Metadata metadata,
				TenantIdentifierSource tenantIdentifierSource) {
			table.addInitCommand( ignored -> new InitCommand(
					"rls:" + table.getName() + ":" + tenantIdentifierColumn.getName() + ":" + tenantIdentifierSource
			) );
		}
	}
}
