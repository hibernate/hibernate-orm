/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.connections.internal;

import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.HibernateException;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.beans.BeanInfoHelper;
import org.hibernate.service.spi.ServiceRegistryImplementor;

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
	 * The strategy for proxool connection pooling
	 */
	public static final String PROXOOL_STRATEGY = "proxool";

	/**
	 * The strategy for hikari connection pooling
	 */
	public static final String HIKARI_STRATEGY = "hikari";

	/**
	 * The strategy for vibur connection pooling
	 */
	public static final String VIBUR_STRATEGY = "vibur";

	/**
	 * The strategy for agroal connection pooling
	 */
	public static final String AGROAL_STRATEGY = "agroal";

	/**
	 * No idea.  Is this even still used?
	 */
	public static final String INJECTION_DATA = "hibernate.connection_provider.injection_data";

	// mapping from legacy connection provider name to actual
	// connection provider that will be used
	private static final Map<String,String> LEGACY_CONNECTION_PROVIDER_MAPPING;

	static {
		LEGACY_CONNECTION_PROVIDER_MAPPING = new HashMap<String,String>( 5 );

		LEGACY_CONNECTION_PROVIDER_MAPPING.put(
				"org.hibernate.connection.DatasourceConnectionProvider",
				DatasourceConnectionProviderImpl.class.getName()
		);
		LEGACY_CONNECTION_PROVIDER_MAPPING.put(
				"org.hibernate.connection.DriverManagerConnectionProvider",
				DriverManagerConnectionProviderImpl.class.getName()
		);
		LEGACY_CONNECTION_PROVIDER_MAPPING.put(
				"org.hibernate.connection.UserSuppliedConnectionProvider",
				UserSuppliedConnectionProviderImpl.class.getName()
		);
	}

	@Override
	public Class<ConnectionProvider> getServiceInitiated() {
		return ConnectionProvider.class;
	}

	@Override
	public ConnectionProvider initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		final MultiTenancyStrategy strategy = MultiTenancyStrategy.determineMultiTenancyStrategy(  configurationValues );
		if ( strategy == MultiTenancyStrategy.DATABASE || strategy == MultiTenancyStrategy.SCHEMA ) {
			// nothing to do, but given the separate hierarchies have to handle this here.
			return null;
		}

		final StrategySelector strategySelector = registry.getService( StrategySelector.class );
		final Object explicitSetting = configurationValues.get( AvailableSettings.CONNECTION_PROVIDER );
		if ( explicitSetting != null ) {
			// if we are explicitly supplied a ConnectionProvider to use (in some form) -> use it..
			if ( explicitSetting instanceof ConnectionProvider ) {
				return (ConnectionProvider) explicitSetting;
			}
			else if ( explicitSetting instanceof Class ) {
				final Class providerClass = (Class) explicitSetting;
				LOG.instantiatingExplicitConnectionProvider( providerClass.getName() );
				return instantiateExplicitConnectionProvider( providerClass );
			}
			else {
				String providerName = StringHelper.nullIfEmpty( explicitSetting.toString() );
				if ( providerName != null ) {
					if ( LEGACY_CONNECTION_PROVIDER_MAPPING.containsKey( providerName ) ) {
						final String actualProviderName = LEGACY_CONNECTION_PROVIDER_MAPPING.get( providerName );
						DeprecationLogger.DEPRECATION_LOGGER.connectionProviderClassDeprecated(
								providerName,
								actualProviderName
						);
						providerName = actualProviderName;
					}

					LOG.instantiatingExplicitConnectionProvider( providerName );
					final Class providerClass = strategySelector.selectStrategyImplementor(
							ConnectionProvider.class,
							providerName
					);
					try {
						return instantiateExplicitConnectionProvider( providerClass );
					}
					catch (Exception e) {
						throw new HibernateException(
								"Could not instantiate connection provider [" + providerName + "]",
								e
						);
					}
				}
			}
		}

		if ( configurationValues.get( AvailableSettings.DATASOURCE ) != null ) {
			return new DatasourceConnectionProviderImpl();
		}

		ConnectionProvider connectionProvider = null;

		final Class<? extends ConnectionProvider> singleRegisteredProvider = getSingleRegisteredProvider( strategySelector );
		if ( singleRegisteredProvider != null ) {
			try {
				connectionProvider = singleRegisteredProvider.newInstance();
			}
			catch (IllegalAccessException | InstantiationException e) {
				throw new HibernateException( "Could not instantiate singular-registered ConnectionProvider", e );
			}
		}

		if ( connectionProvider == null ) {
			if ( c3p0ConfigDefined( configurationValues ) ) {
				connectionProvider = instantiateC3p0Provider( strategySelector );
			}
		}

		if ( connectionProvider == null ) {
			if ( proxoolConfigDefined( configurationValues ) ) {
				connectionProvider = instantiateProxoolProvider( strategySelector );
			}
		}

		if ( connectionProvider == null ) {
			if ( hikariConfigDefined( configurationValues ) ) {
				connectionProvider = instantiateHikariProvider( strategySelector );
			}
		}

		if ( connectionProvider == null ) {
			if ( viburConfigDefined( configurationValues ) ) {
				connectionProvider = instantiateViburProvider( strategySelector );
			}
		}

		if ( connectionProvider == null ) {
			if ( agroalConfigDefined( configurationValues ) ) {
				connectionProvider = instantiateAgroalProvider( strategySelector );
			}
		}

		if ( connectionProvider == null ) {
			if ( configurationValues.get( AvailableSettings.URL ) != null ) {
				connectionProvider = new DriverManagerConnectionProviderImpl();
			}
		}

		if ( connectionProvider == null ) {
			LOG.noAppropriateConnectionProvider();
			connectionProvider = new UserSuppliedConnectionProviderImpl();
		}


		final Map injectionData = (Map) configurationValues.get( INJECTION_DATA );
		if ( injectionData != null && injectionData.size() > 0 ) {
			final ConnectionProvider theConnectionProvider = connectionProvider;
			new BeanInfoHelper( connectionProvider.getClass() ).applyToBeanInfo(
					connectionProvider,
					new BeanInfoHelper.BeanInfoDelegate() {
						public void processBeanInfo(BeanInfo beanInfo) throws Exception {
							final PropertyDescriptor[] descriptors = beanInfo.getPropertyDescriptors();
							for ( PropertyDescriptor descriptor : descriptors ) {
								final String propertyName = descriptor.getName();
								if ( injectionData.containsKey( propertyName ) ) {
									final Method method = descriptor.getWriteMethod();
									method.invoke(
											theConnectionProvider,
											injectionData.get( propertyName )
									);
								}
							}
						}
					}
			);
		}

		return connectionProvider;
	}

	private Class<? extends ConnectionProvider> getSingleRegisteredProvider(StrategySelector strategySelector) {
		final Collection<Class<? extends ConnectionProvider>> implementors = strategySelector.getRegisteredStrategyImplementors( ConnectionProvider.class );
		if ( implementors != null && implementors.size() == 1 ) {
			return implementors.iterator().next();
		}

		return null;
	}

	private ConnectionProvider instantiateExplicitConnectionProvider(Class providerClass) {
			try {
				return (ConnectionProvider) providerClass.newInstance();
			}
			catch (Exception e) {
				throw new HibernateException( "Could not instantiate connection provider [" + providerClass.getName() + "]", e );
			}
	}

	private static boolean c3p0ConfigDefined(Map configValues) {
		for ( Object key : configValues.keySet() ) {
			if ( String.class.isInstance( key )
					&& ( (String) key ).startsWith( AvailableSettings.C3P0_CONFIG_PREFIX ) ) {
				return true;
			}
		}
		return false;
	}

	private ConnectionProvider instantiateC3p0Provider(StrategySelector strategySelector) {
		try {
			return strategySelector.selectStrategyImplementor( ConnectionProvider.class, C3P0_STRATEGY ).newInstance();
		}
		catch ( Exception e ) {
			LOG.c3p0ProviderClassNotFound( C3P0_STRATEGY );
			return null;
		}
	}

	private static boolean proxoolConfigDefined(Map configValues) {
		for ( Object key : configValues.keySet() ) {
			if ( String.class.isInstance( key )
					&& ( (String) key ).startsWith( AvailableSettings.PROXOOL_CONFIG_PREFIX ) ) {
				return true;
			}
		}
		return false;
	}

	private ConnectionProvider instantiateProxoolProvider(StrategySelector strategySelector) {
		try {
			return strategySelector.selectStrategyImplementor( ConnectionProvider.class, PROXOOL_STRATEGY ).newInstance();
		}
		catch ( Exception e ) {
			LOG.proxoolProviderClassNotFound( PROXOOL_STRATEGY );
			return null;
		}
	}

	private boolean hikariConfigDefined(Map configValues) {
		for ( Object key : configValues.keySet() ) {
			if ( !String.class.isInstance( key ) ) {
				continue;
			}

			if ( ( (String) key ).startsWith( "hibernate.hikari." ) ) {
				return true;
			}
		}
		return false;
	}

	private ConnectionProvider instantiateHikariProvider(StrategySelector strategySelector) {
		try {
			return strategySelector.selectStrategyImplementor( ConnectionProvider.class, HIKARI_STRATEGY ).newInstance();
		}
		catch ( Exception e ) {
			LOG.hikariProviderClassNotFound();
			return null;
		}
	}

	private boolean viburConfigDefined(Map configValues) {
		for ( Object key : configValues.keySet() ) {
			if ( !String.class.isInstance( key ) ) {
				continue;
			}

			if ( ( (String) key ).startsWith( "hibernate.vibur." ) ) {
				return true;
			}
		}
		return false;
	}


	private boolean agroalConfigDefined(Map configValues) {
		for ( Object key : configValues.keySet() ) {
			if ( !String.class.isInstance( key ) ) {
				continue;
			}

			if ( ( (String) key ).startsWith( "hibernate.agroal." ) ) {
				return true;
			}
		}
		return false;
	}

	private ConnectionProvider instantiateViburProvider(StrategySelector strategySelector) {
		try {
			return strategySelector.selectStrategyImplementor( ConnectionProvider.class, VIBUR_STRATEGY ).newInstance();
		}
		catch ( Exception e ) {
			LOG.viburProviderClassNotFound();
			return null;
		}
	}

	private ConnectionProvider instantiateAgroalProvider(StrategySelector strategySelector) {
		try {
			return strategySelector.selectStrategyImplementor( ConnectionProvider.class, AGROAL_STRATEGY ).newInstance();
		}
		catch ( Exception e ) {
			LOG.agroalProviderClassNotFound();
			return null;
		}
	}

	/**
	 * Build the connection properties capable of being passed to the {@link java.sql.DriverManager#getConnection}
	 * forms taking {@link Properties} argument.  We seek out all keys in the passed map which start with
	 * {@code hibernate.connection.}, using them to create a new {@link Properties} instance.  The keys in this
	 * new {@link Properties} have the {@code hibernate.connection.} prefix trimmed.
	 *
	 * @param properties The map from which to build the connection specific properties.
	 *
	 * @return The connection properties.
	 */
	public static Properties getConnectionProperties(Map<?,?> properties) {
		final Properties result = new Properties();
		for ( Map.Entry entry : properties.entrySet() ) {
			if ( ! ( String.class.isInstance( entry.getKey() ) ) || ! String.class.isInstance( entry.getValue() ) ) {
				continue;
			}
			final String key = (String) entry.getKey();
			final String value = (String) entry.getValue();
			if ( key.startsWith( AvailableSettings.CONNECTION_PREFIX ) ) {
				if ( SPECIAL_PROPERTIES.contains( key ) ) {
					if ( AvailableSettings.USER.equals( key ) ) {
						result.setProperty( "user", value );
					}
				}
				else {
					result.setProperty(
							key.substring( AvailableSettings.CONNECTION_PREFIX.length() + 1 ),
							value
					);
				}
			}
			else if ( CONDITIONAL_PROPERTIES.containsKey( key ) ) {
				result.setProperty( CONDITIONAL_PROPERTIES.get( key ), value );
			}
		}
		return result;
	}

	private static final Set<String> SPECIAL_PROPERTIES;

	private static final Map<String,Integer> ISOLATION_VALUE_MAP;
	private static final Map<Integer, String> ISOLATION_VALUE_CONSTANT_NAME_MAP;
	private static final Map<Integer, String> ISOLATION_VALUE_NICE_NAME_MAP;

	static {
		SPECIAL_PROPERTIES = new HashSet<String>();
		SPECIAL_PROPERTIES.add( AvailableSettings.DATASOURCE );
		SPECIAL_PROPERTIES.add( AvailableSettings.URL );
		SPECIAL_PROPERTIES.add( AvailableSettings.CONNECTION_PROVIDER );
		SPECIAL_PROPERTIES.add( AvailableSettings.POOL_SIZE );
		SPECIAL_PROPERTIES.add( AvailableSettings.ISOLATION );
		SPECIAL_PROPERTIES.add( AvailableSettings.DRIVER );
		SPECIAL_PROPERTIES.add( AvailableSettings.USER );
		SPECIAL_PROPERTIES.add( AvailableSettings.CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT );

		ISOLATION_VALUE_MAP = new ConcurrentHashMap<String, Integer>();
		ISOLATION_VALUE_MAP.put( "TRANSACTION_NONE", Connection.TRANSACTION_NONE );
		ISOLATION_VALUE_MAP.put( "NONE", Connection.TRANSACTION_NONE );
		ISOLATION_VALUE_MAP.put( "TRANSACTION_READ_UNCOMMITTED", Connection.TRANSACTION_READ_UNCOMMITTED );
		ISOLATION_VALUE_MAP.put( "READ_UNCOMMITTED", Connection.TRANSACTION_READ_UNCOMMITTED );
		ISOLATION_VALUE_MAP.put( "TRANSACTION_READ_COMMITTED", Connection.TRANSACTION_READ_COMMITTED );
		ISOLATION_VALUE_MAP.put( "READ_COMMITTED", Connection.TRANSACTION_READ_COMMITTED );
		ISOLATION_VALUE_MAP.put( "TRANSACTION_REPEATABLE_READ", Connection.TRANSACTION_REPEATABLE_READ );
		ISOLATION_VALUE_MAP.put( "REPEATABLE_READ", Connection.TRANSACTION_REPEATABLE_READ );
		ISOLATION_VALUE_MAP.put( "TRANSACTION_SERIALIZABLE", Connection.TRANSACTION_SERIALIZABLE );
		ISOLATION_VALUE_MAP.put( "SERIALIZABLE", Connection.TRANSACTION_SERIALIZABLE );

		ISOLATION_VALUE_CONSTANT_NAME_MAP = new ConcurrentHashMap<Integer, String>();
		ISOLATION_VALUE_CONSTANT_NAME_MAP.put( Connection.TRANSACTION_NONE, "TRANSACTION_NONE" );
		ISOLATION_VALUE_CONSTANT_NAME_MAP.put( Connection.TRANSACTION_READ_UNCOMMITTED, "TRANSACTION_READ_UNCOMMITTED" );
		ISOLATION_VALUE_CONSTANT_NAME_MAP.put( Connection.TRANSACTION_READ_COMMITTED, "TRANSACTION_READ_COMMITTED" );
		ISOLATION_VALUE_CONSTANT_NAME_MAP.put( Connection.TRANSACTION_REPEATABLE_READ, "TRANSACTION_REPEATABLE_READ" );
		ISOLATION_VALUE_CONSTANT_NAME_MAP.put( Connection.TRANSACTION_SERIALIZABLE, "TRANSACTION_SERIALIZABLE" );

		ISOLATION_VALUE_NICE_NAME_MAP = new ConcurrentHashMap<Integer, String>();
		ISOLATION_VALUE_NICE_NAME_MAP.put( Connection.TRANSACTION_NONE, "NONE" );
		ISOLATION_VALUE_NICE_NAME_MAP.put( Connection.TRANSACTION_READ_UNCOMMITTED, "READ_UNCOMMITTED" );
		ISOLATION_VALUE_NICE_NAME_MAP.put( Connection.TRANSACTION_READ_COMMITTED, "READ_COMMITTED" );
		ISOLATION_VALUE_NICE_NAME_MAP.put( Connection.TRANSACTION_REPEATABLE_READ, "REPEATABLE_READ" );
		ISOLATION_VALUE_NICE_NAME_MAP.put( Connection.TRANSACTION_SERIALIZABLE, "SERIALIZABLE" );
	}

	// Connection properties (map value) that automatically need set if the
	// Hibernate property (map key) is available. Makes the assumption that
	// both settings use the same value type.
	private static final Map<String, String> CONDITIONAL_PROPERTIES;

	static {
		CONDITIONAL_PROPERTIES = new HashMap<String, String>();
		// Oracle requires that includeSynonyms=true in order for getColumns to work using a table synonym name.
		CONDITIONAL_PROPERTIES.put( AvailableSettings.ENABLE_SYNONYMS, "includeSynonyms" );
	}

	public static Integer extractIsolation(Map settings) {
		return interpretIsolation( settings.get( AvailableSettings.ISOLATION ) );
	}

	public static Integer interpretIsolation(Object setting) {
		if ( setting == null ) {
			return null;
		}

		if ( Number.class.isInstance( setting ) ) {
			return ( (Number) setting ).intValue();
		}

		final String settingAsString = setting.toString();
		if ( StringHelper.isEmpty( settingAsString ) ) {
			return null;
		}

		if ( ISOLATION_VALUE_MAP.containsKey( settingAsString ) ) {
			return ISOLATION_VALUE_MAP.get( settingAsString );
		}

		// it could be a String representation of the isolation numeric value...
		try {
			return Integer.valueOf( settingAsString );
		}
		catch (NumberFormatException ignore) {
		}

		throw new HibernateException( "Could not interpret transaction isolation setting [" + setting + "]" );
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
		String name = null;

		if ( isolation != null ) {
			name = ISOLATION_VALUE_NICE_NAME_MAP.get( isolation );
		}

		if ( name == null ) {
			name = "<unknown>";
		}
		return name;
	}
}
