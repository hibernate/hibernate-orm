/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.cfg.spi;

/**
 * Specialized DomainDataCachingConfig describing the requested
 * caching config for the natural-id data of a particular entity (hierarchy)
 *
 * @author Steve Ebersole
 */
public interface NaturalIdDataCachingConfig extends DomainDataCachingConfig {
}
