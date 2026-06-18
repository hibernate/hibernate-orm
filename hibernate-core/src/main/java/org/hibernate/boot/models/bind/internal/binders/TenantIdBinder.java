/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import org.hibernate.boot.models.bind.internal.materialize.TenantIdMappingMaterializer;
import org.hibernate.boot.models.bind.internal.model.TenantIdContribution;
import org.hibernate.boot.models.bind.internal.view.TenantIdContributionView;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.type.BasicType;
import org.hibernate.type.spi.TypeConfiguration;

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
		final TypeConfiguration typeConfiguration = bindingState.getTypeConfiguration();

		final MemberDetails memberDetails = attributeMetadata.getMember();
		final String returnedClassName = memberDetails.getType().determineRawClass().getClassName();
		final BasicType<?> tenantIdType = typeConfiguration
				.getBasicTypeRegistry()
				.getRegisteredType( returnedClassName );

		final TenantIdContribution contribution = new TenantIdContribution(
				managedType,
				attributeMetadata.getName(),
				memberDetails,
				tenantIdType
		);
		bindingState.getBootBindingModel().addTenantIdContribution( managedType, contribution );

		final Property property = new TenantIdMappingMaterializer().materializeTenantId(
				new TenantIdContributionView( contribution ),
				typeBinding,
				bindingOptions,
				bindingState,
				bindingContext
		);
		CustomMappingBinder.callAttributeBinders( memberDetails, typeBinding, property, bindingState, bindingContext );
	}
}
