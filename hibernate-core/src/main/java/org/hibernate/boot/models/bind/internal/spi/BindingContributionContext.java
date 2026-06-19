/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.spi;

import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
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
