/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import org.hibernate.annotations.TenantId;
import org.hibernate.boot.models.bind.internal.materialize.TenantIdMappingMaterializer;
import org.hibernate.boot.models.bind.internal.model.AttributeDeclarationBinding;
import org.hibernate.boot.models.bind.internal.model.AttributeUsageBinding;
import org.hibernate.boot.models.bind.internal.model.BasicValueIntent;
import org.hibernate.boot.models.bind.internal.model.IdentifiableAttributeDeclarationBinding;
import org.hibernate.boot.models.bind.internal.model.ManagedTypeBinding;
import org.hibernate.boot.models.bind.internal.model.StandardAttributeUsageBinding;
import org.hibernate.boot.models.bind.internal.spi.BindingContributionContext;
import org.hibernate.boot.models.bind.internal.spi.StandardAttributeBindingTarget;
import org.hibernate.boot.models.bind.internal.spi.TenantIdAttributeContributor;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.models.spi.MemberDetails;

/// Binds the source-model `@TenantId` attribute.
///
/// `@TenantId` contributes both a normal basic property and global filter
/// metadata.  The binder resolves the tenant-id type from the categorized
/// attribute and delegates the legacy mapping contribution to
/// [TenantIdMappingMaterializer].
///
/// @since 9.0
/// @author Steve Ebersole
public class TenantIdBinder {
	public static final String FILTER_NAME = TenantIdMappingMaterializer.FILTER_NAME;
	public static final String PARAMETER_NAME = TenantIdMappingMaterializer.PARAMETER_NAME;

	public static void bindTenantId(
			AttributeMetadata attributeMetadata,
			EntityTypeMetadata managedType,
			RootClass typeBinding,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final MemberDetails memberDetails = attributeMetadata.getMember();
		final AttributeUsageBinding usageBinding = createTenantIdUsage(
				attributeMetadata,
				managedType,
				bindingState,
				bindingContext
		);

		final var contributionContext = new BindingContributionContext(
				bindingOptions,
				bindingState,
				bindingContext
		);
		final var target = StandardAttributeBindingTarget.forEntityAttribute(
				managedType,
				usageBinding,
				typeBinding,
				contributionContext
		);
		new TenantIdAttributeContributor().contribute(
				memberDetails.getDirectAnnotationUsage( TenantId.class ),
				target,
				contributionContext
		);
		final Property property = target.contributedProperty();
		if ( property == null ) {
			throw new IllegalStateException(
					"@TenantId contributor did not materialize property `" + attributeMetadata.getName() + "`"
			);
		}
		CustomMappingBinder.callAttributeBinders( memberDetails, typeBinding, property, bindingState, bindingContext );
	}

	private static AttributeUsageBinding createTenantIdUsage(
			AttributeMetadata attributeMetadata,
			EntityTypeMetadata managedType,
			BindingState bindingState,
			BindingContext bindingContext) {
		final ManagedTypeBinding managedTypeBinding = bindingState.getBootBindingModel()
				.getManagedTypeBinding( managedType.getClassDetails() );
		if ( managedTypeBinding == null ) {
			throw new IllegalStateException(
					"Managed type binding was not registered before tenant-id binding - "
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
