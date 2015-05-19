/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util.beans;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.lang.reflect.InvocationTargetException;

/**
 * Utility for helping deal with {@link BeanInfo}
 *
 * @author Steve Ebersole
 */
public class BeanInfoHelper {
	public static interface BeanInfoDelegate {
		public void processBeanInfo(BeanInfo beanInfo) throws Exception;
	}

	public static interface ReturningBeanInfoDelegate<T> {
		public T processBeanInfo(BeanInfo beanInfo) throws Exception;
	}

	private final Class beanClass;
	private final Class stopClass;

	public BeanInfoHelper(Class beanClass) {
		this( beanClass, Object.class );
	}

	public BeanInfoHelper(Class beanClass, Class stopClass) {
		this.beanClass = beanClass;
		this.stopClass = stopClass;
	}

	public void applyToBeanInfo(Object bean, BeanInfoDelegate delegate) {
		if ( ! beanClass.isInstance( bean ) ) {
			throw new BeanIntrospectionException( "Bean [" + bean + "] was not of declared bean type [" + beanClass.getName() + "]" );
		}

		visitBeanInfo( beanClass, stopClass, delegate );
	}

	public static void visitBeanInfo(Class beanClass, BeanInfoDelegate delegate) {
		visitBeanInfo( beanClass, Object.class, delegate );
	}

	public static void visitBeanInfo(Class beanClass, Class stopClass, BeanInfoDelegate delegate) {
		try {
			BeanInfo info = Introspector.getBeanInfo( beanClass, stopClass );
			try {
				delegate.processBeanInfo( info );
			}
			catch ( RuntimeException e ) {
				throw e;
			}
			catch ( InvocationTargetException e ) {
				throw new BeanIntrospectionException( "Error delegating bean info use", e.getTargetException() );
			}
			catch ( Exception e ) {
				throw new BeanIntrospectionException( "Error delegating bean info use", e );
			}
			finally {
				Introspector.flushFromCaches( beanClass );
			}
		}
		catch ( IntrospectionException e ) {
			throw new BeanIntrospectionException( "Unable to determine bean info from class [" + beanClass.getName() + "]", e );
		}
	}

	public static <T> T visitBeanInfo(Class beanClass, ReturningBeanInfoDelegate<T> delegate) {
		return visitBeanInfo( beanClass, null, delegate );
	}

	public static <T> T visitBeanInfo(Class beanClass, Class stopClass, ReturningBeanInfoDelegate<T> delegate) {
		try {
			BeanInfo info = Introspector.getBeanInfo( beanClass, stopClass );
			try {
				return delegate.processBeanInfo( info );
			}
			catch ( RuntimeException e ) {
				throw e;
			}
			catch ( InvocationTargetException e ) {
				throw new BeanIntrospectionException( "Error delegating bean info use", e.getTargetException() );
			}
			catch ( Exception e ) {
				throw new BeanIntrospectionException( "Error delegating bean info use", e );
			}
			finally {
				Introspector.flushFromCaches( beanClass );
			}
		}
		catch ( IntrospectionException e ) {
			throw new BeanIntrospectionException( "Unable to determine bean info from class [" + beanClass.getName() + "]", e );
		}
	}


}
