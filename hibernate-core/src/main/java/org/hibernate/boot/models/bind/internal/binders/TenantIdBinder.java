/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import org.hibernate.MappingException;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.spi.TypeConfiguration;

import static java.util.Collections.singletonMap;
import static org.hibernate.boot.models.bind.internal.binders.AttributeBinder.bindPropertyAccessor;
import static org.hibernate.boot.models.bind.internal.binders.AttributeBinder.processColumn;

/// Binds the entity tenant-id property and shared tenant filter definition.
///
/// `@TenantId` contributes both a normal basic property and global filter
/// metadata.  The binder therefore validates the tenant-id type against any
/// previously registered tenant filter parameter before adding the property to
/// the root entity mapping.
///
/// @since 9.0
/// @author Steve Ebersole
public class TenantIdBinder {
	public static final String FILTER_NAME = "_tenantId";
	public static final String PARAMETER_NAME = "tenantId";

	public static void bindTenantId(
			AttributeMetadata attributeMetadata,
			EntityTypeMetadata managedType,
			RootClass typeBinding,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final TypeConfiguration typeConfiguration = bindingState.getTypeConfiguration();

		final MemberDetails memberDetails = attributeMetadata.getMember();
		final String returnedClassName = memberDetails.getType().determineRawClass().getClassName();
		final BasicType<?> tenantIdType = typeConfiguration
				.getBasicTypeRegistry()
				.getRegisteredType( returnedClassName );

		final FilterDefinition filterDefinition = bindingState.getFilterDefinition( FILTER_NAME );
		if ( filterDefinition == null ) {
			bindingState.addFilterDefinition( new FilterDefinition(
					FILTER_NAME,
					"",
					singletonMap( PARAMETER_NAME, tenantIdType )
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

		final Property property = new Property();
		typeBinding.addProperty( property );
		property.setName( attributeMetadata.getName() );
		bindPropertyAccessor( memberDetails, property );

		final BasicValue basicValue = new BasicValue( bindingState.getMetadataBuildingContext(), typeBinding.getRootTable() );
		property.setValue( basicValue );

		processColumn(
				memberDetails,
				property,
				basicValue,
				typeBinding.getRootTable(),
				bindingOptions,
				bindingState,
				bindingContext
		);
		BasicValueBinder.bindBasicValue(
				org.hibernate.boot.models.bind.internal.sources.BasicValueSource.attribute( memberDetails ),
				property,
				basicValue,
				bindingOptions,
				bindingState,
				bindingContext
		);

		property.resetUpdateable( false );
		property.resetOptional( false );
	}
}
