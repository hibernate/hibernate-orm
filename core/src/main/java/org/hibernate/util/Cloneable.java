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
package org.hibernate.util;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.hibernate.HibernateException;

/**
 * An object that is shallow-coneable
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
			for ( int i = 0, max = pds.length; i < max; i++ ) {
				try {
					pds[i].getWriteMethod().invoke(
							copy,
							new Object[] {
								pds[i].getReadMethod().invoke( this, READER_METHOD_ARGS )
							}
						);
				}
				catch( Throwable t ) {
					throw new HibernateException( "Unable copy copy listener [" + pds[i].getName() + "]" );
				}
			}
		}
		catch( Exception t ) {
			throw new HibernateException( "Unable to copy listeners", t );
		}
		finally {
			if ( beanInfo != null ) {
				// release the jdk internal caches everytime to ensure this
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
		catch( IntrospectionException t ) {
			throw new HibernateException( "Unable to validate listener config", t );
		}
		finally {
			if ( beanInfo != null ) {
				// release the jdk internal caches everytime to ensure this
				// plays nicely with destroyable class-loaders
				Introspector.flushFromCaches( getClass() );
			}
		}
	}

	private void internalCheckListeners(BeanInfo beanInfo) {
		PropertyDescriptor[] pds = beanInfo.getPropertyDescriptors();
		try {
			for ( int i = 0, max = pds.length; i < max; i++ ) {
				final Object listener = pds[i].getReadMethod().invoke( this, READER_METHOD_ARGS );
				if ( listener == null ) {
					throw new HibernateException( "Listener [" + pds[i].getName() + "] was null" );
				}
				if ( listener.getClass().isArray() ) {
					Object[] listenerArray = (Object[]) listener;
					int length = listenerArray.length;
					for ( int index = 0 ; index < length ; index++ ) {
						if ( listenerArray[index] == null ) {
							throw new HibernateException( "Listener in [" + pds[i].getName() + "] was null" );
						}
					}
				}
			}
		}
		catch( HibernateException e ) {
			throw e;
		}
		catch( Throwable t ) {
			throw new HibernateException( "Unable to validate listener config" );
		}
	}
	
}
