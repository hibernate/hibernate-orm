/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache;

/**
 * Legacy (deprecated) namespace for the RegionFactory contract.
 *
 * @author Steve Ebersole
 *
 * @deprecated Moved, but still need this definition for ehcache
 */
@Deprecated(since="4")
public interface RegionFactory extends org.hibernate.cache.spi.RegionFactory {
}
