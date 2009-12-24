/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.connection;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.Map;
import java.beans.Introspector;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;
import org.hibernate.util.ReflectHelper;

/**
 * Instantiates a connection provider given either <tt>System</tt> properties or
 * a <tt>java.util.Properties</tt> instance. The <tt>ConnectionProviderFactory</tt>
 * first attempts to find a name of a <tt>ConnectionProvider</tt> subclass in the
 * property <tt>hibernate.connection.provider_class</tt>. If missing, heuristics are used
 * to choose either <tt>DriverManagerConnectionProvider</tt>,
 * <tt>DatasourceConnectionProvider</tt>, <tt>C3P0ConnectionProvider</tt> or
 * <tt>DBCPConnectionProvider</tt>.
 * @see ConnectionProvider
 * @author Gavin King
 */

public final class ConnectionProviderFactory {

	private static final Logger log = LoggerFactory.getLogger(ConnectionProviderFactory.class);

	/**
	 * Instantiate a <tt>ConnectionProvider</tt> using <tt>System</tt> properties.
	 * @return The created connection provider.
	 * @throws HibernateException
	 */
	public static ConnectionProvider newConnectionProvider() throws HibernateException {
		return newConnectionProvider( Environment.getProperties() );
	}

	/**
	 * Instantiate a <tt>ConnectionProvider</tt> using given properties.
	 * Method newConnectionProvider.
	 * @param properties hibernate <tt>SessionFactory</tt> properties
	 * @return ConnectionProvider
	 * @throws HibernateException
	 */
	public static ConnectionProvider newConnectionProvider(Properties properties) throws HibernateException {
		return newConnectionProvider( properties, null );
	}

	/**
	 * Create a connection provider based on the given information.
	 *
	 * @param properties Properties being used to build the {@link org.hibernate.SessionFactory}.
	 * @param connectionProviderInjectionData Soemthing to be injected in the conenction provided
	 * @return The created connection provider
	 * @throws HibernateException
	 */
	public static ConnectionProvider newConnectionProvider(Properties properties, Map connectionProviderInjectionData) throws HibernateException {
		ConnectionProvider connections;
		String providerClass = properties.getProperty(Environment.CONNECTION_PROVIDER);
		if ( providerClass!=null ) {
			try {
				log.info("Initializing connection provider: " + providerClass);
				connections = (ConnectionProvider) ReflectHelper.classForName(providerClass).newInstance();
			}
			catch ( Exception e ) {
				log.error( "Could not instantiate connection provider", e );
				throw new HibernateException("Could not instantiate connection provider: " + providerClass);
			}
		}
		else if ( properties.getProperty(Environment.DATASOURCE)!=null ) {
			connections = new DatasourceConnectionProvider();
		}
		else if ( properties.getProperty(Environment.URL)!=null ) {
			connections = new DriverManagerConnectionProvider();
		}
		else {
			connections = new UserSuppliedConnectionProvider();
		}

		if ( connectionProviderInjectionData != null && connectionProviderInjectionData.size() != 0 ) {
			//inject the data
			try {
				BeanInfo info = Introspector.getBeanInfo( connections.getClass() );
				PropertyDescriptor[] descritors = info.getPropertyDescriptors();
				int size = descritors.length;
				for (int index = 0 ; index < size ; index++) {
					String propertyName = descritors[index].getName();
					if ( connectionProviderInjectionData.containsKey( propertyName ) ) {
						Method method = descritors[index].getWriteMethod();
						method.invoke( connections, new Object[] { connectionProviderInjectionData.get( propertyName ) } );
					}
				}
			}
			catch (IntrospectionException e) {
				throw new HibernateException("Unable to inject objects into the conenction provider", e);
			}
			catch (IllegalAccessException e) {
				throw new HibernateException("Unable to inject objects into the conenction provider", e);
			}
			catch (InvocationTargetException e) {
				throw new HibernateException("Unable to inject objects into the conenction provider", e);
			}
		}
		connections.configure(properties);
		return connections;
	}

	// cannot be instantiated
	private ConnectionProviderFactory() { throw new UnsupportedOperationException(); }

	/**
	 * Transform JDBC connection properties.
	 *
	 * Passed in the form <tt>hibernate.connection.*</tt> to the
	 * format accepted by <tt>DriverManager</tt> by trimming the leading "<tt>hibernate.connection</tt>".
	 */
	public static Properties getConnectionProperties(Properties properties) {

		Iterator iter = properties.keySet().iterator();
		Properties result = new Properties();
		while ( iter.hasNext() ) {
			String prop = (String) iter.next();
			if ( prop.startsWith(Environment.CONNECTION_PREFIX) && !SPECIAL_PROPERTIES.contains(prop) ) {
				result.setProperty(
					prop.substring( Environment.CONNECTION_PREFIX.length()+1 ),
					properties.getProperty(prop)
				);
			}
		}
		String userName = properties.getProperty(Environment.USER);
		if (userName!=null) result.setProperty( "user", userName );
		return result;
	}

	private static final Set SPECIAL_PROPERTIES;
	static {
		SPECIAL_PROPERTIES = new HashSet();
		SPECIAL_PROPERTIES.add(Environment.DATASOURCE);
		SPECIAL_PROPERTIES.add(Environment.URL);
		SPECIAL_PROPERTIES.add(Environment.CONNECTION_PROVIDER);
		SPECIAL_PROPERTIES.add(Environment.POOL_SIZE);
		SPECIAL_PROPERTIES.add(Environment.ISOLATION);
		SPECIAL_PROPERTIES.add(Environment.DRIVER);
		SPECIAL_PROPERTIES.add(Environment.USER);

	}

}






