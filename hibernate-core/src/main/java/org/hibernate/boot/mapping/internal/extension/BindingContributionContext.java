/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.extension;

import org.hibernate.boot.mapping.internal.context.BindingContext;
import org.hibernate.boot.mapping.internal.context.BindingOptions;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.type.spi.TypeConfiguration;

/// Internal context passed to binding-layer contributors.
///
/// This keeps the contributor method shape close to a future public SPI while
/// still giving built-in contributors access to ORM's current binding services.
///
/// @since 9.0
/// @author Steve Ebersole
public record BindingContributionContext(
		BindingOptions bindingOptions,
		BindingState bindingState,
		BindingContext bindingContext) {
	public TypeConfiguration typeConfiguration() {
		return bindingState.getTypeConfiguration();
	}
}
