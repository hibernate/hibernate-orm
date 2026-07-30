/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi;

import org.hibernate.service.JavaServiceLoadable;

/// Factory for the cache facade owned by a SessionFactory.
///
/// Implementations are discovered as Java services.  The factory is selected
/// during SessionFactory construction and invoked only after the
/// SessionFactory reference and cache-region configurations are available.
/// The returned cache must be fully initialized and ready for use.
///
/// @since 9.0
/// @author Steve Ebersole
@JavaServiceLoadable
public interface CacheFactory {
	CacheImplementor buildCache(CacheConstructionContext constructionContext);
}
