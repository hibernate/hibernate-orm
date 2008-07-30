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
package org.hibernate.impl;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.event.EventContext;
import javax.naming.event.NamespaceChangeListener;
import javax.naming.event.NamingEvent;
import javax.naming.event.NamingExceptionEvent;
import javax.naming.event.NamingListener;
import javax.naming.spi.ObjectFactory;

import org.hibernate.SessionFactory;
import org.hibernate.util.FastHashMap;
import org.hibernate.util.NamingHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves <tt>SessionFactory</tt> JNDI lookups and deserialization
 */
public class SessionFactoryObjectFactory implements ObjectFactory {

	private static final SessionFactoryObjectFactory INSTANCE; //to stop the class from being unloaded

	private static final Logger log;

	static {
		log = LoggerFactory.getLogger( SessionFactoryObjectFactory.class );
		INSTANCE = new SessionFactoryObjectFactory();
		log.debug("initializing class SessionFactoryObjectFactory");
	}

	private static final FastHashMap INSTANCES = new FastHashMap();
	private static final FastHashMap NAMED_INSTANCES = new FastHashMap();

	private static final NamingListener LISTENER = new NamespaceChangeListener() {
		public void objectAdded(NamingEvent evt) {
			log.debug( "A factory was successfully bound to name: " + evt.getNewBinding().getName() );
		}
		public void objectRemoved(NamingEvent evt) {
			String name = evt.getOldBinding().getName();
			log.info("A factory was unbound from name: " + name);
			Object instance = NAMED_INSTANCES.remove(name);
			Iterator iter = INSTANCES.values().iterator();
			while ( iter.hasNext() ) {
				if ( iter.next()==instance ) iter.remove();
			}
		}
		public void objectRenamed(NamingEvent evt) {
			String name = evt.getOldBinding().getName();
			log.info("A factory was renamed from name: " + name);
			NAMED_INSTANCES.put( evt.getNewBinding().getName(), NAMED_INSTANCES.remove(name) );
		}
		public void namingExceptionThrown(NamingExceptionEvent evt) {
			log.warn( "Naming exception occurred accessing factory: " + evt.getException() );
		}
	};

	public Object getObjectInstance(Object reference, Name name, Context ctx, Hashtable env) throws Exception {
		log.debug("JNDI lookup: " + name);
		String uid = (String) ( (Reference) reference ).get(0).getContent();
		return getInstance(uid);
	}

	public static void addInstance(String uid, String name, SessionFactory instance, Properties properties) {

		log.debug("registered: " + uid + " (" + ( (name==null) ? "unnamed" : name ) + ')');
		INSTANCES.put(uid, instance);
		if (name!=null) NAMED_INSTANCES.put(name, instance);

		//must add to JNDI _after_ adding to HashMaps, because some JNDI servers use serialization
		if (name==null) {
			log.info("Not binding factory to JNDI, no JNDI name configured");
		}
		else {

			log.info("Factory name: " + name);

			try {
				Context ctx = NamingHelper.getInitialContext(properties);
				NamingHelper.bind(ctx, name, instance);
				log.info("Bound factory to JNDI name: " + name);
				( (EventContext) ctx ).addNamingListener(name, EventContext.OBJECT_SCOPE, LISTENER);
			}
			catch (InvalidNameException ine) {
				log.error("Invalid JNDI name: " + name, ine);
			}
			catch (NamingException ne) {
				log.warn("Could not bind factory to JNDI", ne);
			}
			catch(ClassCastException cce) {
				log.warn("InitialContext did not implement EventContext");
			}

		}

	}

	public static void removeInstance(String uid, String name, Properties properties) {
		//TODO: theoretically non-threadsafe...

		if (name!=null) {
			log.info("Unbinding factory from JNDI name: " + name);

			try {
				Context ctx = NamingHelper.getInitialContext(properties);
				ctx.unbind(name);
				log.info("Unbound factory from JNDI name: " + name);
			}
			catch (InvalidNameException ine) {
				log.error("Invalid JNDI name: " + name, ine);
			}
			catch (NamingException ne) {
				log.warn("Could not unbind factory from JNDI", ne);
			}

			NAMED_INSTANCES.remove(name);

		}

		INSTANCES.remove(uid);

	}

	public static Object getNamedInstance(String name) {
		log.debug("lookup: name=" + name);
		Object result = NAMED_INSTANCES.get(name);
		if (result==null) {
			log.debug("Not found: " + name);
			log.debug( NAMED_INSTANCES.toString() );
		}
		return result;
	}

	public static Object getInstance(String uid) {
		log.debug("lookup: uid=" + uid);
		Object result = INSTANCES.get(uid);
		if (result==null) {
			log.debug("Not found: " + uid);
			log.debug( INSTANCES.toString() );
		}
		return result;
	}

}







