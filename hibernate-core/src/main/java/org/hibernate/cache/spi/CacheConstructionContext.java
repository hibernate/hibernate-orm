/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi;

import java.util.Set;

import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.engine.spi.SessionFactoryImplementor;

/// Inputs available to a [CacheFactory] while constructing the cache facade
/// owned by a SessionFactory.
///
/// The region configurations are fully prepared before cache construction.
/// A factory must consume these inputs and return a fully initialized
/// [CacheImplementor].
///
/// @since 9.0
/// @author Steve Ebersole
public interface CacheConstructionContext {
	/// The SessionFactory which will own the cache.
	SessionFactoryImplementor getSessionFactory();

	/// The immutable domain-data region configurations prepared from the
	/// resolved mapping.
	Set<DomainDataRegionConfig> getCacheRegionConfigs();
}
