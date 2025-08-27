/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi;

/**
 * Optional contract for a {@link Region} defining support for extra statistic information.
 *
 * @author Steve Ebersole
 */
public interface ExtendedStatisticsSupport {
	long getElementCountInMemory();

	long getElementCountOnDisk();

	long getSizeInMemory();
}
