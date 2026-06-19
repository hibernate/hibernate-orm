/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import org.hibernate.boot.mapping.internal.context.BindingContext;
import org.hibernate.boot.mapping.internal.context.BindingOptions;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.boot.mapping.internal.materialize.BasicValueMappingMaterializer;
import org.hibernate.boot.mapping.internal.materialize.PropertyMappingMaterializer;
import org.hibernate.boot.mapping.internal.categorize.AttributeMetadata;
import org.hibernate.boot.mapping.internal.categorize.EntityTypeMetadata;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.models.spi.MemberDetails;

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
		final Property property = new PropertyMappingMaterializer().createProperty(
				attributeMetadata.getName(),
				attributeMetadata.getMember()
		);
		typeBinding.setVersion( property );
		typeBinding.addProperty( property );

		final MemberDetails memberDetails = attributeMetadata.getMember();
		new BasicValueMappingMaterializer().createVersionBasicValue(
				memberDetails,
				property,
				typeBinding.getRootTable(),
				bindingOptions,
				bindingState,
				bindingContext
		);
		CustomMappingBinder.callAttributeBinders( memberDetails, typeBinding, property, bindingState, bindingContext );
	}
}
