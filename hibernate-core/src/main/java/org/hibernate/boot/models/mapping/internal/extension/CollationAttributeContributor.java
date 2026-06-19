/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.mapping.internal.extension;

import org.hibernate.annotations.Collate;

/// Internal contributor for Hibernate's built-in `@Collate` annotation.
///
/// @since 9.0
/// @author Steve Ebersole
public class CollationAttributeContributor implements AttributeBindingContributor<Collate> {
	@Override
	public void contribute(
			Collate annotation,
			AttributeBindingTarget target,
			BindingContributionContext context) {
		target.selectables().collation( annotation.value() );
	}
}
