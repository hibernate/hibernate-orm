/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.stat.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * Factory for custom implementations of {@link StatisticsImplementor}.
 *
 * @author Steve Ebersole
 */
public interface StatisticsFactory {
	StatisticsImplementor buildStatistics(SessionFactoryImplementor sessionFactory);
}
