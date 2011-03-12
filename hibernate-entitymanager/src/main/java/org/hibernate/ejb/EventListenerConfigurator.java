/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
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
 */
package org.hibernate.ejb;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.ejb.event.CallbackHandlerConsumer;
import org.hibernate.ejb.event.EJB3AutoFlushEventListener;
import org.hibernate.ejb.event.EJB3DeleteEventListener;
import org.hibernate.ejb.event.EJB3FlushEntityEventListener;
import org.hibernate.ejb.event.EJB3FlushEventListener;
import org.hibernate.ejb.event.EJB3MergeEventListener;
import org.hibernate.ejb.event.EJB3PersistEventListener;
import org.hibernate.ejb.event.EJB3PersistOnFlushEventListener;
import org.hibernate.ejb.event.EJB3PostDeleteEventListener;
import org.hibernate.ejb.event.EJB3PostInsertEventListener;
import org.hibernate.ejb.event.EJB3PostLoadEventListener;
import org.hibernate.ejb.event.EJB3PostUpdateEventListener;
import org.hibernate.ejb.event.EJB3SaveEventListener;
import org.hibernate.ejb.event.EJB3SaveOrUpdateEventListener;
import org.hibernate.ejb.event.EntityCallbackHandler;
import org.hibernate.event.AutoFlushEventListener;
import org.hibernate.event.DeleteEventListener;
import org.hibernate.event.EventListeners;
import org.hibernate.event.FlushEntityEventListener;
import org.hibernate.event.FlushEventListener;
import org.hibernate.event.MergeEventListener;
import org.hibernate.event.PersistEventListener;
import org.hibernate.event.PostDeleteEventListener;
import org.hibernate.event.PostInsertEventListener;
import org.hibernate.event.PostLoadEventListener;
import org.hibernate.event.PostUpdateEventListener;
import org.hibernate.event.PreDeleteEventListener;
import org.hibernate.event.PreInsertEventListener;
import org.hibernate.event.PreLoadEventListener;
import org.hibernate.event.PreUpdateEventListener;
import org.hibernate.event.SaveOrUpdateEventListener;
import org.hibernate.event.def.DefaultPostLoadEventListener;
import org.hibernate.event.def.DefaultPreLoadEventListener;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.secure.JACCPreDeleteEventListener;
import org.hibernate.secure.JACCPreInsertEventListener;
import org.hibernate.secure.JACCPreLoadEventListener;
import org.hibernate.secure.JACCPreUpdateEventListener;
import org.hibernate.secure.JACCSecurityListener;

/**
 * @author Emmanuel Bernard
 */
public class EventListenerConfigurator {
	private static final Object[] READER_METHOD_ARGS = new Object[0];

	private Ejb3Configuration configuration;
	private boolean isSecurity;

	public EventListenerConfigurator(Ejb3Configuration configuration) {
		this.configuration = configuration;
		EventListeners listenerConfig = configuration.getEventListeners();

		//Action event
		//EJB3-specific ops listeners
		listenerConfig.setFlushEventListeners( new FlushEventListener[] { EJB3FlushEventListener.INSTANCE } );
		//EJB3-specific ops listeners
		listenerConfig.setAutoFlushEventListeners( new AutoFlushEventListener[] { EJB3AutoFlushEventListener.INSTANCE } );
		listenerConfig.setDeleteEventListeners( new DeleteEventListener[] { new EJB3DeleteEventListener() } );
		listenerConfig.setFlushEntityEventListeners(
				new FlushEntityEventListener[] { new EJB3FlushEntityEventListener() }
		);
		listenerConfig.setMergeEventListeners( new MergeEventListener[] { new EJB3MergeEventListener() } );
		listenerConfig.setPersistEventListeners( new PersistEventListener[] { new EJB3PersistEventListener() } );
		listenerConfig.setPersistOnFlushEventListeners(
				new PersistEventListener[] { new EJB3PersistOnFlushEventListener() }
		);
		listenerConfig.setSaveEventListeners( new SaveOrUpdateEventListener[] { new EJB3SaveEventListener() } );
		listenerConfig.setSaveOrUpdateEventListeners(
				new SaveOrUpdateEventListener[] { new EJB3SaveOrUpdateEventListener() }
		);

		//Pre events
		listenerConfig.setPreInsertEventListeners(
				new PreInsertEventListener[] {
						new JACCPreInsertEventListener(),
				}
		);
		listenerConfig.setPreUpdateEventListeners(
				new PreUpdateEventListener[] {
						new JACCPreUpdateEventListener(),
				}
		);
		listenerConfig.setPreDeleteEventListeners(
				new PreDeleteEventListener[] {
						new JACCPreDeleteEventListener()
				}
		);

		//Add the default Hibernate Core PreLoadEventListener
		//TODO shouldn't we read the value from getPreLoadEventListeners and add JACC?
		//probably a better thing to do as it allows cfg.xml config but this is a big change and need more thoughts
		listenerConfig.setPreLoadEventListeners(
				new PreLoadEventListener[] {
						new DefaultPreLoadEventListener(),
						new JACCPreLoadEventListener()
				}
		);

		//post events
		listenerConfig.setPostDeleteEventListeners(
				new PostDeleteEventListener[] { new EJB3PostDeleteEventListener() }
		);
		listenerConfig.setPostInsertEventListeners(
				new PostInsertEventListener[] { new EJB3PostInsertEventListener() }
		);
		//Add the default Hibernate Core PostLoadEventListener
		//TODO shouldn't we read the value from getPostLoadEventListeners
		//probably a better thing to do as it allows cfg.xml config but this is a big change and need more thoughts
		listenerConfig.setPostLoadEventListeners(
				new PostLoadEventListener[] { new EJB3PostLoadEventListener(), new DefaultPostLoadEventListener() }
		);
		
		EJB3PostUpdateEventListener postUpdateEventListener = new EJB3PostUpdateEventListener();
		listenerConfig.setPostUpdateEventListeners(
				new PostUpdateEventListener[] { postUpdateEventListener }
		);
		
// EJB-288 - registration of post collection listeners. Commented out due to problem
// of duplicated callbacks. See Jira.
//		listenerConfig.setPostCollectionRecreateEventListeners(
//				new PostCollectionRecreateEventListener[] { postUpdateEventListener });
//		
//		listenerConfig.setPostCollectionRemoveEventListeners(
//				new PostCollectionRemoveEventListener[] { postUpdateEventListener });
//		
//		listenerConfig.setPostCollectionUpdateEventListeners(
//				new PostCollectionUpdateEventListener[] { postUpdateEventListener });		
	}

	public void setProperties(Properties properties) {
		if ( properties.containsKey( AvailableSettings.JACC_ENABLED ) ) {
			isSecurity = true;
		}
		//override events if needed
		Enumeration<?> enumeration = properties.propertyNames();
		while ( enumeration.hasMoreElements() ) {
			String name = (String) enumeration.nextElement();
			if ( name.startsWith( AvailableSettings.EVENT_LISTENER_PREFIX ) ) {
				String type = name.substring( AvailableSettings.EVENT_LISTENER_PREFIX.length() + 1 );
				StringTokenizer st = new StringTokenizer( properties.getProperty( name ), " ,", false );
				List<String> listeners = new ArrayList<String>();
				while ( st.hasMoreElements() ) {
					listeners.add( (String) st.nextElement() );
				}
				configuration.setListeners( type, listeners.toArray( new String[listeners.size()] ) );
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void configure() {
		//TODO exclude pure hbm file classes?
		//TODO move it to each event listener initialize()?
		EntityCallbackHandler callbackHandler = new EntityCallbackHandler();
		configuration.buildMappings(); //needed to get all the classes
		Iterator classes = configuration.getClassMappings();
		ReflectionManager reflectionManager = configuration.getHibernateConfiguration().getReflectionManager();
		while ( classes.hasNext() ) {
			PersistentClass clazz = (PersistentClass) classes.next();
			if ( clazz.getClassName() != null ) {
				//we can have non java class persisted by hibernate
				try {
					callbackHandler.add( reflectionManager.classForName( clazz.getClassName(), this.getClass() ), reflectionManager );
				}
				catch (ClassNotFoundException e) {
					throw new MappingException( "entity class not found: " + clazz.getNodeName(), e );
				}
			}
		}

		EventListeners listenerConfig = configuration.getEventListeners();

		BeanInfo beanInfo = null;
		try {
			beanInfo = Introspector.getBeanInfo( listenerConfig.getClass(), Object.class );
			PropertyDescriptor[] pds = beanInfo.getPropertyDescriptors();
			try {
				for (int i = 0, max = pds.length; i < max; i++) {
					final Object listeners = pds[i].getReadMethod().invoke( listenerConfig, READER_METHOD_ARGS );
					if ( listeners == null ) {
						throw new HibernateException( "Listener [" + pds[i].getName() + "] was null" );
					}
					if ( listeners instanceof Object[] ) {
						int securityListenersNbr = 0;
						Object[] listenersArray = (Object[]) listeners;
						for (Object listener : listenersArray) {
							if ( listener != null && listener instanceof CallbackHandlerConsumer ) {
								( (CallbackHandlerConsumer) listener ).setCallbackHandler( callbackHandler );
							}
							if ( listener != null && listener instanceof JACCSecurityListener ) {
								if ( !isSecurity ) {
									securityListenersNbr++;
								}
							}
						}
						if ( !isSecurity ) {
							Class clazz = pds[i].getReadMethod().getReturnType().getComponentType();
							Object newArray = Array.newInstance( clazz, listenersArray.length - securityListenersNbr );
							int index = 0;
							for (Object listener : listenersArray) {
								if ( !( listener != null && listener instanceof JACCSecurityListener ) ) {
									Array.set( newArray, index++, listener );
								}
							}
							pds[i].getWriteMethod().invoke( listenerConfig, newArray );
						}
					}
				}
			}
			catch (HibernateException e) {
				throw e;
			}
			catch (Throwable t) {
				throw new HibernateException( "Unable to validate listener config", t );
			}
		}
		catch (Exception t) {
			throw new HibernateException( "Unable to copy listeners", t );
		}
		finally {
			if ( beanInfo != null ) {
				// release the jdk internal caches everytime to ensure this
				// plays nicely with destroyable class-loaders
				Introspector.flushFromCaches( getClass() );
			}
		}
	}
}
