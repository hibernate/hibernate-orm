/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.binder.internal;

import java.util.Collections;

import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.annotations.TenantId;
import org.hibernate.binder.AttributeBinder;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.dialect.rowsecurity.RowLevelSecurity;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_CREDENTIALS_MAPPER;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_RLS_ENABLED;
import static org.hibernate.context.spi.MultiTenancy.getTenantCredentialsMapper;
import static org.hibernate.dialect.rowsecurity.RowLevelSecurity.TenantIdentifierSource.DATABASE_USER;
import static org.hibernate.dialect.rowsecurity.RowLevelSecurity.TenantIdentifierSource.SESSION;
import static org.hibernate.internal.util.config.ConfigurationHelper.getBoolean;

/**
 * Sets up filters associated with a {@link TenantId} field
 *
 * @author Gavin King
 */
public class TenantIdBinder implements AttributeBinder<TenantId> {

	public static final String FILTER_NAME = "_tenantId";
	public static final String PARAMETER_NAME = "tenantId";

	@Override
	public void bind(
			TenantId tenantId,
			MetadataBuildingContext buildingContext,
			PersistentClass persistentClass,
			Property property) {
		final var collector = buildingContext.getMetadataCollector();
		final var tenantIdType =
				collector.getTypeConfiguration().getBasicTypeRegistry()
						.getRegisteredType( property.getReturnedClassName() );
		final var filterDefinition = collector.getFilterDefinition( FILTER_NAME );
		if ( filterDefinition == null ) {
			collector.addFilterDefinition(
					new FilterDefinition(
							FILTER_NAME,
							"",
							false,
							true,
							singletonMap( PARAMETER_NAME, tenantIdType ),
							emptyMap()
					)
			);
		}
		else {
			final var tenantIdTypeJtd = tenantIdType.getJavaTypeDescriptor();
			final var jdbcMapping = filterDefinition.getParameterJdbcMapping( PARAMETER_NAME );
			assert jdbcMapping != null;
			final var parameterJavaType = jdbcMapping.getJavaTypeDescriptor();
			if ( !parameterJavaType.getJavaTypeClass()
					.equals( tenantIdTypeJtd.getJavaTypeClass() ) ) {
				throw new MappingException(
						"all @TenantId fields must have the same type: "
								+ parameterJavaType.getTypeName()
								+ " differs from "
								+ tenantIdTypeJtd.getTypeName()
				);
			}
		}
		persistentClass.addFilter(
				FILTER_NAME,
				columnNameOrFormula( property )
						+ " = :"
						+ PARAMETER_NAME,
				true,
				emptyMap(),
				emptyMap()
		);

		if ( isRowLevelSecurityEnabled( buildingContext ) ) {
			addRowLevelSecurity(
					collector.getDatabase().getDialect().getRowLevelSecurity(),
					collector,
					buildingContext,
					property
			);
		}

		property.resetUpdateable( false );
		property.resetOptional( false );
	}

	private static boolean isRowLevelSecurityEnabled(MetadataBuildingContext buildingContext) {
		return getBoolean(
				MULTI_TENANT_RLS_ENABLED,
				buildingContext.getBootstrapContext().getConfigurationService().getSettings(),
				true
		);
	}

	private static void addRowLevelSecurity(
			RowLevelSecurity rowLevelSecurity,
			InFlightMetadataCollector collector,
			MetadataBuildingContext buildingContext,
			Property property) {
		if ( rowLevelSecurity.supportsRowLevelSecurity() ) {
			final var table = property.getValue().getTable();
			if ( property.getSelectables().get( 0 ) instanceof Column column
					&& table.isPhysicalTable() && !table.isView() ) {
				final var tenantIdentifierSource =
						getTenantIdentifierSource( rowLevelSecurity, buildingContext );
				if ( tenantIdentifierSource == SESSION ) {
					rowLevelSecurity.addTenantIdTableInitCommands( collector, table, column, collector );
				}
				else {
					rowLevelSecurity.addTenantIdTableInitCommands(
							collector,
							table,
							column,
							collector,
							tenantIdentifierSource
					);
				}
			}
		}
	}

	private static RowLevelSecurity.TenantIdentifierSource getTenantIdentifierSource(
			RowLevelSecurity rowLevelSecurity,
			MetadataBuildingContext buildingContext) {
		final var databaseUserSource =
				hasTenantCredentialsMapper( buildingContext )
						&& rowLevelSecurity.supportsTenantIdentifierSource( DATABASE_USER );
		return databaseUserSource ? DATABASE_USER : SESSION;
	}

	private static boolean hasTenantCredentialsMapper(MetadataBuildingContext buildingContext) {
		final var bootstrapContext = buildingContext.getBootstrapContext();
		final var settings = bootstrapContext.getConfigurationService().getSettings();
		return settings.get( MULTI_TENANT_CREDENTIALS_MAPPER ) != null
			|| getTenantCredentialsMapper( settings, bootstrapContext.getServiceRegistry() ) != null;
	}

	private String columnNameOrFormula(Property property) {
		if ( property.getColumnSpan() != 1 ) {
			throw new MappingException( "@TenantId attribute must be mapped to a single column or formula" );
		}
		final var selectable = property.getSelectables().get( 0 );
		if ( selectable instanceof Formula formula ) {
			return formula.getFormula();
		}
		else if ( selectable instanceof Column column ) {
			return column.getName();
		}
		else {
			throw new AssertionFailure( "@TenantId attribute must be mapped to a column or formula" );
		}
	}

}
