/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal;

import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.boot.mapping.internal.categorize.CategorizedDomainModel;
import org.hibernate.boot.spi.MetadataImplementor;

/// Resolved ORM boot metadata and associated boot-model products.
/// This product keeps the ORM metadata handoff explicit while preserving the
/// intermediate boot-model products needed by later SessionFactory bootstrap
/// experiments.
///
/// @since 9.0
/// @author Steve Ebersole
public record ResolvedMetadata(
		MetadataImplementor metadata,
		CategorizedDomainModel categorizedDomainModel,
		BindingState bindingState) {
}
