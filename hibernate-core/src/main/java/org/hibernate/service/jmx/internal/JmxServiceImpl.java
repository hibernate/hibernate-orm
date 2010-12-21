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

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.TRACE;
import static org.jboss.logging.Logger.Level.WARN;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Map;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.jmx.spi.JmxService;
import org.hibernate.service.spi.Manageable;
import org.hibernate.service.spi.Service;
import org.hibernate.service.spi.Stoppable;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * Standard implementation of JMX services
 *
 * @author Steve Ebersole
 */
public class JmxServiceImpl implements JmxService, Stoppable {

    private static final Logger LOG = org.jboss.logging.Logger.getMessageLogger(Logger.class,
                                                                                JmxServiceImpl.class.getPackage().getName());

	public static final String JMX_PLATFORM_SERVER = "hibernate.jmx.usePlatformServer";
	public static final String JMX_AGENT_ID = "hibernate.jmx.agentId";
	public static final String JMX_DOMAIN_NAME = "hibernate.jmx.defaultDomain";
	public static final String JMX_SF_NAME = "hibernate.jmx.sessionFactoryName";

	private final boolean usePlatformServer;
	private final String agentId;
	private final String defaultDomain;
	private final String sessionFactoryName;

	public JmxServiceImpl(Map configValues) {
		usePlatformServer = ConfigurationHelper.getBoolean( JMX_PLATFORM_SERVER, configValues );
		agentId = (String) configValues.get( JMX_AGENT_ID );
		defaultDomain = (String) configValues.get( JMX_DOMAIN_NAME );
		sessionFactoryName = ConfigurationHelper.getString(
				JMX_SF_NAME,
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
			// MBean server and do *some* work on shutdwon.
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
                            LOG.unregisteringMBean(objectName);
							mBeanServer.unregisterMBean( objectName );
						}
						catch ( Exception e ) {
                            LOG.unableToUnregisterMBean(objectName, e.toString());
						}
					}
				}

				// stop the MBean server if we started it
				if ( startedServer ) {
                    LOG.attemptingToReleaseCreatedMBeanServer();
					try {
						MBeanServerFactory.releaseMBeanServer( mBeanServer );
					}
					catch ( Exception e ) {
                        LOG.unableToReleaseCreatedMBeanServer(e.toString());
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

	public static final String DEFAULT_OBJ_NAME_DOMAIN = "org.hibernate.core";
	public static final String OBJ_NAME_TEMPLATE = "%s:sessionFactory=%s,serviceRole=%s,serviceType=%s";

	// todo : should serviceRole come first in ObjectName template?  depends on the groupings we want in the UI.
	// 		as-is mbeans from each sessionFactory are grouped primarily.

	@Override
	public void registerService(Manageable service, Class<? extends Service> serviceRole) {
		final String domain = service.getManagementDomain() == null
				? DEFAULT_OBJ_NAME_DOMAIN
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
		catch ( HibernateException e ) {
			throw e;
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
			MBeanServer mbeanServer = MBeanServerFactory.createMBeanServer( defaultDomain );
			return mbeanServer;
		}
		catch ( Exception e ) {
			throw new HibernateException( "Unable to start MBeanServer", e );
		}
	}

    /**
     * Interface defining messages that may be logged by the outer class
     */
    @MessageLogger
    interface Logger extends BasicLogger {

        @LogMessage( level = TRACE )
        @Message( value = "Attempting to release created MBeanServer" )
        void attemptingToReleaseCreatedMBeanServer();

        @LogMessage( level = WARN )
        @Message( value = "Unable to locate MBeanServer on JMX service shutdown" )
        void unableToLocateMBeanServer();

        @LogMessage( level = WARN )
        @Message( value = "Unable to release created MBeanServer : %s" )
        void unableToReleaseCreatedMBeanServer( String string );

        @LogMessage( level = DEBUG )
        @Message( value = "Unable to unregsiter registered MBean [ON=%s] : %s" )
        void unableToUnregisterMBean( ObjectName objectName,
                                      String string );

        @LogMessage( level = TRACE )
        @Message( value = "Unregistering registered MBean [ON=%s]" )
        void unregisteringMBean( ObjectName objectName );
    }
}
