/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.property.access.spi;

import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeLoadingInterceptor;
import org.hibernate.engine.spi.CompositeOwner;
import org.hibernate.engine.spi.CompositeTracker;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import java.lang.reflect.Field;

/**
 * A specialized Setter implementation for handling setting values into
 * a into a bytecode-enhanced Class.  The reason we need specialized handling
 * is to render the fact that we need to account for certain enhancement features
 * during the setting process.
 *
 * @author Steve Ebersole
 * @author Luis Barreiro
 */
public class EnhancedSetterImpl extends SetterFieldImpl {

	private final String propertyName;

	public EnhancedSetterImpl(Class containerClass, String propertyName, Field field) {
		super( containerClass, propertyName, field );
		this.propertyName = propertyName;
	}

	@Override
	public void set(Object target, Object value, SessionFactoryImplementor factory) {

		super.set( target, value, factory );

		// This sets the component relation for dirty tracking purposes
		if ( target instanceof CompositeOwner && value instanceof CompositeTracker ) {
			( (CompositeTracker) value ).$$_hibernate_setOwner( propertyName, (CompositeOwner) target );
		}

		// This marks the attribute as initialized, so it doesn't get lazy loaded afterwards
		if ( target instanceof PersistentAttributeInterceptable ) {
			PersistentAttributeInterceptor interceptor = ( (PersistentAttributeInterceptable) target ).$$_hibernate_getInterceptor();
			if ( interceptor != null && interceptor instanceof LazyAttributeLoadingInterceptor ) {
				interceptor.attributeInitialized( propertyName );
			}
		}
	}
}
