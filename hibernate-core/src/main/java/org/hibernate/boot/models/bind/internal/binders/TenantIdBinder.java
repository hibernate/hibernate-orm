/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import org.hibernate.MappingException;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.spi.TypeConfiguration;

import static java.util.Collections.singletonMap;
import static org.hibernate.boot.models.bind.internal.binders.AttributeBinder.bindConversion;
import static org.hibernate.boot.models.bind.internal.binders.AttributeBinder.bindImplicitJavaType;
import static org.hibernate.boot.models.bind.internal.binders.AttributeBinder.processColumn;
import static org.hibernate.boot.models.bind.internal.binders.BasicValueBinder.bindEnumerated;
import static org.hibernate.boot.models.bind.internal.binders.BasicValueBinder.bindJavaType;
import static org.hibernate.boot.models.bind.internal.binders.BasicValueBinder.bindJdbcType;

/**
 * @author Steve Ebersole
 */
public class TenantIdBinder {
	public static final String FILTER_NAME = "_tenantId";
	public static final String PARAMETER_NAME = "tenantId";

	public static void bindTenantId(
			EntityTypeMetadata managedType,
			RootClass rootClass,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final AttributeMetadata tenantIdAttribute = managedType.getHierarchy().getTenantIdAttribute();
		if ( tenantIdAttribute == null ) {
			return;
		}

		bindTenantId( tenantIdAttribute, rootClass, bindingOptions, bindingState, bindingContext );
	}

	public static void bindTenantId(
			AttributeMetadata attributeMetadata,
			RootClass rootClass,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final InFlightMetadataCollector collector = bindingState.getMetadataBuildingContext().getMetadataCollector();
		final TypeConfiguration typeConfiguration = collector.getTypeConfiguration();

		final MemberDetails memberDetails = attributeMetadata.getMember();
		final String returnedClassName = memberDetails.getType().getClassName();
		final BasicType<Object> tenantIdType = typeConfiguration
				.getBasicTypeRegistry()
				.getRegisteredType( returnedClassName );

		final FilterDefinition filterDefinition = collector.getFilterDefinition( FILTER_NAME );
		if ( filterDefinition == null ) {
			collector.addFilterDefinition( new FilterDefinition(
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
		rootClass.addProperty( property );
		property.setName( attributeMetadata.getName() );

		final BasicValue basicValue = new BasicValue( bindingState.getMetadataBuildingContext(), rootClass.getRootTable() );
		property.setValue( basicValue );

		bindImplicitJavaType( memberDetails, property, basicValue, bindingOptions, bindingState, bindingContext );
		bindJavaType( memberDetails, property, basicValue, bindingOptions, bindingState, bindingContext );
		bindJdbcType( memberDetails, property, basicValue, bindingOptions, bindingState, bindingContext );

		bindConversion( memberDetails, property, basicValue, bindingOptions, bindingState, bindingContext );
		bindEnumerated( memberDetails, property, basicValue, bindingOptions, bindingState, bindingContext );

		processColumn(
				memberDetails,
				property,
				basicValue,
				rootClass.getRootTable(),
				bindingOptions,
				bindingState,
				bindingContext
		);

		property.resetUpdateable( false );
		property.resetOptional( false );
	}

	public static void bindTenantId(
			AttributeMetadata attributeMetadata,
			EntityTypeMetadata managedType,
			RootClass typeBinding,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		bindTenantId( attributeMetadata, typeBinding, bindingOptions, bindingState, bindingContext );
	}
}
