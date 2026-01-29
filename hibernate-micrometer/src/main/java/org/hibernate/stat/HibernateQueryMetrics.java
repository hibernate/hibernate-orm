/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.stat;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.hibernate.SessionFactory;

/**
 * A {@link MeterBinder} implementation that provides Hibernate query metrics. It exposes the
 * same statistics as would be exposed when calling {@link Statistics#getQueryStatistics(String)}.
 * Note that only SELECT queries are recorded in {@link QueryStatistics}.
 * <p>
 * Be aware of the potential for high cardinality of unique Hibernate queries executed by your
 * application when considering using this {@link MeterBinder}.
 *
 * @deprecated Use {@link org.hibernate.orm.micrometer.HibernateQueryMetrics} instead.
 */
@Deprecated(since = "7.3", forRemoval = true)
public class HibernateQueryMetrics extends org.hibernate.orm.micrometer.HibernateQueryMetrics {

	/**
	 * Create {@code HibernateQueryMetrics} and bind to the specified meter registry.
	 *
	 * @param registry meter registry to use
	 * @param sessionFactory session factory to use
	 * @param sessionFactoryName session factory name as a tag value
	 * @param tags additional tags
	 */
	public static void monitor(
			MeterRegistry registry,
			SessionFactory sessionFactory,
			String sessionFactoryName,
			String... tags) {
		monitor( registry, sessionFactory, sessionFactoryName, Tags.of( tags ) );
	}

	/**
	 * Create {@code HibernateQueryMetrics} and bind to the specified meter registry.
	 *
	 * @param registry meter registry to use
	 * @param sessionFactory session factory to use
	 * @param sessionFactoryName session factory name as a tag value
	 * @param tags additional tags
	 */
	public static void monitor(
			MeterRegistry registry,
			SessionFactory sessionFactory,
			String sessionFactoryName,
			Iterable<Tag> tags) {
		new HibernateQueryMetrics( sessionFactory, sessionFactoryName, tags ).bindTo( registry );
	}

	/**
	 * Create a {@code HibernateQueryMetrics}.
	 *
	 * @param sessionFactory session factory to use
	 * @param sessionFactoryName session factory name as a tag value
	 * @param tags additional tags
	 */
	public HibernateQueryMetrics(SessionFactory sessionFactory, String sessionFactoryName, Iterable<Tag> tags) {
		super( sessionFactory, sessionFactoryName, tags );
	}
}
