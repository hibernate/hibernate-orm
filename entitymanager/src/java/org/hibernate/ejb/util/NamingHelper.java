//$Id: $
package org.hibernate.ejb.util;

import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.NamingException;
import javax.naming.event.EventContext;
import javax.naming.event.NamespaceChangeListener;
import javax.naming.event.NamingEvent;
import javax.naming.event.NamingExceptionEvent;
import javax.naming.event.NamingListener;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.ejb.HibernatePersistence;
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
		String name = cfg.getHibernateConfiguration().getProperty( HibernatePersistence.CONFIGURATION_JNDI_NAME );
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
