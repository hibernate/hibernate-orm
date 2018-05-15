/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.instantiation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author Steve Ebersole
 */
class BeanInjectorSetter<T> implements BeanInjector<T> {
	private final Method setter;

	public BeanInjectorSetter(Method setter) {
		this.setter = setter;
	}

	@Override
	public void inject(T target, Object value) {
		try {
			setter.invoke( target, value );
		}
		catch (InvocationTargetException e) {
			throw new InstantiationException( "Error performing the dynamic instantiation", e.getCause() );
		}
		catch (Exception e) {
			throw new InstantiationException( "Error performing the dynamic instantiation", e );
		}
	}
}
