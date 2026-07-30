/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import org.hibernate.boot.mapping.internal.materialize.TenantIdMappingMaterializer;
import org.hibernate.boot.mapping.internal.model.AttributeDeclarationBinding;
import org.hibernate.boot.mapping.internal.model.AttributeUsageBinding;
import org.hibernate.boot.mapping.internal.model.BasicValueIntent;
import org.hibernate.boot.mapping.internal.model.IdentifiableAttributeDeclarationBinding;
import org.hibernate.boot.mapping.internal.model.ManagedTypeBinding;
import org.hibernate.boot.mapping.internal.model.StandardAttributeUsageBinding;
import org.hibernate.boot.mapping.internal.model.TenantIdBinding;
import org.hibernate.boot.mapping.internal.context.BindingContext;
import org.hibernate.boot.mapping.internal.context.BindingOptions;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.boot.mapping.internal.categorize.AttributeMetadataImplementor;
import org.hibernate.boot.mapping.internal.categorize.EntityTypeMetadataImpl;
import org.hibernate.boot.mapping.internal.view.TenantIdBindingView;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.type.BasicType;

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
			AttributeMetadataImplementor attributeMetadata,
			EntityTypeMetadataImpl managedType,
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

		final String returnedClassName = usageBinding.resolvedType().determineRawClass().getClassName();
		final BasicType<?> tenantIdType = bindingState.getTypeConfiguration()
				.getBasicTypeRegistry()
				.getRegisteredType( returnedClassName );
		final var tenantIdBinding = new TenantIdBinding(
				managedType,
				usageBinding.attributeName(),
				usageBinding.member(),
				usageBinding.resolvedType(),
				usageBinding.basicValueIntent(),
				tenantIdType
		);
		bindingState.getBootBindingModel().addTenantIdBinding( managedType, tenantIdBinding );
		final Property property = new TenantIdMappingMaterializer().materializeTenantId(
				new TenantIdBindingView( tenantIdBinding ),
				typeBinding,
				bindingOptions,
				bindingState,
				bindingContext
		);
		bindingState.addAttributeCustomMapping(
				CustomMappingBinder.attributeBinding(
						memberDetails,
						typeBinding,
						property,
						bindingState,
						bindingContext
				)
		);
	}

	private static AttributeUsageBinding createTenantIdUsage(
			AttributeMetadataImplementor attributeMetadata,
			EntityTypeMetadataImpl managedType,
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
			bindingState.getBootBindingModel().addDeclaredAttribute(
					managedTypeBinding,
					declarationBinding
			);
		}

		final AttributeUsageBinding usageBinding = new StandardAttributeUsageBinding(
				declarationBinding,
				managedTypeBinding,
				attributeMetadata.getMember(),
				attributeMetadata.resolveAttributeType( managedType.getClassDetails() ),
				managedType.getClassDetails().getName() + "." + attributeName,
				attributeName,
				attributeMetadata.getNature(),
				BasicValueIntent.fromAttribute( attributeMetadata.getMember(), bindingState, bindingContext )
		);
		managedTypeBinding.addAttributeUsage( usageBinding );
		return usageBinding;
	}
}
