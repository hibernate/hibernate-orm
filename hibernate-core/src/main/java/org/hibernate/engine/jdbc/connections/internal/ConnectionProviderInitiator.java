/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.connections.internal;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProviderConfigurationException;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.internal.Helper;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import static java.sql.Connection.TRANSACTION_NONE;
import static java.sql.Connection.TRANSACTION_READ_COMMITTED;
import static java.sql.Connection.TRANSACTION_READ_UNCOMMITTED;
import static java.sql.Connection.TRANSACTION_REPEATABLE_READ;
import static java.sql.Connection.TRANSACTION_SERIALIZABLE;
import static org.hibernate.cfg.AgroalSettings.AGROAL_CONFIG_PREFIX;
import static org.hibernate.cfg.C3p0Settings.C3P0_CONFIG_PREFIX;
import static org.hibernate.cfg.HikariCPSettings.HIKARI_CONFIG_PREFIX;
import static org.hibernate.cfg.JdbcSettings.CONNECTION_PREFIX;
import static org.hibernate.cfg.JdbcSettings.CONNECTION_PROVIDER;
import static org.hibernate.cfg.JdbcSettings.CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT;
import static org.hibernate.cfg.JdbcSettings.DATASOURCE;
import static org.hibernate.cfg.JdbcSettings.DRIVER;
import static org.hibernate.cfg.JdbcSettings.ISOLATION;
import static org.hibernate.cfg.JdbcSettings.POOL_SIZE;
import static org.hibernate.cfg.JdbcSettings.URL;
import static org.hibernate.cfg.JdbcSettings.USER;
import static org.hibernate.cfg.SchemaToolingSettings.ENABLE_SYNONYMS;
import static org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentImpl.isMultiTenancyEnabled;
import static org.hibernate.internal.util.StringHelper.isBlank;
import static org.hibernate.internal.util.StringHelper.nullIfBlank;

/**
 * Instantiates and configures an appropriate {@link ConnectionProvider}.
 *
 * @author Gavin King
 * @author Steve Ebersole
 * @author Brett Meyer
 */
public class ConnectionProviderInitiator implements StandardServiceInitiator<ConnectionProvider> {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( ConnectionProviderInitiator.class );

	/**
	 * Singleton access
	 */
	public static final ConnectionProviderInitiator INSTANCE = new ConnectionProviderInitiator();

	/**
	 * The strategy for c3p0 connection pooling
	 */
	public static final String C3P0_STRATEGY = "c3p0";

	/**
	 * The strategy for hikari connection pooling
	 */
	public static final String HIKARI_STRATEGY = "hikari";

	/**
	 * The strategy for agroal connection pooling
	 */
	public static final String AGROAL_STRATEGY = "agroal";

	@Override
	public Class<ConnectionProvider> getServiceInitiated() {
		return ConnectionProvider.class;
	}

	@Override
	public ConnectionProvider initiateService(
			Map<String, Object> configurationValues,
			ServiceRegistryImplementor registry) {
		if ( isMultiTenancyEnabled( registry ) ) {
			// nothing to do, but given the separate hierarchies have to handle this here.
			return null;
		}

		final BeanContainer beanContainer = Helper.getBeanContainer( registry );
		final StrategySelector strategySelector = registry.requireService( StrategySelector.class );
		final Object explicitSetting = configurationValues.get( CONNECTION_PROVIDER );
		if ( explicitSetting != null ) {
			// if we are explicitly supplied a ConnectionProvider to use (in some form) -> use it..
			if ( explicitSetting instanceof ConnectionProvider provider) {
				return provider;
			}
			else if ( explicitSetting instanceof Class<?> providerClass ) {
				LOG.instantiatingExplicitConnectionProvider( providerClass.getName() );
				return instantiateExplicitConnectionProvider( connectionProviderClass( providerClass ), beanContainer );
			}
			else {
				final String providerName = nullIfBlank( explicitSetting.toString() );
				if ( providerName != null ) {
					return instantiateNamedConnectionProvider( providerName, strategySelector, beanContainer );
				}
			}
		}

		return instantiateConnectionProvider( configurationValues, strategySelector, beanContainer );
	}

	private static Class<? extends ConnectionProvider> connectionProviderClass(Class<?> providerClass) {
		if ( !ConnectionProvider.class.isAssignableFrom( providerClass ) ) {
			throw new ConnectionProviderConfigurationException( "Class '" + providerClass.getName()
																+ "' does not implement 'ConnectionProvider'" );
		}
		@SuppressWarnings("unchecked")
		final Class<? extends ConnectionProvider> connectionProviderClass =
				(Class<? extends ConnectionProvider>) providerClass;
		return connectionProviderClass;
	}

	private ConnectionProvider instantiateNamedConnectionProvider(
			String providerName, StrategySelector strategySelector, BeanContainer beanContainer) {
		LOG.instantiatingExplicitConnectionProvider( providerName );
		final Class<? extends ConnectionProvider> providerClass =
				strategySelector.selectStrategyImplementor( ConnectionProvider.class, providerName );
		try {
			return instantiateExplicitConnectionProvider( providerClass, beanContainer );
		}
		catch (Exception e) {
			throw new HibernateException( "Could not instantiate connection provider [" + providerName + "]", e );
		}
	}

	private ConnectionProvider instantiateConnectionProvider(
			Map<String, Object> configurationValues, StrategySelector strategySelector, BeanContainer beanContainer) {
		if ( configurationValues.containsKey( DATASOURCE ) ) {
			return new DataSourceConnectionProvider();
		}

		final Class<? extends ConnectionProvider> singleRegisteredProvider =
				getSingleRegisteredProvider( strategySelector );
		if ( singleRegisteredProvider != null ) {
			try {
				return singleRegisteredProvider.getConstructor().newInstance();
			}
			catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException | InstantiationException e) {
				throw new HibernateException( "Could not instantiate singular-registered ConnectionProvider", e );
			}
		}
		else if ( hasConfiguration( configurationValues, C3P0_CONFIG_PREFIX ) ) {
			return instantiateProvider( strategySelector, C3P0_STRATEGY );
		}
		else if ( hasConfiguration( configurationValues, HIKARI_CONFIG_PREFIX ) ) {
			return instantiateProvider( strategySelector, HIKARI_STRATEGY );
		}
		else if ( hasConfiguration( configurationValues, AGROAL_CONFIG_PREFIX ) ) {
			return instantiateProvider( strategySelector, AGROAL_STRATEGY );
		}
		else if ( configurationValues.containsKey( URL ) ) {
			return new DriverManagerConnectionProvider();
		}
		else {
			if ( beanContainer != null ) {
				return Helper.getBean(
					beanContainer,
					ConnectionProvider.class,
					true,
					true,
					this::noAppropriateConnectionProvider
				);
			}
			else {
				return noAppropriateConnectionProvider();
			}

		}
	}

	private ConnectionProvider noAppropriateConnectionProvider() {
		LOG.noAppropriateConnectionProvider();
		return new UserSuppliedConnectionProviderImpl();
	}

	private Class<? extends ConnectionProvider> getSingleRegisteredProvider(StrategySelector strategySelector) {
		final Collection<Class<? extends ConnectionProvider>> implementors =
				strategySelector.getRegisteredStrategyImplementors( ConnectionProvider.class );
		return implementors != null && implementors.size() == 1
				? implementors.iterator().next()
				: null;
	}

	private <T extends ConnectionProvider> T instantiateExplicitConnectionProvider(
			Class<T> providerClass, BeanContainer beanContainer) {
		try {
			if ( beanContainer != null ) {
				return Helper.getBean(
					beanContainer,
					providerClass,
					true,
					true,
					() -> {
						try {
							return providerClass.getConstructor().newInstance();
						}
						catch (Exception e) {
							throw new HibernateException( "Could not instantiate connection provider [" + providerClass.getName() + "]", e );
						}
					}
				);
			}
			else {
				return providerClass.getConstructor().newInstance();
			}
		}
		catch (Exception e) {
			throw new HibernateException( "Could not instantiate connection provider [" + providerClass.getName() + "]", e );
		}
	}

	private static ConnectionProvider instantiateProvider(StrategySelector selector, String strategy) {
		try {
			return selector.selectStrategyImplementor( ConnectionProvider.class, strategy ).getConstructor().newInstance();
		}
		catch ( Exception e ) {
			LOG.providerClassNotFound(strategy);
			return null;
		}
	}

	/**
	 * Build the connection properties capable of being passed to
	 * {@link java.sql.DriverManager#getConnection(String, Properties)} forms taking {@link Properties} argument.
	 * We seek out all keys in the given map which start with {@code hibernate.connection.}, using them to create
	 * a new {@link Properties} instance. The keys in this new {@link Properties} have the
	 * {@code hibernate.connection.} prefix trimmed.
	 *
	 * @param properties The map from which to build the connection specific properties.
	 *
	 * @return The connection properties.
	 */
	public static Properties getConnectionProperties(Map<String, Object> properties) {
		final Properties result = new Properties();
		for ( var entry : properties.entrySet() ) {
			if ( entry.getValue() instanceof String value ) {
				final String key = entry.getKey();
				if ( key.startsWith( CONNECTION_PREFIX ) ) {
					if ( SPECIAL_PROPERTIES.contains( key ) ) {
						if ( USER.equals( key ) ) {
							result.setProperty( "user", value );
						}
					}
					else {
						result.setProperty( key.substring(CONNECTION_PREFIX.length() + 1), value );
					}
				}
				else if ( CONDITIONAL_PROPERTIES.containsKey( key ) ) {
					result.setProperty( CONDITIONAL_PROPERTIES.get( key ), value );
				}
			}
		}
		return result;
	}

	private static final Set<String> SPECIAL_PROPERTIES;

	private static final Map<String, Integer> ISOLATION_VALUE_MAP;
	private static final Map<Integer, String> ISOLATION_VALUE_CONSTANT_NAME_MAP;
	private static final Map<Integer, String> ISOLATION_VALUE_NICE_NAME_MAP;

	static {
		SPECIAL_PROPERTIES = new HashSet<>();
		SPECIAL_PROPERTIES.add( DATASOURCE );
		SPECIAL_PROPERTIES.add( URL );
		SPECIAL_PROPERTIES.add( CONNECTION_PROVIDER );
		SPECIAL_PROPERTIES.add( POOL_SIZE );
		SPECIAL_PROPERTIES.add( ISOLATION );
		SPECIAL_PROPERTIES.add( DRIVER );
		SPECIAL_PROPERTIES.add( USER );
		SPECIAL_PROPERTIES.add( CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT );

		ISOLATION_VALUE_MAP = new ConcurrentHashMap<>();
		ISOLATION_VALUE_MAP.put( "TRANSACTION_NONE", TRANSACTION_NONE );
		ISOLATION_VALUE_MAP.put( "NONE", TRANSACTION_NONE );
		ISOLATION_VALUE_MAP.put( "TRANSACTION_READ_UNCOMMITTED", TRANSACTION_READ_UNCOMMITTED );
		ISOLATION_VALUE_MAP.put( "READ_UNCOMMITTED", TRANSACTION_READ_UNCOMMITTED );
		ISOLATION_VALUE_MAP.put( "TRANSACTION_READ_COMMITTED", TRANSACTION_READ_COMMITTED );
		ISOLATION_VALUE_MAP.put( "READ_COMMITTED", TRANSACTION_READ_COMMITTED );
		ISOLATION_VALUE_MAP.put( "TRANSACTION_REPEATABLE_READ", TRANSACTION_REPEATABLE_READ );
		ISOLATION_VALUE_MAP.put( "REPEATABLE_READ", TRANSACTION_REPEATABLE_READ );
		ISOLATION_VALUE_MAP.put( "TRANSACTION_SERIALIZABLE", TRANSACTION_SERIALIZABLE );
		ISOLATION_VALUE_MAP.put( "SERIALIZABLE", TRANSACTION_SERIALIZABLE );

		ISOLATION_VALUE_CONSTANT_NAME_MAP = new ConcurrentHashMap<>();
		ISOLATION_VALUE_CONSTANT_NAME_MAP.put( TRANSACTION_NONE, "TRANSACTION_NONE" );
		ISOLATION_VALUE_CONSTANT_NAME_MAP.put( TRANSACTION_READ_UNCOMMITTED, "TRANSACTION_READ_UNCOMMITTED" );
		ISOLATION_VALUE_CONSTANT_NAME_MAP.put( TRANSACTION_READ_COMMITTED, "TRANSACTION_READ_COMMITTED" );
		ISOLATION_VALUE_CONSTANT_NAME_MAP.put( TRANSACTION_REPEATABLE_READ, "TRANSACTION_REPEATABLE_READ" );
		ISOLATION_VALUE_CONSTANT_NAME_MAP.put( TRANSACTION_SERIALIZABLE, "TRANSACTION_SERIALIZABLE" );

		ISOLATION_VALUE_NICE_NAME_MAP = new ConcurrentHashMap<>();
		ISOLATION_VALUE_NICE_NAME_MAP.put( TRANSACTION_NONE, "NONE" );
		ISOLATION_VALUE_NICE_NAME_MAP.put( TRANSACTION_READ_UNCOMMITTED, "READ_UNCOMMITTED" );
		ISOLATION_VALUE_NICE_NAME_MAP.put( TRANSACTION_READ_COMMITTED, "READ_COMMITTED" );
		ISOLATION_VALUE_NICE_NAME_MAP.put( TRANSACTION_REPEATABLE_READ, "REPEATABLE_READ" );
		ISOLATION_VALUE_NICE_NAME_MAP.put( TRANSACTION_SERIALIZABLE, "SERIALIZABLE" );
	}

	// Connection properties (map value) that automatically need set if the
	// Hibernate property (map key) is available. Makes the assumption that
	// both settings use the same value type.
	private static final Map<String, String> CONDITIONAL_PROPERTIES =
			// Oracle requires that includeSynonyms=true in order for
			// getColumns() to work using a table synonym name.
			Map.of( ENABLE_SYNONYMS, "includeSynonyms" );

	public static Integer extractIsolation(Map<String,?> settings) {
		return interpretIsolation( settings.get( ISOLATION ) );
	}

	public static Integer interpretIsolation(Object setting) {
		if ( setting == null ) {
			return null;
		}
		else if ( setting instanceof Number number ) {
			final int isolationLevel = number.intValue();
			checkIsolationLevel( isolationLevel );
			return isolationLevel;
		}
		else {
			final String string = setting.toString();
			if ( isBlank( string ) ) {
				return null;
			}
			else if ( ISOLATION_VALUE_MAP.containsKey( string ) ) {
				return ISOLATION_VALUE_MAP.get( string );
			}
			else {
				// it could be a String representation of the isolation numeric value
				try {
					final int isolationLevel = Integer.parseInt( string );
					checkIsolationLevel( isolationLevel );
					return isolationLevel;
				}
				catch (NumberFormatException ignore) {
					throw new ConnectionProviderConfigurationException( "Unknown transaction isolation level: '" + string + "'" );
				}
			}
		}
	}

	private static void checkIsolationLevel(int isolationLevel) {
		if ( !ISOLATION_VALUE_CONSTANT_NAME_MAP.containsKey( isolationLevel ) ) {
			throw new ConnectionProviderConfigurationException( "Unknown transaction isolation level: " + isolationLevel );
		}
	}

	/**
	 * Gets the {@link Connection} constant name corresponding to the given isolation.
	 *
	 * @param isolation The transaction isolation numeric value.
	 *
	 * @return The corresponding Connection constant name.
	 *
	 * @throws HibernateException If the given isolation does not map to JDBC standard isolation
	 *
	 * @see #toIsolationNiceName
	 */
	public static String toIsolationConnectionConstantName(Integer isolation) {
		final String name = ISOLATION_VALUE_CONSTANT_NAME_MAP.get( isolation );
		if ( name == null ) {
			throw new HibernateException(
					"Could not convert isolation value [" + isolation + "] to java.sql.Connection constant name"
			);
		}
		return name;
	}

	/**
	 * Get the name of a JDBC transaction isolation level
	 *
	 * @param isolation The transaction isolation numeric value.
	 *
	 * @return a nice human-readable name
	 *
	 * @see #toIsolationConnectionConstantName
	 */
	public static String toIsolationNiceName(Integer isolation) {
		final String name = isolation != null ? ISOLATION_VALUE_NICE_NAME_MAP.get( isolation ) : null;
		return name == null ? "<unknown>" : name;
	}

	public static String extractSetting(Map<String, Object> settings, String... names) {
		for ( String name : names ) {
			if ( settings.containsKey(name) ) {
				return (String) settings.get(name);
			}
		}
		return null;
	}

	@FunctionalInterface
	public interface SettingConsumer {
		void consumeSetting(String name, String value);
	}

	public static void consumeSetting(Map<String, Object> settings, SettingConsumer consumer, String... names) {
		for ( String name : names ) {
			final Object setting = settings.get( name );
			if ( setting != null ) {
				consumer.consumeSetting( name, setting.toString() );
				return;
			}
		}
	}

	private static boolean hasConfiguration(Map<String, Object> configValues, String namespace) {
		final String prefix = namespace + ".";
		for ( String key : configValues.keySet() ) {
			if ( key.startsWith( prefix ) ) {
				return true;
			}
		}
		return false;
	}
}
