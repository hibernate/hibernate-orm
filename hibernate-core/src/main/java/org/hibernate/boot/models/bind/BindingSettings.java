/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind;

import jakarta.persistence.FetchType;

/// Settings needed while binding the categorized domain model.
///
/// This contract exposes only the mapping settings consumed by the binding phase;
/// bootstrap source collection and other pipeline settings remain internal to the
/// bootstrap pipeline.
///
/// @since 9.0
/// @author Steve Ebersole
public interface BindingSettings {
	FetchType defaultToOneFetchType();

	boolean createImplicitDiscriminatorsForJoinedInheritance();

	boolean ignoreExplicitDiscriminatorsForJoinedInheritance();
}
