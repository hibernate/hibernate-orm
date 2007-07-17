//$Id: Cloneable.java 8670 2005-11-25 17:36:29Z epbernard $
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
