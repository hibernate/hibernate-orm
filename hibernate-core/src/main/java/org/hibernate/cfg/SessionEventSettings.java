/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cfg;

import java.util.function.Supplier;

import org.hibernate.Interceptor;

/**
 * @author Steve Ebersole
 */
public interface SessionEventSettings {

	/**
	 * Defines a default {@link org.hibernate.SessionEventListener} to be applied to
	 * newly-opened {@link org.hibernate.Session}s.
	 */
	String AUTO_SESSION_EVENTS_LISTENER = "hibernate.session.events.auto";

	/**
	 * Specifies an {@link org.hibernate.Interceptor} implementation associated with
	 * the {@link org.hibernate.SessionFactory} and propagated to each {@code Session}
	 * created from the {@code SessionFactory}. Either:
	 * <ul>
	 *     <li>an instance of {@code Interceptor},
	 *     <li>a {@link Class} representing a class that implements {@code Interceptor}, or
	 *     <li>the name of a class that implements {@code Interceptor}.
	 * </ul>
	 * <p>
	 * This setting identifies an {@code Interceptor} which is effectively a singleton
	 * across all the sessions opened from the {@code SessionFactory} to which it is
	 * applied; the same instance will be passed to each {@code Session}. If there
	 * should be a separate instance of {@code Interceptor} for each {@code Session},
	 * use {@link #SESSION_SCOPED_INTERCEPTOR} instead.
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyInterceptor(Interceptor)
	 *
	 * @since 5.0
	 */
	String INTERCEPTOR = "hibernate.session_factory.interceptor";

	/**
	 * Specifies an {@link org.hibernate.Interceptor} implementation associated with
	 * the {@link org.hibernate.SessionFactory} and propagated to each {@code Session}
	 * created from the {@code SessionFactory}. Either:
	 * <ul>
	 *     <li>a {@link Class} representing a class that implements {@code Interceptor},
	 *     <li>the name of a class that implements {@code Interceptor}, or
	 *     <li>an instance of {@link Supplier} used to obtain the interceptor.
	 * </ul>
	 * <p>
	 * Note that this setting cannot specify an {@code Interceptor} instance.
	 * <p>
	 * This setting identifies an {@code Interceptor} implementation that is to be
	 * applied to every {@code Session} opened from the {@code SessionFactory}, but
	 * unlike {@link #INTERCEPTOR}, a separate instance created for each {@code Session}.
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyStatelessInterceptor(Class)
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyStatelessInterceptor(Supplier)
	 *
	 * @since 5.2
	 */
	String SESSION_SCOPED_INTERCEPTOR = "hibernate.session_factory.session_scoped_interceptor";

	/**
	 * @deprecated This setting is now ignored. Enable the log category
	 *             {@code org.hibernate.session.metrics} to automatically
	 *             collect and log session-level metrics.
	 */
	@Deprecated(since = "7", forRemoval = true)
	String LOG_SESSION_METRICS = "hibernate.session.events.log";
}
