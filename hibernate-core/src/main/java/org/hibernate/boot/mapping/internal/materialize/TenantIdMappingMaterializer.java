/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.materialize;

import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.boot.mapping.internal.model.BasicValueIntent;
import org.hibernate.boot.mapping.internal.view.TenantIdContributionView;
import org.hibernate.boot.mapping.internal.context.BindingContext;
import org.hibernate.boot.mapping.internal.context.BindingOptions;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.dialect.rowsecurity.RowLevelSecurity;
import org.hibernate.dialect.rowsecurity.RowLevelSecurity.TenantIdentifierSource;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_CREDENTIALS_MAPPER;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_RLS_ENABLED;
import static org.hibernate.context.spi.MultiTenancy.getTenantCredentialsMapper;
import static org.hibernate.internal.util.config.ConfigurationHelper.getBoolean;

/// Materializes the legacy mapping contribution for a `@TenantId` attribute.
///
/// Tenant-id binding is a specialized built-in contribution layered on top of a
/// basic property.  In addition to creating the property/value mapping shape, it
/// registers the shared tenant filter definition, attaches the filter to the
/// entity, and optionally contributes row-level-security table initialization.
///
/// @since 9.0
/// @author Steve Ebersole
public class TenantIdMappingMaterializer {
	public static final String FILTER_NAME = "_tenantId";
	public static final String PARAMETER_NAME = "tenantId";

	public Property materializeTenantId(
			TenantIdContributionView contribution,
			RootClass typeBinding,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		return materializeTenantId(
				contribution.attributeName(),
				contribution.member(),
				contribution.valueIntent(),
				contribution.tenantIdType(),
				typeBinding,
				bindingOptions,
				bindingState,
				bindingContext
		);
	}

	private Property materializeTenantId(
			String attributeName,
			MemberDetails memberDetails,
			BasicValueIntent valueIntent,
			BasicType<?> tenantIdType,
			RootClass typeBinding,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		validateAndRegisterFilterDefinition( tenantIdType, bindingState );

		final Property property = new PropertyMappingMaterializer().createProperty( attributeName, memberDetails );
		typeBinding.addProperty( property );

		new BasicValueMappingMaterializer().materializeTenantIdBasicValue(
				memberDetails,
				valueIntent,
				property,
				typeBinding.getRootTable(),
				bindingOptions,
				bindingState,
				bindingContext
		);

		typeBinding.addFilter(
				FILTER_NAME,
				columnNameOrFormula( property ) + " = :" + PARAMETER_NAME,
				true,
				emptyMap(),
				emptyMap()
		);

		if ( isRowLevelSecurityEnabled( bindingState.getMetadataBuildingContext() ) ) {
			addRowLevelSecurity(
					bindingState.getDatabase().getDialect().getRowLevelSecurity(),
					bindingState.getMetadataBuildingContext().getMetadataCollector(),
					bindingState.getMetadataBuildingContext(),
					property
			);
		}

		property.resetUpdateable( false );
		property.resetOptional( false );
		return property;
	}

	private static void validateAndRegisterFilterDefinition(BasicType<?> tenantIdType, BindingState bindingState) {
		final FilterDefinition filterDefinition = bindingState.getFilterDefinition( FILTER_NAME );
		if ( filterDefinition == null ) {
			bindingState.addFilterDefinition( new FilterDefinition(
					FILTER_NAME,
					"",
					false,
					true,
					singletonMap( PARAMETER_NAME, tenantIdType ),
					emptyMap()
			) );
		}
		else {
			final JavaType<?> tenantIdTypeJtd = tenantIdType.getJavaTypeDescriptor();
			final JavaType<?> parameterJtd = filterDefinition
					.getParameterJdbcMapping( PARAMETER_NAME )
					.getJavaTypeDescriptor();
			if ( !parameterJtd.getJavaTypeClass().equals( tenantIdTypeJtd.getJavaTypeClass() ) ) {
				throw new MappingException(
						"all @TenantId fields must have the same type: "
								+ parameterJtd.getJavaType().getTypeName()
								+ " differs from "
								+ tenantIdTypeJtd.getJavaType().getTypeName()
				);
			}
		}
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
				rowLevelSecurity.addTenantIdTableInitCommands(
						collector,
						table,
						column,
						collector,
						hasTenantCredentialsMapper( buildingContext )
								&& rowLevelSecurity.supportsTenantIdentifierSource( TenantIdentifierSource.DATABASE_USER )
								? TenantIdentifierSource.DATABASE_USER
								: TenantIdentifierSource.SESSION
				);
			}
		}
	}

	private static boolean hasTenantCredentialsMapper(MetadataBuildingContext buildingContext) {
		final var bootstrapContext = buildingContext.getBootstrapContext();
		final var settings = bootstrapContext.getConfigurationService().getSettings();
		return settings.get( MULTI_TENANT_CREDENTIALS_MAPPER ) != null
				|| getTenantCredentialsMapper( settings, bootstrapContext.getServiceRegistry() ) != null;
	}

	private static String columnNameOrFormula(Property property) {
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
