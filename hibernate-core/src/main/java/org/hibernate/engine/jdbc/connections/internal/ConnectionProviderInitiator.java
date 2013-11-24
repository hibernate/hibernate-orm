/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.engine.jdbc.connections.internal;

import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.beans.BeanInfoHelper;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import org.jboss.logging.Logger;

/**
 * Instantiates and configures an appropriate {@link ConnectionProvider}.
 *
 * @author Gavin King
 * @author Steve Ebersole
 * @author Brett Meyer
 */
public class ConnectionProviderInitiator implements StandardServiceInitiator<ConnectionProvider> {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			ConnectionProviderInitiator.class.getName()
	);

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

		ConnectionProvider connectionProvider = null;
		final String providerName = getConfiguredConnectionProviderName( configurationValues );
		if ( providerName != null ) {
			connectionProvider = instantiateExplicitConnectionProvider( providerName, strategySelector );
		}
		else if ( configurationValues.get( Environment.DATASOURCE ) != null ) {
			connectionProvider = new DatasourceConnectionProviderImpl();
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
			if ( configurationValues.get( Environment.URL ) != null ) {
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

	private String getConfiguredConnectionProviderName( Map configurationValues ) {
		String providerName = (String) configurationValues.get( Environment.CONNECTION_PROVIDER );
		if ( LEGACY_CONNECTION_PROVIDER_MAPPING.containsKey( providerName ) ) {
			final String actualProviderName = LEGACY_CONNECTION_PROVIDER_MAPPING.get( providerName );
			LOG.providerClassDeprecated( providerName, actualProviderName );
			providerName = actualProviderName;
		}
		return providerName;
	}

	private ConnectionProvider instantiateExplicitConnectionProvider(
			String providerName,
			StrategySelector strategySelector) {
		try {
			LOG.instantiatingExplicitConnectionProvider( providerName );
            // This relies on selectStrategyImplementor trying
            // classLoaderService.classForName( name ).
            // TODO: Maybe we shouldn't rely on that here and do it manually?
			return strategySelector.selectStrategyImplementor( ConnectionProvider.class, providerName ).newInstance();
		}
		catch ( Exception e ) {
			throw new HibernateException( "Could not instantiate connection provider [" + providerName + "]", e );
		}
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

	private static boolean c3p0ConfigDefined(Map configValues) {
		for ( Object key : configValues.keySet() ) {
			if ( String.class.isInstance( key )
					&& ( (String) key ).startsWith( AvailableSettings.C3P0_CONFIG_PREFIX ) ) {
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

	private static boolean proxoolConfigDefined(Map configValues) {
		for ( Object key : configValues.keySet() ) {
			if ( String.class.isInstance( key )
					&& ( (String) key ).startsWith( AvailableSettings.PROXOOL_CONFIG_PREFIX ) ) {
				return true;
			}
		}
		return false;
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
			if ( key.startsWith( Environment.CONNECTION_PREFIX ) ) {
				if ( SPECIAL_PROPERTIES.contains( key ) ) {
					if ( Environment.USER.equals( key ) ) {
						result.setProperty( "user", value );
					}
				}
				else {
					result.setProperty(
							key.substring( Environment.CONNECTION_PREFIX.length() + 1 ),
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

	static {
		SPECIAL_PROPERTIES = new HashSet<String>();
		SPECIAL_PROPERTIES.add( Environment.DATASOURCE );
		SPECIAL_PROPERTIES.add( Environment.URL );
		SPECIAL_PROPERTIES.add( Environment.CONNECTION_PROVIDER );
		SPECIAL_PROPERTIES.add( Environment.POOL_SIZE );
		SPECIAL_PROPERTIES.add( Environment.ISOLATION );
		SPECIAL_PROPERTIES.add( Environment.DRIVER );
		SPECIAL_PROPERTIES.add( Environment.USER );
	}

	// Connection properties (map value) that automatically need set if the
	// Hibernate property (map key) is available. Makes the assumption that
	// both settings use the same value type.
	private static final Map<String, String> CONDITIONAL_PROPERTIES;

	static {
		CONDITIONAL_PROPERTIES = new HashMap<String, String>();
		// Oracle requires that includeSynonyms=true in order for getColumns to work using a table synonym name.
		CONDITIONAL_PROPERTIES.put( Environment.ENABLE_SYNONYMS, "includeSynonyms" );
	}
}
