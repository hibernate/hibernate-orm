/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;

/**
 * @author Thomas Wearmouth
 */
public interface AgroalSettings {

	/**
	 * A setting prefix used to indicate settings that target the {@code hibernate-agroal} integration.
	 */
	String AGROAL_CONFIG_PREFIX = "hibernate.agroal";

	/**
	 * The maximum size of the connection pool.
	 * <p>
	 * There is no default, this setting is mandatory.
	 */
	String AGROAL_MAX_SIZE = AGROAL_CONFIG_PREFIX + ".maxSize";

	/**
	 * The ninimum size of the connection pool.
	 * <p>
	 * The default is zero.
	 */
	String AGROAL_MIN_SIZE = AGROAL_CONFIG_PREFIX + ".minSize";

	/**
	 * Initial size of the connection pool.
	 * <p>
	 * The default is zero.
	 */
	String AGROAL_INITIAL_SIZE = AGROAL_CONFIG_PREFIX + ".initialSize";

	/**
	 * The maximum amount of time a connection can live, after which it is evicted.
	 * <p>
	 * The default is zero, resulting in no restriction on connection lifetime.
	 * <p>
	 * Parsed as a {@link java.time.Duration}.
	 *
	 * @see java.time.Duration#parse(CharSequence)
	 */
	String AGROAL_MAX_LIFETIME = AGROAL_CONFIG_PREFIX + ".maxLifetime";

	/**
	 * The maximum amount of time a connection can remain out of the pool, after
	 * which it is reported as a leak.
	 * <p>
	 * The default is zero, resulting in no checks for connection leaks.
	 * <p>
	 * Parsed as a {@link java.time.Duration}.
	 *
	 * @see java.time.Duration#parse(CharSequence)
	 */
	String AGROAL_LEAK_TIMEOUT = AGROAL_CONFIG_PREFIX + ".leakTimeout";

	/**
	 * The maximum amount of time a connection can remain idle, after which it is evicted.
	 * <p>
	 * The default is zero, resulting in connections never being considered idle.
	 * <p>
	 * Parsed as a {@link java.time.Duration}.
	 *
	 * @see java.time.Duration#parse(CharSequence)
	 */
	String AGROAL_IDLE_TIMEOUT = AGROAL_CONFIG_PREFIX + ".reapTimeout";

	/**
	 * The maximum amount of time a thread can wait for a connection, after which an
	 * exception is thrown instead.
	 * <p>
	 * The default is zero, resulting in threads waiting indefinitely.
	 * <p>
	 * Parsed as a {@link java.time.Duration}.
	 *
	 * @see java.time.Duration#parse(CharSequence)
	 */
	String AGROAL_ACQUISITION_TIMEOUT = AGROAL_CONFIG_PREFIX + ".acquisitionTimeout";

	/**
	 * Background validation is executed at intervals of this value.
	 * <p>
	 * The default is zero, resulting in background validation not being performed.
	 * <p>
	 * Parsed as a {@link java.time.Duration}.
	 *
	 * @see java.time.Duration#parse(CharSequence)
	 */
	String AGROAL_VALIDATION_TIMEOUT = AGROAL_CONFIG_PREFIX + ".validationTimeout";

	/**
	 * A foreground validation is executed if a connection has been idle in the pool
	 * for longer than this value.
	 * <p>
	 * The default is zero, resulting in foreground validation not being performed.
	 * <p>
	 * Parsed as a {@link java.time.Duration}.
	 *
	 * @see java.time.Duration#parse(CharSequence)
	 */
	String AGROAL_IDLE_VALIDATION_TIMEOUT = AGROAL_CONFIG_PREFIX + ".idleValidation";

	/**
	 * An SQL command to be executed when a connection is created.
	 */
	String AGROAL_INITIAL_SQL = AGROAL_CONFIG_PREFIX + ".initialSQL";

	/**
	 * If {@code true}, connections will be flushed whenever they return to the pool.
	 * <p>
	 * The default is {@code false}.
	 *
	 * @since agroal-api 1.6
	 */
	String AGROAL_FLUSH_ON_CLOSE = AGROAL_CONFIG_PREFIX + ".flushOnClose";

	/**
	 * If {@code true}, connections will receive foreground validation on every acquisition
	 * regardless of {@link AgroalSettings#AGROAL_IDLE_VALIDATION_TIMEOUT}.
	 * <p>
	 * The default is {@code false}.
	 *
	 * @since agroal-api 2.3
	 */
	String AGROAL_VALIDATE_ON_BORROW = AGROAL_CONFIG_PREFIX + ".validateOnBorrow";
}
