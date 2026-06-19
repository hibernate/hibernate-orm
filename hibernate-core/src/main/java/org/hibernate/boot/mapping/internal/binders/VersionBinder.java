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
import org.hibernate.boot.mapping.internal.model.AttributeDeclarationBinding;
import org.hibernate.boot.mapping.internal.model.AttributeUsageBinding;
import org.hibernate.boot.mapping.internal.model.BasicValueIntent;
import org.hibernate.boot.mapping.internal.model.IdentifiableAttributeDeclarationBinding;
import org.hibernate.boot.mapping.internal.model.ManagedTypeBinding;
import org.hibernate.boot.mapping.internal.model.StandardAttributeUsageBinding;
import org.hibernate.boot.mapping.internal.model.VersionContribution;
import org.hibernate.boot.mapping.internal.view.VersionContributionView;
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
		final AttributeUsageBinding usageBinding = createVersionUsage(
				attributeMetadata,
				managedType,
				bindingState,
				bindingContext
		);
		final var contribution = new VersionContribution(
				managedType,
				usageBinding.attributeName(),
				usageBinding.member(),
				usageBinding.basicValueIntent()
		);
		bindingState.getBootBindingModel().addVersionContribution( managedType, contribution );
		materializeVersion(
				new VersionContributionView( contribution ),
				typeBinding,
				bindingOptions,
				bindingState,
				bindingContext
		);
	}

	private static void materializeVersion(
			VersionContributionView contribution,
			RootClass typeBinding,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final Property property = new PropertyMappingMaterializer().createProperty(
				contribution.attributeName(),
				contribution.member()
		);
		typeBinding.setVersion( property );
		typeBinding.addProperty( property );

		final MemberDetails memberDetails = contribution.member();
		new BasicValueMappingMaterializer().materializeVersionBasicValue(
				memberDetails,
				contribution.valueIntent(),
				property,
				typeBinding.getRootTable(),
				bindingOptions,
				bindingState,
				bindingContext
		);
		CustomMappingBinder.callAttributeBinders( memberDetails, typeBinding, property, bindingState, bindingContext );
	}

	private static AttributeUsageBinding createVersionUsage(
			AttributeMetadata attributeMetadata,
			EntityTypeMetadata managedType,
			BindingState bindingState,
			BindingContext bindingContext) {
		final ManagedTypeBinding managedTypeBinding = bindingState.getBootBindingModel()
				.getManagedTypeBinding( managedType.getClassDetails() );
		if ( managedTypeBinding == null ) {
			throw new IllegalStateException(
					"Managed type binding was not registered before version binding - "
							+ managedType.getClassDetails().getName()
			);
		}

		final String attributeName = attributeMetadata.getName();
		AttributeDeclarationBinding declarationBinding = bindingState.getBootBindingModel()
				.findAttributeDeclaration( managedType.getClassDetails(), attributeName );
		if ( declarationBinding == null ) {
			declarationBinding = IdentifiableAttributeDeclarationBinding.from(
					attributeMetadata,
					managedTypeBinding,
					managedTypeBinding,
					attributeMetadata.getMember(),
					managedType.getAccessType(),
					attributeMetadata.getNature(),
					managedType.getClassDetails().getName() + "." + attributeName,
					attributeName
			);
			managedTypeBinding.addDeclaredAttribute( declarationBinding );
		}

		final AttributeUsageBinding usageBinding = new StandardAttributeUsageBinding(
				declarationBinding,
				managedTypeBinding,
				attributeMetadata.getMember(),
				attributeMetadata.getMember().resolveRelativeType( managedType.getClassDetails() ),
				managedType.getClassDetails().getName() + "." + attributeName,
				attributeName,
				attributeMetadata.getNature(),
				BasicValueIntent.fromAttribute( attributeMetadata.getMember(), bindingState, bindingContext )
		);
		managedTypeBinding.addAttributeUsage( usageBinding );
		return usageBinding;
	}
}
