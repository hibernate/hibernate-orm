/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi;

/**
 * Defines the contract for a cache region that stores timestamps.
 * The timestamps are used to manage query results with respect to
 * staleness of the underlying tables (sometimes called "query spaces"
 * or "table spaces").
 *
 * @author Steve Ebersole
 */
public interface TimestampsRegion extends DirectAccessRegion {
}
