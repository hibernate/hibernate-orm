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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Emmanuel Bernard
 */
public class NamingHelper {
	private NamingHelper() {}

	private static final Logger log = LoggerFactory.getLogger( NamingHelper.class );

	/** bind the configuration to the JNDI */
	public static void bind(Ejb3Configuration cfg) {
		String name = cfg.getHibernateConfiguration().getProperty( AvailableSettings.CONFIGURATION_JNDI_NAME );
		if ( name == null ) {
			log.debug( "No JNDI name configured for binding Ejb3Configuration" );
		}
		else {
			log.info( "Ejb3Configuration name: {}", name );

			try {
				Context ctx = org.hibernate.util.NamingHelper.getInitialContext( cfg.getProperties() );
				org.hibernate.util.NamingHelper.bind( ctx, name, cfg );
				log.info( "Bound Ejb3Configuration to JNDI name: {}", name );
				( (EventContext) ctx ).addNamingListener( name, EventContext.OBJECT_SCOPE, LISTENER );
			}
			catch (InvalidNameException ine) {
				log.error( "Invalid JNDI name: " + name, ine );
			}
			catch (NamingException ne) {
				log.warn( "Could not bind Ejb3Configuration to JNDI", ne );
			}
			catch (ClassCastException cce) {
				log.warn( "InitialContext did not implement EventContext" );
			}
		}
	}

	private static final NamingListener LISTENER = new NamespaceChangeListener() {
		public void objectAdded(NamingEvent evt) {
			log.debug( "An Ejb3Configuration was successfully bound to name: {}", evt.getNewBinding().getName() );
		}

		public void objectRemoved(NamingEvent evt) {
			String name = evt.getOldBinding().getName();
			log.info( "An Ejb3Configuration was unbound from name: {}", name );
		}

		public void objectRenamed(NamingEvent evt) {
			String name = evt.getOldBinding().getName();
			log.info( "An Ejb3Configuration was renamed from name: {}", name );
		}

		public void namingExceptionThrown(NamingExceptionEvent evt) {
			log.warn( "Naming exception occurred accessing Ejb3Configuration", evt.getException() );
		}
	};


}
