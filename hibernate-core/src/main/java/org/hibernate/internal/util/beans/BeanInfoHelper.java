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


}
