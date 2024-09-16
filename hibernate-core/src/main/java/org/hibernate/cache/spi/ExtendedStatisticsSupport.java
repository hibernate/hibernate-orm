/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
