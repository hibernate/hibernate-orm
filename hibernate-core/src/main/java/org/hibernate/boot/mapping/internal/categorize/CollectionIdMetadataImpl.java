/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.categorize;

import jakarta.annotation.Nullable;

import org.hibernate.annotations.CollectionId;
import org.hibernate.boot.model.IdentifierGeneratorRegistration;

/// Categorized metadata for the synthetic basic value which identifies an
/// id-bag row.
///
/// A `null` registration means the collection id uses a local or legacy
/// strategy which still requires collection-specific resolution.
///
/// @since 9.0
/// @author Steve Ebersole
public record CollectionIdMetadataImpl(
		CollectionId source,
		@Nullable IdentifierGeneratorRegistration generatorRegistration)
		implements org.hibernate.boot.mapping.spi.CollectionIdMetadata {
}
