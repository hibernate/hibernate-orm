/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
 * Base MBean impl
 *
 * @author gkeim
 */
public abstract class AbstractEmitterBean extends StandardMBean implements NotificationEmitter {
	private final Emitter emitter = new Emitter();
	private final AtomicLong sequenceNumber = new AtomicLong();

	private final List<NotificationListener> notificationListeners = new CopyOnWriteArrayList<NotificationListener>();

	/**
	 * Constructs a AbstractEmitterBean
	 *
	 * @param mbeanInterface The MBean contract
	 * @param <T> Not used as far as I can see
	 *
	 * @throws javax.management.NotCompliantMBeanException thrown from JMX super ctor
	 */
	protected <T> AbstractEmitterBean(Class<T> mbeanInterface) throws NotCompliantMBeanException {
		super( mbeanInterface );
	}

	/**
	 * Sends notification of an event
	 *
	 * @param eventType The type of event
	 */
	public void sendNotification(String eventType) {
		sendNotification( eventType, null, null );
	}

	/**
	 * Sends notification of an event
	 *
	 * @param eventType The type of event
	 * @param data The event data
	 */
	public void sendNotification(String eventType, Object data) {
		sendNotification( eventType, data, null );
	}

	/**
	 * Sends notification of an event
	 *
	 * @param eventType The type of event
	 * @param data The event data
	 * @param msg A message
	 */
	public void sendNotification(String eventType, Object data, String msg) {
		final Notification notification = new Notification(
				eventType,
				this,
				sequenceNumber.incrementAndGet(),
				System.currentTimeMillis(),
				msg
		);
		if ( data != null ) {
			notification.setUserData( data );
		}
		emitter.sendNotification( notification );
	}

	/**
	 * Dispose of this SampledCacheManager and clean up held resources
	 */
	@SuppressWarnings("UnusedDeclaration")
	public final void dispose() {
		doDispose();
		removeAllNotificationListeners();
	}

	/**
	 * Dispose callback of subclasses
	 */
	protected abstract void doDispose();

	private class Emitter extends NotificationBroadcasterSupport {
		@Override
		public MBeanNotificationInfo[] getNotificationInfo() {
			return AbstractEmitterBean.this.getNotificationInfo();
		}
	}

	@Override
	public void addNotificationListener(NotificationListener notif, NotificationFilter filter, Object callBack) {
		emitter.addNotificationListener( notif, filter, callBack );
		notificationListeners.add( notif );
	}

	private void removeAllNotificationListeners() {
		for ( NotificationListener listener : notificationListeners ) {
			try {
				emitter.removeNotificationListener( listener );
			}
			catch (ListenerNotFoundException e) {
				// ignore
			}
		}
		notificationListeners.clear();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public abstract MBeanNotificationInfo[] getNotificationInfo();


	@Override
	public void removeNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
		emitter.removeNotificationListener( listener );
		notificationListeners.remove( listener );
	}

	@Override
	public void removeNotificationListener(NotificationListener notif, NotificationFilter filter, Object callBack)
			throws ListenerNotFoundException {
		emitter.removeNotificationListener( notif, filter, callBack );
		notificationListeners.remove( notif );
	}
}
