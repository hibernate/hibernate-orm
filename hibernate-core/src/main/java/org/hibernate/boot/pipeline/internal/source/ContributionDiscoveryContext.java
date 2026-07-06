/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal.source;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;

/// Context for discovering mapping-source contributions from archives and
/// classpath resources.
///
/// @since 9.0
/// @author Steve Ebersole
public record ContributionDiscoveryContext(
		ClassLoaderService classLoaderService) {
}
