/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.named.spi;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.Incubating;
import org.hibernate.boot.spi.MetadataImplementor;

/// Restricted view for resolving an HQL or native selection query used as a
/// custom entity or collection loader.
///
/// Stored procedures and mutation queries are not valid loader queries and are
/// deliberately excluded from this contract.
///
/// @since 9.0
/// @author Steve Ebersole
@Incubating
public interface NamedLoaderQueryResolver {
	/// Resolve the named selection query used by a custom loader.
	///
	/// @return The resolved loader-query memento, or `null` if no HQL or
	/// native query is registered under the given name.
	@Nullable
	NamedSelectionMemento<?> resolveLoaderQuery(
			@Nonnull MetadataImplementor bootMetamodel,
			@Nonnull String registrationName);
}
