/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.models.spi.MemberDetails;

import static org.hibernate.boot.models.bind.internal.binders.AttributeBinder.bindImplicitJavaType;
import static org.hibernate.boot.models.bind.internal.binders.AttributeBinder.processColumn;
import static org.hibernate.boot.models.bind.internal.binders.BasicValueBinder.bindJavaType;
import static org.hibernate.boot.models.bind.internal.binders.BasicValueBinder.bindJdbcType;

/**
 * @author Steve Ebersole
 */
public class VersionBinder {
	public static void bindVersion(
			EntityTypeMetadata typeMetadata,
			RootClass rootClass,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final AttributeMetadata versionAttribute = typeMetadata.getHierarchy().getVersionAttribute();
		if ( versionAttribute == null ) {
			return;
		}

		bindVersion( versionAttribute, rootClass, bindingOptions, bindingState, bindingContext );
	}

	public static void bindVersion(
			AttributeMetadata versionAttribute,
			RootClass typeBinding,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final Property property = new Property();
		property.setName( versionAttribute.getName() );
		typeBinding.setVersion( property );
		typeBinding.addProperty( property );

		final BasicValue basicValue = new BasicValue(
				bindingState.getMetadataBuildingContext(),
				typeBinding.getRootTable()
		);
		property.setValue( basicValue );

		final MemberDetails memberDetails = versionAttribute.getMember();
		bindImplicitJavaType( memberDetails, property, basicValue, bindingOptions, bindingState, bindingContext );
		bindJavaType( memberDetails, property, basicValue, bindingOptions, bindingState, bindingContext );
		bindJdbcType( memberDetails, property, basicValue, bindingOptions, bindingState, bindingContext );

		final org.hibernate.mapping.Column column = processColumn(
				memberDetails,
				property,
				basicValue,
				typeBinding.getRootTable(),
				bindingOptions,
				bindingState,
				bindingContext
		);
		// force it to be non-nullable
		column.setNullable( false );
	}

	public static void bindVersion(
			AttributeMetadata attributeMetadata,
			EntityTypeMetadata managedType,
			RootClass typeBinding,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final Property property = new Property();
		property.setName( attributeMetadata.getName() );
		typeBinding.setVersion( property );
		typeBinding.addProperty( property );

		final BasicValue basicValue = new BasicValue(
				bindingState.getMetadataBuildingContext(),
				typeBinding.getRootTable()
		);
		property.setValue( basicValue );

		final MemberDetails memberDetails = attributeMetadata.getMember();
		bindImplicitJavaType( memberDetails, property, basicValue, bindingOptions, bindingState, bindingContext );
		bindJavaType( memberDetails, property, basicValue, bindingOptions, bindingState, bindingContext );
		bindJdbcType( memberDetails, property, basicValue, bindingOptions, bindingState, bindingContext );

		final org.hibernate.mapping.Column column = processColumn(
				memberDetails,
				property,
				basicValue,
				typeBinding.getRootTable(),
				bindingOptions,
				bindingState,
				bindingContext
		);
		// force it to be non-nullable
		column.setNullable( false );
	}
}
