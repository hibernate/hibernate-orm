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

import javax.naming.event.NamespaceChangeListener;
import javax.naming.event.NamingEvent;
import javax.naming.event.NamingExceptionEvent;

import org.jboss.logging.Logger;

import org.hibernate.ejb.AvailableSettings;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.ejb.internal.EntityManagerMessageLogger;
import org.hibernate.service.jndi.JndiException;
import org.hibernate.service.jndi.JndiNameException;
import org.hibernate.service.jndi.internal.JndiServiceImpl;

/**
 * @author Emmanuel Bernard
 */
public class NamingHelper {
	private NamingHelper() {}

    private static final EntityManagerMessageLogger LOG = Logger.getMessageLogger(EntityManagerMessageLogger.class, NamingHelper.class.getName());

	public static void bind(Ejb3Configuration cfg) {
		String name = cfg.getHibernateConfiguration().getProperty( AvailableSettings.CONFIGURATION_JNDI_NAME );
        if ( name == null ) {
			LOG.debug( "No JNDI name configured for binding Ejb3Configuration" );
		}
		else {
            LOG.ejb3ConfigurationName( name );

			// todo : instantiating the JndiService here is temporary until HHH-6159 is resolved.
			JndiServiceImpl jndiService = new JndiServiceImpl( cfg.getProperties() );
			try {
				jndiService.bind( name, cfg );
				LOG.boundEjb3ConfigurationToJndiName( name );
				try {
					jndiService.addListener( name, LISTENER );
				}
				catch (Exception e) {
					LOG.couldNotBindJndiListener();
				}
			}
			catch (JndiNameException e) {
				LOG.invalidJndiName( name, e );
			}
			catch (JndiException e) {
				LOG.unableToBindEjb3ConfigurationToJndi( e );
			}
		}
	}

	private static final NamespaceChangeListener LISTENER = new NamespaceChangeListener() {
		public void objectAdded(NamingEvent evt) {
            LOG.debugf("An Ejb3Configuration was successfully bound to name: %s", evt.getNewBinding().getName());
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
            LOG.unableToAccessEjb3Configuration(evt.getException());
		}
	};


}
