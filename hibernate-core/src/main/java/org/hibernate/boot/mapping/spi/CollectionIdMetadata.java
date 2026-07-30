/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.spi;

import jakarta.annotation.Nullable;

import org.hibernate.annotations.CollectionId;
import org.hibernate.boot.model.IdentifierGeneratorRegistration;

/// Categorized description of an id-bag collection identifier.
///
/// @since 9.0
/// @author Steve Ebersole
public interface CollectionIdMetadata {
	/// The source `@CollectionId` annotation.
	CollectionId source();

	/// The resolved generator registration, or `null` when a local or legacy
	/// strategy still requires collection-specific resolution.
	@Nullable
	IdentifierGeneratorRegistration generatorRegistration();
}
