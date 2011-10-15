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
package org.hibernate.service.jmx.internal;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Map;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.Service;
import org.hibernate.service.jmx.spi.JmxService;
import org.hibernate.service.spi.Manageable;
import org.hibernate.service.spi.Stoppable;

/**
 * Standard implementation of JMX services
 *
 * @author Steve Ebersole
 */
public class JmxServiceImpl implements JmxService, Stoppable {
    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, JmxServiceImpl.class.getName());

	public static final String OBJ_NAME_TEMPLATE = "%s:sessionFactory=%s,serviceRole=%s,serviceType=%s";

	private final boolean usePlatformServer;
	private final String agentId;
	private final String defaultDomain;
	private final String sessionFactoryName;

	public JmxServiceImpl(Map configValues) {
		usePlatformServer = ConfigurationHelper.getBoolean( AvailableSettings.JMX_PLATFORM_SERVER, configValues );
		agentId = (String) configValues.get( AvailableSettings.JMX_AGENT_ID );
		defaultDomain = (String) configValues.get( AvailableSettings.JMX_DOMAIN_NAME );
		sessionFactoryName = ConfigurationHelper.getString(
				AvailableSettings.JMX_SF_NAME,
				configValues,
				ConfigurationHelper.getString( Environment.SESSION_FACTORY_NAME, configValues )
		);
	}

	private boolean startedServer;
	private ArrayList<ObjectName> registeredMBeans;

	@Override
	public void stop() {
		try {
			// if we either started the JMX server or we registered some MBeans we at least need to look up
			// MBean server and do *some* work on shutdown.
			if ( startedServer || registeredMBeans != null ) {
				MBeanServer mBeanServer = findServer();
				if ( mBeanServer == null ) {
					LOG.unableToLocateMBeanServer();
					return;
				}

				// release any MBeans we registered
				if ( registeredMBeans != null ) {
					for ( ObjectName objectName : registeredMBeans ) {
						try {
							LOG.tracev( "Unregistering registered MBean [ON={0}]", objectName );
							mBeanServer.unregisterMBean( objectName );
						}
						catch ( Exception e ) {
							LOG.debugf( "Unable to unregsiter registered MBean [ON=%s] : %s", objectName, e.toString() );
						}
					}
				}

				// stop the MBean server if we started it
				if ( startedServer ) {
					LOG.trace( "Attempting to release created MBeanServer" );
					try {
						MBeanServerFactory.releaseMBeanServer( mBeanServer );
					}
					catch ( Exception e ) {
						LOG.unableToReleaseCreatedMBeanServer( e.toString() );
					}
				}
			}
		}
		finally {
			startedServer = false;
			if ( registeredMBeans != null ) {
				registeredMBeans.clear();
				registeredMBeans = null;
			}
		}
	}


	// todo : should serviceRole come first in ObjectName template?  depends on the groupings we want in the UI.
	// 		as-is mbeans from each sessionFactory are grouped primarily.

	@Override
	public void registerService(Manageable service, Class<? extends Service> serviceRole) {
		final String domain = service.getManagementDomain() == null
				? AvailableSettings.JMX_DEFAULT_OBJ_NAME_DOMAIN
				: service.getManagementDomain();
		final String serviceType = service.getManagementServiceType() == null
				? service.getClass().getName()
				: service.getManagementServiceType();
		try {
			final ObjectName objectName = new ObjectName(
					String.format(
							OBJ_NAME_TEMPLATE,
							domain,
							sessionFactoryName,
							serviceRole.getName(),
							serviceType
					)
			);
			registerMBean( objectName, service.getManagementBean() );
		}
		catch ( MalformedObjectNameException e ) {
			throw new HibernateException( "Unable to generate service IbjectName", e );
		}
	}

	@Override
	public void registerMBean(ObjectName objectName, Object mBean) {
		MBeanServer mBeanServer = findServer();
		if ( mBeanServer == null ) {
			if ( startedServer ) {
				throw new HibernateException( "Could not locate previously started MBeanServer" );
			}
			mBeanServer = startMBeanServer();
			startedServer = true;
		}

		try {
			mBeanServer.registerMBean( mBean, objectName );
			if ( registeredMBeans == null ) {
				registeredMBeans = new ArrayList<ObjectName>();
			}
			registeredMBeans.add( objectName );
		}
		catch ( Exception e ) {
			throw new HibernateException( "Unable to register MBean [ON=" + objectName + "]", e );
		}
	}

	/**
	 * Locate the MBean server to use based on user input from startup.
	 *
	 * @return The MBean server to use.
	 */
	private MBeanServer findServer() {
		if ( usePlatformServer ) {
			// they specified to use the platform (vm) server
			return ManagementFactory.getPlatformMBeanServer();
		}

		// otherwise lookup all servers by (optional) agentId.
		// IMPL NOTE : the findMBeanServer call treats a null agentId to mean match all...
		ArrayList<MBeanServer> mbeanServers = MBeanServerFactory.findMBeanServer( agentId );

		if ( defaultDomain == null ) {
			// they did not specify a domain by which to locate a particular MBeanServer to use, so chose the first
			return mbeanServers.get( 0 );
		}

		for ( MBeanServer mbeanServer : mbeanServers ) {
			// they did specify a domain, so attempt to locate an MBEanServer with a matching default domain, returning it
			// if we find it.
			if ( defaultDomain.equals( mbeanServer.getDefaultDomain() ) ) {
				return mbeanServer;
			}
		}

		return null;
	}

	private MBeanServer startMBeanServer() {
		try {
			return MBeanServerFactory.createMBeanServer( defaultDomain );
		}
		catch ( Exception e ) {
			throw new HibernateException( "Unable to start MBeanServer", e );
		}
	}
}
