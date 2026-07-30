/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.spi;

import jakarta.annotation.Nullable;
import jakarta.persistence.AccessType;

import org.hibernate.models.spi.ClassDetails;

/// Read-only categorized declaration of an embeddable Java type.
///
/// A declaration is usage-independent. Its contextual access strategy,
/// type-variable resolution, and attributes are exposed by
/// [EmbeddableUsageMetadata].
///
/// @since 9.0
/// @author Steve Ebersole
public interface EmbeddableTypeMetadata {
	/// The embeddable source class.
	ClassDetails getClassDetails();

	/// The access strategy explicitly declared by the embeddable, or `null`
	/// when access is inherited from the usage site.
	@Nullable
	AccessType getExplicitAccessType();
}
