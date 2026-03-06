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
 * A {@link MeterBinder} implementation that provides Hibernate metrics. It exposes the
 * same statistics as would be exposed when calling {@link Statistics#logSummary()}.
 *
 * @deprecated Use {@link org.hibernate.orm.micrometer.HibernateMetrics} instead.
 */
@Deprecated(since = "7.3", forRemoval = true)
public class HibernateMetrics extends org.hibernate.orm.micrometer.HibernateMetrics {

	/**
	 * Create {@code HibernateMetrics} and bind to the specified meter registry.
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
	 * Create {@code HibernateMetrics} and bind to the specified meter registry.
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
		new org.hibernate.orm.micrometer.HibernateMetrics( sessionFactory, sessionFactoryName, tags ).bindTo( registry );
	}

	/**
	 * Create a {@code HibernateMetrics}.
	 *
	 * @param sessionFactory session factory to use
	 * @param sessionFactoryName session factory name as a tag value
	 * @param tags additional tags
	 */
	public HibernateMetrics(SessionFactory sessionFactory, String sessionFactoryName, Iterable<Tag> tags) {
		super(sessionFactory, sessionFactoryName, tags);
	}
}
