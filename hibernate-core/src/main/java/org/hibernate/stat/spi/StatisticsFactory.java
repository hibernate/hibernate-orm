/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.stat.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.JavaServiceLoadable;

/**
 * Factory for custom implementations of {@link StatisticsImplementor}.
 * <p>
 * A custom implementation may be selected either by setting the configuration
 * property {@value org.hibernate.cfg.StatisticsSettings#STATS_BUILDER}, or by
 * registering it as a {@linkplain java.util.ServiceLoader Java service}.
 *
 * @author Steve Ebersole
 */
@JavaServiceLoadable
public interface StatisticsFactory {
	StatisticsImplementor buildStatistics(SessionFactoryImplementor sessionFactory);
}
