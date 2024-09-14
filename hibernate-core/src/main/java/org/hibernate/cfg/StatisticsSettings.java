/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;

import org.hibernate.stat.spi.StatisticsFactory;

/**
 * @author Steve Ebersole
 */
public interface StatisticsSettings {
	/**
	 * When enabled, specifies that {@linkplain org.hibernate.stat.Statistics statistics}
	 * should be collected.
	 *
	 * @settingDefault {@code false}
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyStatisticsSupport(boolean)
	 */
	String GENERATE_STATISTICS = "hibernate.generate_statistics";

	/**
	 * When statistics are {@linkplain #GENERATE_STATISTICS enabled}, names the
	 * {@link StatisticsFactory} to use.  Recognizes a class name as well as an instance of
	 * {@link StatisticsFactory}.
	 * <p/>
	 * Allows customization of how the Hibernate Statistics are collected.
	 */
	String STATS_BUILDER = "hibernate.stats.factory";

	/**
	 * This setting controls the number of {@link org.hibernate.stat.QueryStatistics}
	 * entries that will be stored by the Hibernate {@link org.hibernate.stat.Statistics}
	 * object.
	 * <p>
	 * The default value is {@value org.hibernate.stat.Statistics#DEFAULT_QUERY_STATISTICS_MAX_SIZE}.
	 *
	 * @since 5.4
	 *
	 * @see org.hibernate.stat.Statistics#getQueries()
	 */
	String QUERY_STATISTICS_MAX_SIZE = "hibernate.statistics.query_max_size";
}
