/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;

import java.util.function.Supplier;

import org.hibernate.Interceptor;

/**
 * @author Steve Ebersole
 */
public interface SessionEventSettings {

	/**
	 * Controls whether {@linkplain org.hibernate.stat.SessionStatistics session metrics}
	 * should be {@linkplain org.hibernate.engine.internal.StatisticalLoggingSessionEventListener
	 * logged} for any session in which statistics are being collected.
	 * <p>
	 * By default, logging of session metrics is disabled unless {@link StatisticsSettings#GENERATE_STATISTICS}
	 * is enabled.
	 *
	 * @settingDefault Defined by {@link StatisticsSettings#GENERATE_STATISTICS}
	 */
	String LOG_SESSION_METRICS = "hibernate.session.events.log";

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
}
