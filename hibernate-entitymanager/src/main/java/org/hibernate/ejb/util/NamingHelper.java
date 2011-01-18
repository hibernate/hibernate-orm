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
package org.hibernate.ejb.util;

import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.NamingException;
import javax.naming.event.EventContext;
import javax.naming.event.NamespaceChangeListener;
import javax.naming.event.NamingEvent;
import javax.naming.event.NamingExceptionEvent;
import javax.naming.event.NamingListener;
import org.hibernate.ejb.AvailableSettings;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.ejb.EntityManagerLogger;
import org.hibernate.internal.util.jndi.JndiHelper;

/**
 * @author Emmanuel Bernard
 */
public class NamingHelper {
	private NamingHelper() {}

    private static final EntityManagerLogger LOG = org.jboss.logging.Logger.getMessageLogger(EntityManagerLogger.class,
                                                                                             EntityManagerLogger.class.getPackage().getName());

	/** bind the configuration to the JNDI */
	public static void bind(Ejb3Configuration cfg) {
		String name = cfg.getHibernateConfiguration().getProperty( AvailableSettings.CONFIGURATION_JNDI_NAME );
        if (name == null) LOG.debug("No JNDI name configured for binding Ejb3Configuration");
		else {
            LOG.ejb3ConfigurationName(name);

			try {
				Context ctx = JndiHelper.getInitialContext( cfg.getProperties() );
				JndiHelper.bind( ctx, name, cfg );
                LOG.boundEjb3ConfigurationToJndiName(name);
				( (EventContext) ctx ).addNamingListener( name, EventContext.OBJECT_SCOPE, LISTENER );
			}
			catch (InvalidNameException ine) {
                LOG.error(LOG.invalidJndiName(name), ine);
			}
			catch (NamingException ne) {
                LOG.warn(LOG.unableToBindEjb3ConfigurationToJndi(), ne);
			}
			catch (ClassCastException cce) {
                LOG.initialContextDoesNotImplementEventContext();
			}
		}
	}

	private static final NamingListener LISTENER = new NamespaceChangeListener() {
		public void objectAdded(NamingEvent evt) {
            LOG.debug("An Ejb3Configuration was successfully bound to name: " + evt.getNewBinding().getName());
		}

		public void objectRemoved(NamingEvent evt) {
			String name = evt.getOldBinding().getName();
            LOG.ejb3ConfigurationUnboundFromName(name);
		}

		public void objectRenamed(NamingEvent evt) {
			String name = evt.getOldBinding().getName();
            LOG.ejb3ConfigurationRenamedFromName(name);
		}

		public void namingExceptionThrown(NamingExceptionEvent evt) {
            LOG.warn(LOG.unableToAccessEjb3Configuration(), evt.getException());
		}
	};


}
