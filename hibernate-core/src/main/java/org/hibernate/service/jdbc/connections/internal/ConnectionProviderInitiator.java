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
package org.hibernate.service.jdbc.connections.internal;

import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.beans.BeanInfoHelper;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.spi.BasicServiceInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Instantiates and configures an appropriate {@link ConnectionProvider}.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class ConnectionProviderInitiator implements BasicServiceInitiator<ConnectionProvider> {
	public static final ConnectionProviderInitiator INSTANCE = new ConnectionProviderInitiator();

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class,
                                                                       ConnectionProviderInitiator.class.getName());
	public static final String C3P0_PROVIDER_CLASS_NAME =
			"org.hibernate.service.jdbc.connections.internal.C3P0ConnectionProvider";

	public static final String PROXOOL_PROVIDER_CLASS_NAME =
			"org.hibernate.service.jdbc.connections.internal.ProxoolConnectionProvider";

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
		LEGACY_CONNECTION_PROVIDER_MAPPING.put(
				"org.hibernate.connection.C3P0ConnectionProvider",
				C3P0_PROVIDER_CLASS_NAME
		);
		LEGACY_CONNECTION_PROVIDER_MAPPING.put(
				"org.hibernate.connection.ProxoolConnectionProvider",
				PROXOOL_PROVIDER_CLASS_NAME
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

		final ClassLoaderService classLoaderService = registry.getService( ClassLoaderService.class );

		ConnectionProvider connectionProvider = null;
		String providerClassName = getConfiguredConnectionProviderName( configurationValues );
		if ( providerClassName != null ) {
			connectionProvider = instantiateExplicitConnectionProvider( providerClassName, classLoaderService );
		}
		else if ( configurationValues.get( Environment.DATASOURCE ) != null ) {
			connectionProvider = new DatasourceConnectionProviderImpl();
		}

		if ( connectionProvider == null ) {
			if ( c3p0ConfigDefined( configurationValues ) && c3p0ProviderPresent( classLoaderService ) ) {
				connectionProvider = instantiateExplicitConnectionProvider( C3P0_PROVIDER_CLASS_NAME,
						classLoaderService
				);
			}
		}

		if ( connectionProvider == null ) {
			if ( proxoolConfigDefined( configurationValues ) && proxoolProviderPresent( classLoaderService ) ) {
				connectionProvider = instantiateExplicitConnectionProvider( PROXOOL_PROVIDER_CLASS_NAME,
						classLoaderService
				);
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
							PropertyDescriptor[] descritors = beanInfo.getPropertyDescriptors();
							for ( int i = 0, size = descritors.length; i < size; i++ ) {
								String propertyName = descritors[i].getName();
								if ( injectionData.containsKey( propertyName ) ) {
									Method method = descritors[i].getWriteMethod();
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
		String providerClassName = ( String ) configurationValues.get( Environment.CONNECTION_PROVIDER );
		if ( LEGACY_CONNECTION_PROVIDER_MAPPING.containsKey( providerClassName ) ) {
			String actualProviderClassName = LEGACY_CONNECTION_PROVIDER_MAPPING.get( providerClassName );
            LOG.providerClassDeprecated(providerClassName, actualProviderClassName);
			providerClassName = actualProviderClassName;
		}
		return providerClassName;
	}

	private ConnectionProvider instantiateExplicitConnectionProvider(
			String providerClassName,
			ClassLoaderService classLoaderService) {
		try {
            LOG.instantiatingExplicitConnectionProvider( providerClassName );
			return (ConnectionProvider) classLoaderService.classForName( providerClassName ).newInstance();
		}
		catch ( Exception e ) {
			throw new HibernateException( "Could not instantiate connection provider [" + providerClassName + "]", e );
		}
	}

	private boolean c3p0ProviderPresent(ClassLoaderService classLoaderService) {
		try {
			classLoaderService.classForName( C3P0_PROVIDER_CLASS_NAME );
		}
		catch ( Exception e ) {
            LOG.c3p0ProviderClassNotFound(C3P0_PROVIDER_CLASS_NAME);
			return false;
		}
		return true;
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

	private boolean proxoolProviderPresent(ClassLoaderService classLoaderService) {
		try {
			classLoaderService.classForName( PROXOOL_PROVIDER_CLASS_NAME );
		}
		catch ( Exception e ) {
            LOG.proxoolProviderClassNotFound(PROXOOL_PROVIDER_CLASS_NAME);
			return false;
		}
		return true;
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
		Properties result = new Properties();
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
}
