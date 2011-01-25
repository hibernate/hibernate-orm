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
package org.hibernate.impl;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
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
import org.hibernate.Logger;
import org.hibernate.SessionFactory;
import org.hibernate.internal.util.jndi.JndiHelper;

/**
 * Resolves {@link SessionFactory} instances during <tt>JNDI<tt> look-ups as well as during deserialization
 */
public class SessionFactoryObjectFactory implements ObjectFactory {

	@SuppressWarnings({ "UnusedDeclaration" })
	private static final SessionFactoryObjectFactory INSTANCE; //to stop the class from being unloaded

    private static final Logger LOG = org.jboss.logging.Logger.getMessageLogger(Logger.class,
                                                                                SessionFactoryObjectFactory.class.getPackage().getName());

	static {
		INSTANCE = new SessionFactoryObjectFactory();
        LOG.debug("Initializing class SessionFactoryObjectFactory");
	}

	private static final ConcurrentHashMap<String, SessionFactory> INSTANCES = new ConcurrentHashMap<String, SessionFactory>();
	private static final ConcurrentHashMap<String, SessionFactory> NAMED_INSTANCES = new ConcurrentHashMap<String, SessionFactory>();

	private static final NamingListener LISTENER = new NamespaceChangeListener() {
		public void objectAdded(NamingEvent evt) {
            LOG.debug("A factory was successfully bound to name: " + evt.getNewBinding().getName());
		}
		public void objectRemoved(NamingEvent evt) {
			String name = evt.getOldBinding().getName();
            LOG.factoryUnboundFromName(name);
			Object instance = NAMED_INSTANCES.remove(name);
			Iterator iter = INSTANCES.values().iterator();
			while ( iter.hasNext() ) {
				if ( iter.next()==instance ) iter.remove();
			}
		}
		public void objectRenamed(NamingEvent evt) {
			String name = evt.getOldBinding().getName();
            LOG.factoryRenamedFromName(name);
			NAMED_INSTANCES.put( evt.getNewBinding().getName(), NAMED_INSTANCES.remove(name) );
		}
		public void namingExceptionThrown(NamingExceptionEvent evt) {
			//noinspection ThrowableResultOfMethodCallIgnored
            LOG.namingExceptionAccessingFactory(evt.getException());
		}
	};

	public Object getObjectInstance(Object reference, Name name, Context ctx, Hashtable env) throws Exception {
        LOG.debug("JNDI lookup: " + name);
		String uid = (String) ( (Reference) reference ).get(0).getContent();
		return getInstance(uid);
	}

	public static void addInstance(String uid, String name, SessionFactory instance, Properties properties) {
        LOG.debug("Registered: " + uid + " (" + (name == null ? "<unnamed>" : name) + ")");
		INSTANCES.put(uid, instance);
		if (name!=null) NAMED_INSTANCES.put(name, instance);

		//must add to JNDI _after_ adding to HashMaps, because some JNDI servers use serialization
        if (name == null) LOG.notBindingFactoryToJndi();
		else {
            LOG.factoryName(name);

			try {
				Context ctx = JndiHelper.getInitialContext(properties);
				JndiHelper.bind(ctx, name, instance);
                LOG.factoryBoundToJndiName(name);
				( (EventContext) ctx ).addNamingListener(name, EventContext.OBJECT_SCOPE, LISTENER);
			}
			catch (InvalidNameException ine) {
                LOG.invalidJndiName(name, ine);
			}
			catch (NamingException ne) {
                LOG.warn(LOG.unableToBindFactoryToJndi(), ne);
			}
			catch(ClassCastException cce) {
                LOG.initialContextDidNotImplementEventContext();
			}
		}
	}

	public static void removeInstance(String uid, String name, Properties properties) {
		//TODO: theoretically non-threadsafe...

		if (name!=null) {
            LOG.unbindingFactoryFromJndiName(name);

			try {
				Context ctx = JndiHelper.getInitialContext(properties);
				ctx.unbind(name);
                LOG.factoryUnboundFromJndiName(name);
			}
			catch (InvalidNameException ine) {
                LOG.invalidJndiName(name, ine);
			}
			catch (NamingException ne) {
                LOG.warn(LOG.unableToUnbindFactoryFromJndi(), ne);
			}

			NAMED_INSTANCES.remove(name);

		}

		INSTANCES.remove(uid);

	}

	public static Object getNamedInstance(String name) {
        LOG.debug("Lookup: name=" + name);
		Object result = NAMED_INSTANCES.get(name);
		if (result==null) {
            LOG.debug("Not found: " + name);
            LOG.debug(NAMED_INSTANCES.toString());
		}
		return result;
	}

	public static Object getInstance(String uid) {
        LOG.debug("Lookup: uid=" + uid);
		Object result = INSTANCES.get(uid);
		if (result==null) {
            LOG.debug("Not found: " + uid);
            LOG.debug(INSTANCES.toString());
		}
		return result;
	}
}