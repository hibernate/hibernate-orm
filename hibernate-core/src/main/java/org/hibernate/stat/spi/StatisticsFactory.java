/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.stat.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * Factory for custom implementations of {@link StatisticsImplementor}.
 * <p>
 * A custom implementation may be selected via the configuration property
 * {@value org.hibernate.cfg.StatisticsSettings#STATS_BUILDER}.
 *
 * @author Steve Ebersole
 */
public interface StatisticsFactory {
	StatisticsImplementor buildStatistics(SessionFactoryImplementor sessionFactory);
}
