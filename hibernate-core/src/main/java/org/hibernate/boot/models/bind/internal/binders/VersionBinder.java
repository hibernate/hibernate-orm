/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
import static org.hibernate.boot.models.bind.internal.binders.AttributeBinder.bindPropertyAccessor;
import static org.hibernate.boot.models.bind.internal.binders.AttributeBinder.processColumn;
import static org.hibernate.boot.models.bind.internal.binders.BasicValueBinder.bindJavaType;
import static org.hibernate.boot.models.bind.internal.binders.BasicValueBinder.bindJdbcType;

/// Binds the entity version property.
///
/// Version binding is currently part of the entity member phase, but it is kept
/// separate from normal attribute binding because the mapping model stores the
/// version property in dedicated `RootClass` state and forces its column to be
/// non-nullable.
///
/// @since 9.0
/// @author Steve Ebersole
public class VersionBinder {
	public static void bindVersion(
			AttributeMetadata attributeMetadata,
			EntityTypeMetadata managedType,
			RootClass typeBinding,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final Property property = new Property();
		property.setName( attributeMetadata.getName() );
		bindPropertyAccessor( attributeMetadata.getMember(), property );
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
