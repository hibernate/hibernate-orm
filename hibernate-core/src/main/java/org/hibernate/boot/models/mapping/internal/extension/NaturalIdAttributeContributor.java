/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.mapping.internal.extension;

import org.hibernate.annotations.NaturalId;

/// Internal contributor for Hibernate's built-in `@NaturalId` annotation.
///
/// @since 9.0
/// @author Steve Ebersole
public class NaturalIdAttributeContributor implements AttributeBindingContributor<NaturalId> {
	@Override
	public void contribute(
			NaturalId annotation,
			AttributeBindingTarget target,
			BindingContributionContext context) {
		target.options().naturalId( annotation.mutable() );
	}
}
