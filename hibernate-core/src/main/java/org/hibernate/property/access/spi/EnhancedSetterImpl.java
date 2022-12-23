/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.property.access.spi;

import java.io.Serializable;
import java.lang.reflect.Field;

import org.hibernate.bytecode.enhance.spi.interceptor.BytecodeLazyAttributeInterceptor;
import org.hibernate.engine.internal.ManagedTypeHelper;
import org.hibernate.engine.spi.CompositeOwner;
import org.hibernate.engine.spi.CompositeTracker;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.property.access.internal.AbstractFieldSerialForm;

import static org.hibernate.engine.internal.ManagedTypeHelper.asCompositeOwner;
import static org.hibernate.engine.internal.ManagedTypeHelper.asCompositeTracker;
import static org.hibernate.engine.internal.ManagedTypeHelper.asPersistentAttributeInterceptable;
import static org.hibernate.engine.internal.ManagedTypeHelper.isCompositeTracker;
import static org.hibernate.engine.internal.ManagedTypeHelper.isPersistentAttributeInterceptableType;

/**
 * A specialized Setter implementation for handling setting values into
 * a bytecode-enhanced Class.  The reason we need specialized handling
 * is to render the fact that we need to account for certain enhancement features
 * during the setting process.
 *
 * @author Steve Ebersole
 * @author Luis Barreiro
 */
public class EnhancedSetterImpl extends SetterFieldImpl {

	private static final int COMPOSITE_TRACKER_MASK = 1;
	private static final int COMPOSITE_OWNER = 2;
	private static final int PERSISTENT_ATTRIBUTE_INTERCEPTABLE_MASK = 4;

	private final String propertyName;
	private final int enhancementState;

	public EnhancedSetterImpl(Class<?> containerClass, String propertyName, Field field) {
		super( containerClass, propertyName, field );
		this.propertyName = propertyName;
		this.enhancementState = ( CompositeOwner.class.isAssignableFrom( containerClass ) ? COMPOSITE_OWNER : 0 )
				| ( CompositeTracker.class.isAssignableFrom( field.getType() ) ? COMPOSITE_TRACKER_MASK : 0 )
				| ( isPersistentAttributeInterceptableType( containerClass ) ? PERSISTENT_ATTRIBUTE_INTERCEPTABLE_MASK : 0 );
	}

	@Override
	public void set(Object target, Object value) {
		super.set( target, value );

		// This sets the component relation for dirty tracking purposes
		if ( ( enhancementState & COMPOSITE_OWNER ) != 0 && ( ( enhancementState & COMPOSITE_TRACKER_MASK ) != 0 && value != null || isCompositeTracker( value ) ) ) {
			asCompositeTracker( value ).$$_hibernate_setOwner( propertyName, asCompositeOwner( target ) );
		}

		// This marks the attribute as initialized, so it doesn't get lazily loaded afterwards
		if ( ( enhancementState & PERSISTENT_ATTRIBUTE_INTERCEPTABLE_MASK ) != 0 ) {
			PersistentAttributeInterceptor interceptor = asPersistentAttributeInterceptable( target ).$$_hibernate_getInterceptor();
			if ( interceptor instanceof BytecodeLazyAttributeInterceptor ) {
				( (BytecodeLazyAttributeInterceptor) interceptor ).attributeInitialized( propertyName );
			}
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// serialization

	private Object writeReplace() {
		return new SerialForm( getContainerClass(), propertyName, getField() );
	}

	@SuppressWarnings("rawtypes")
	private static class SerialForm extends AbstractFieldSerialForm implements Serializable {
		private final Class containerClass;
		private final String propertyName;


		private SerialForm(Class containerClass, String propertyName, Field field) {
			super( field );
			this.containerClass = containerClass;
			this.propertyName = propertyName;
		}

		private Object readResolve() {
			return new EnhancedSetterImpl( containerClass, propertyName, resolveField() );
		}
	}
}
