/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.property.access.spi;

import org.hibernate.models.accessor.HibernateAccessorFactory;
import org.hibernate.service.Service;

/**
 * Service providing access to {@link HibernateAccessorFactory}.
 *
 * <p>The default implementation wraps the ByteBuddy factory in an ORM-specific
 * delegating factory ({@code OrmHibernateAccessorFactory}) that provides optimized
 * individual accessors and ORM-aware multi-value accessors with enhancement logic.
 *
 * <p>Quarkus and similar frameworks replace this service entirely with one backed
 * by build-time-generated accessors. Each ecosystem project (ORM, Validator, Search)
 * may need different multi-value behavior — the factory returned by this service
 * is specific to the current project's needs.
 */
public interface PropertyAccessorService extends Service {

	HibernateAccessorFactory hibernateAccessorFactory();
}
