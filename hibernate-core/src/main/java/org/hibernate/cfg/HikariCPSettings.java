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
public interface HikariCPSettings {

	/**
	 * A setting prefix used to indicate settings that target the {@code hibernate-hikaricp} integration.
	 */
	String HIKARI_CONFIG_PREFIX = "hibernate.hikari";

	/**
	 * The maximum size of the connection pool.
	 * <p>
	 * The default is 10.
	 */
	String HIKARI_MAX_SIZE = HIKARI_CONFIG_PREFIX + ".maximumPoolSize";

	/**
	 * The minimum number of idle connections to try and maintain in the pool.
	 * <p>
	 * The default is the same as {@link HikariCPSettings#HIKARI_MAX_SIZE}.
	 */
	String HIKARI_MIN_IDLE_SIZE = HIKARI_CONFIG_PREFIX + ".minimumIdle";

	/**
	 * The maximum amount of time a connection can live, after which it is evicted.
	 * <p>
	 * The default is 1800000 milliseconds (30 minutes).
	 */
	String HIKARI_MAX_LIFETIME = HIKARI_CONFIG_PREFIX + ".maxLifetime";

	/**
	 * The maximum amount of time a connection can remain out of the pool, after
	 * which it is reported as a leak.
	 * <p>
	 * The default is 0 milliseconds, resulting in no checks for connection leaks.
	 */
	String HIKARI_LEAK_TIMEOUT = HIKARI_CONFIG_PREFIX + ".leakDetectionThreshold";

	/**
	 * The maximum amount of time a connection can remain idle, after which it is evicted.
	 * <p>
	 * The default is 600000 milliseconds (10 minutes).
	 */
	String HIKARI_IDLE_TIMEOUT = HIKARI_CONFIG_PREFIX + ".idleTimeout";

	/**
	 * The maximum amount of time a thread can wait for a connection, after which an
	 * exception is thrown instead.
	 * <p>
	 * The default is 30000 milliseconds (30 seconds).
	 */
	String HIKARI_ACQUISITION_TIMEOUT = HIKARI_CONFIG_PREFIX + ".connectionTimeout";

	/**
	 * The maximum amount of time that a connection will be tested for aliveness. Must
	 * be lower than {@link HikariCPSettings#HIKARI_ACQUISITION_TIMEOUT}.
	 * <p>
	 * The default is 5000 milliseconds (5 seconds).
	 */
	String HIKARI_VALIDATION_TIMEOUT = HIKARI_CONFIG_PREFIX + ".validationTimeout";

	/**
	 * The maximum amount of time the application thread can wait to attempt to acquire
	 * an initial connection. Applied after {@link HikariCPSettings#HIKARI_ACQUISITION_TIMEOUT}.
	 * <p>
	 * The default is 1 millisecond.
	 */
	String HIKARI_INITIALIZATION_TIMEOUT = HIKARI_CONFIG_PREFIX + ".initializationFailTimeout";

	/**
	 * How often connections will attempt to be kept alive to prevent a timeout.
	 * <p>
	 * The default is 0 milliseconds, resulting in no keep-alive behaviour.
	 */
	String HIKARI_KEEPALIVE_TIME = HIKARI_CONFIG_PREFIX + ".keepaliveTime";

	/**
	 * An SQL command to be executed when a connection is created.
	 */
	String HIKARI_INITIAL_SQL = HIKARI_CONFIG_PREFIX + ".connectionInitSql";

	/**
	 * A user-defined name for the pool that appears in logging.
	 * <p>
	 * The default is auto-generated.
	 */
	String HIKARI_POOL_NAME = HIKARI_CONFIG_PREFIX + ".poolName";

	/**
	 * If {@code true}, connections obtained from the pool are in read-only mode
	 * by default.
	 * <p>
	 * Some databases do not support read-only mode while some will provide query
	 * optimizations when a connection is in read-only mode.
	 * <p>
	 * The default is {@code false}.
	 */
	String HIKARI_READ_ONLY = HIKARI_CONFIG_PREFIX + ".readOnly";

	/**
	 * If {@code true}, internal pool queries (such as keep-alives) will be isolated
	 * in their own transaction.
	 * <p>
	 * Only applies if {@link AvailableSettings#AUTOCOMMIT} is disabled.
	 * <p>
	 * The default is {@code false}.
	 */
	String HIKARI_ISOLATE_INTERNAL_QUERIES = HIKARI_CONFIG_PREFIX + ".isolateInternalQueries";
}
