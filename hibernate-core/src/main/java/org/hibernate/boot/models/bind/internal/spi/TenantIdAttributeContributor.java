/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.spi;

import org.hibernate.annotations.TenantId;
import org.hibernate.type.BasicType;

/// Internal contributor for Hibernate's built-in `@TenantId` annotation.
///
/// @since 9.0
/// @author Steve Ebersole
public class TenantIdAttributeContributor implements AttributeBindingContributor<TenantId> {
	@Override
	public void contribute(
			TenantId annotation,
			AttributeBindingTarget target,
			BindingContributionContext context) {
		final String returnedClassName = target.usage().resolvedType().determineRawClass().getClassName();
		final BasicType<?> tenantIdType = context.typeConfiguration()
				.getBasicTypeRegistry()
				.getRegisteredType( returnedClassName );
		target.entity().tenantId( tenantIdType );
	}
}
