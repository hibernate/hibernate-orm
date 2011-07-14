/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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

package org.hibernate.cache.ehcache.management.impl;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.StandardMBean;

/**
 * @author gkeim
 */
public abstract class BaseEmitterBean extends StandardMBean implements NotificationEmitter {
	/**
	 * emitter
	 */
	protected final Emitter emitter = new Emitter();

	/**
	 * sequenceNumber
	 */
	protected final AtomicLong sequenceNumber = new AtomicLong();


	private final List<NotificationListener> notificationListeners = new CopyOnWriteArrayList<NotificationListener>();

	/**
	 * BaseEmitterBean
	 *
	 * @param <T>
	 * @param mbeanInterface
	 *
	 * @throws javax.management.NotCompliantMBeanException
	 */
	protected <T> BaseEmitterBean(Class<T> mbeanInterface) throws NotCompliantMBeanException {
		super( mbeanInterface );
	}

	/**
	 * sendNotification
	 *
	 * @param eventType
	 */
	public void sendNotification(String eventType) {
		sendNotification( eventType, null, null );
	}

	/**
	 * sendNotification
	 *
	 * @param eventType
	 * @param data
	 */
	public void sendNotification(String eventType, Object data) {
		sendNotification( eventType, data, null );
	}

	/**
	 * sendNotification
	 *
	 * @param eventType
	 * @param data
	 * @param msg
	 */
	public void sendNotification(String eventType, Object data, String msg) {
		Notification notif = new Notification(
				eventType,
				this,
				sequenceNumber.incrementAndGet(),
				System.currentTimeMillis(),
				msg
		);
		if ( data != null ) {
			notif.setUserData( data );
		}
		emitter.sendNotification( notif );
	}

	/**
	 * Dispose of this SampledCacheManager and clean up held resources
	 */
	public final void dispose() {
		doDispose();
		removeAllNotificationListeners();
	}

	/**
	 * Dispose callback of subclasses
	 */
	protected abstract void doDispose();

	/**
	 * @author gkeim
	 */
	private class Emitter extends NotificationBroadcasterSupport {
		/**
		 * @see javax.management.NotificationBroadcasterSupport#getNotificationInfo()
		 */
		@Override
		public MBeanNotificationInfo[] getNotificationInfo() {
			return BaseEmitterBean.this.getNotificationInfo();
		}
	}

	/**
	 * @see javax.management.NotificationBroadcaster#addNotificationListener(javax.management.NotificationListener,
	 *	  javax.management.NotificationFilter, java.lang.Object)
	 */
	public void addNotificationListener(NotificationListener notif, NotificationFilter filter, Object callBack) {
		emitter.addNotificationListener( notif, filter, callBack );
		notificationListeners.add( notif );
	}

	/**
	 * remove all added notification listeners
	 */
	private void removeAllNotificationListeners() {
		for ( NotificationListener listener : notificationListeners ) {
			try {
				emitter.removeNotificationListener( listener );
			}
			catch ( ListenerNotFoundException e ) {
				// ignore
			}
		}
		notificationListeners.clear();
	}

	/**
	 * @see javax.management.NotificationBroadcaster#getNotificationInfo()
	 */
	public abstract MBeanNotificationInfo[] getNotificationInfo();


	/**
	 * @see javax.management.NotificationBroadcaster#removeNotificationListener(javax.management.NotificationListener)
	 */
	public void removeNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
		emitter.removeNotificationListener( listener );
		notificationListeners.remove( listener );
	}

	/**
	 * @see javax.management.NotificationEmitter#removeNotificationListener(javax.management.NotificationListener,
	 *	  javax.management.NotificationFilter, java.lang.Object)
	 */
	public void removeNotificationListener(NotificationListener notif, NotificationFilter filter, Object callBack)
			throws ListenerNotFoundException {
		emitter.removeNotificationListener( notif, filter, callBack );
		notificationListeners.remove( notif );
	}
}
