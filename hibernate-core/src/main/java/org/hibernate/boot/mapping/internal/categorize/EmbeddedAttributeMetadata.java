/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.categorize;

/// Categorized metadata for an embedded-valued attribute.
///
/// @since 9.0
/// @author Steve Ebersole
public interface EmbeddedAttributeMetadata extends SingularAttributeMetadata {
	@Override
	EmbeddedValueMetadata getValue();

	default EmbeddableUsageMetadata getEmbeddableUsage() {
		return getValue().getEmbeddableUsage();
	}
}
