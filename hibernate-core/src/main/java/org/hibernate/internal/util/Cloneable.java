/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.hibernate.HibernateException;

/**
 * An object that is shallow-cloneable
 *
 * @author Steve Ebersole
 */
public class Cloneable {

	private static final Object[] READER_METHOD_ARGS = new Object[0];

	/**
	 * Essentially performs a shallow copy of this SessionEventListenerConfig
	 * instance; meaning the SessionEventListenerConfig itself is cloned, but
	 * the individual listeners are <b>not</b> cloned.
	 *
	 * @return The SessionEventListenerConfig shallow copy.
	 */
	public Object shallowCopy() {
		return AccessController.doPrivileged(
				new PrivilegedAction() {
					@Override
					public Object run() {
						return copyListeners();
					}
				}
		);
	}

	/**
	 * Checks to ensure the SessionEventListenerConfig is fully
	 * configured (basically, that none of the listeners is null).
	 *
	 * @throws HibernateException If the SessionEventListenerConfig
	 * is not fully configured.
	 */
	public void validate() throws HibernateException {
		AccessController.doPrivileged(
				new PrivilegedAction() {
					@Override
					public Object run() {
						checkListeners();
						return null;
					}
				}
		);

	}

	private Object copyListeners() {
		Object copy = null;
		BeanInfo beanInfo = null;
		try {
			beanInfo = Introspector.getBeanInfo( getClass(), Object.class );
			internalCheckListeners( beanInfo );
			copy = getClass().newInstance();
			PropertyDescriptor[] pds = beanInfo.getPropertyDescriptors();
			for ( PropertyDescriptor pd : pds ) {
				try {
					pd.getWriteMethod().invoke(
							copy,
							pd.getReadMethod().invoke( this, READER_METHOD_ARGS )
					);
				}
				catch (Throwable t) {
					throw new HibernateException( "Unable copy copy listener [" + pd.getName() + "]" );
				}
			}
		}
		catch (Exception t) {
			throw new HibernateException( "Unable to copy listeners", t );
		}
		finally {
			if ( beanInfo != null ) {
				// release the jdk internal caches every time to ensure this
				// plays nicely with destroyable class-loaders
				Introspector.flushFromCaches( getClass() );
			}
		}

		return copy;
	}

	private void checkListeners() {
		BeanInfo beanInfo = null;
		try {
			beanInfo = Introspector.getBeanInfo( getClass(), Object.class );
			internalCheckListeners( beanInfo );
		}
		catch (IntrospectionException t) {
			throw new HibernateException( "Unable to validate listener config", t );
		}
		finally {
			if ( beanInfo != null ) {
				// release the jdk internal caches every time to ensure this
				// plays nicely with destroyable class-loaders
				Introspector.flushFromCaches( getClass() );
			}
		}
	}

	private void internalCheckListeners(BeanInfo beanInfo) {
		PropertyDescriptor[] pds = beanInfo.getPropertyDescriptors();
		try {
			for ( PropertyDescriptor pd : pds ) {
				final Object listener = pd.getReadMethod().invoke( this, READER_METHOD_ARGS );
				if ( listener == null ) {
					throw new HibernateException( "Listener [" + pd.getName() + "] was null" );
				}
				if ( listener.getClass().isArray() ) {
					Object[] listenerArray = (Object[]) listener;
					for ( Object aListenerArray : listenerArray ) {
						if ( aListenerArray == null ) {
							throw new HibernateException( "Listener in [" + pd.getName() + "] was null" );
						}
					}
				}
			}
		}
		catch (HibernateException e) {
			throw e;
		}
		catch (Throwable t) {
			throw new HibernateException( "Unable to validate listener config" );
		}
	}

}
