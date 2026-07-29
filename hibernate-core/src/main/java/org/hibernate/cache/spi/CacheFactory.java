/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.JavaServiceLoadable;

/// Factory for the cache facade owned by a SessionFactory.
///
/// Implementations are discovered as Java services.  The factory is selected
/// during SessionFactory construction and invoked only after the
/// SessionFactory reference is available.
///
/// @since 9.0
/// @author Steve Ebersole
@JavaServiceLoadable
public interface CacheFactory {
	CacheImplementor buildCache(SessionFactoryImplementor sessionFactory);
}
