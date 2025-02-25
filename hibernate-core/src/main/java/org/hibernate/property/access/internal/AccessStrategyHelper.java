/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.property.access.internal;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.hibernate.PropertyNotFoundException;
import org.hibernate.bytecode.enhance.spi.interceptor.BytecodeLazyAttributeInterceptor;
import org.hibernate.engine.spi.CompositeOwner;
import org.hibernate.engine.spi.CompositeTracker;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.internal.util.NullnessUtil;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Transient;
import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.engine.internal.ManagedTypeHelper.asCompositeOwner;
import static org.hibernate.engine.internal.ManagedTypeHelper.asCompositeTracker;
import static org.hibernate.engine.internal.ManagedTypeHelper.asPersistentAttributeInterceptable;
import static org.hibernate.engine.internal.ManagedTypeHelper.isCompositeTracker;
import static org.hibernate.engine.internal.ManagedTypeHelper.isPersistentAttributeInterceptableType;
import static org.hibernate.internal.util.ReflectHelper.NO_PARAM_SIGNATURE;
import static org.hibernate.internal.util.ReflectHelper.findField;
import static org.hibernate.internal.util.ReflectHelper.getterMethodOrNull;
import static org.hibernate.internal.util.ReflectHelper.isRecord;

/**
 * @author Steve Ebersole
 */
public class AccessStrategyHelper {
	public static final int COMPOSITE_TRACKER_MASK = 1;
	public static final int COMPOSITE_OWNER = 2;
	public static final int PERSISTENT_ATTRIBUTE_INTERCEPTABLE_MASK = 4;

	public static @Nullable Field fieldOrNull(Class<?> containerJavaType, String propertyName) {
		try {
			return findField( containerJavaType, propertyName );
		}
		catch (PropertyNotFoundException e) {
			return null;
		}
	}

	public static AccessType getAccessType(Class<?> containerJavaType, String propertyName) {
		final Field field = fieldOrNull( containerJavaType, propertyName );
		final AccessType explicitAccessType = getExplicitAccessType( containerJavaType, propertyName, field );
		if ( explicitAccessType != null ) {
			return explicitAccessType;
		}

		// No @Access on property or field; check to see if containerJavaType has an explicit @Access
		AccessType classAccessType = getAccessTypeOrNull( containerJavaType );
		if ( classAccessType != null ) {
			return classAccessType;
		}

		// prefer using the field for getting if we can
		return field != null ? AccessType.FIELD : AccessType.PROPERTY;
	}

	public static @Nullable AccessType getExplicitAccessType(Class<?> containerClass, String propertyName, @Nullable Field field) {
		if ( isRecord( containerClass ) ) {
			try {
				containerClass.getMethod( propertyName, NO_PARAM_SIGNATURE );
				return AccessType.PROPERTY;
			}
			catch (NoSuchMethodException e) {
				// Ignore
			}
		}

		if ( field != null
				&& field.isAnnotationPresent( Access.class )
				&& !field.isAnnotationPresent( Transient.class )
				&& !Modifier.isStatic( field.getModifiers() ) ) {
			return NullnessUtil.castNonNull( field.getAnnotation( Access.class ) ).value();
		}

		final Method getter = getterMethodOrNull( containerClass, propertyName );
		if ( getter != null && getter.isAnnotationPresent( Access.class ) ) {
			return NullnessUtil.castNonNull( getter.getAnnotation( Access.class ) ).value();
		}

		return null;
	}

	protected static @Nullable AccessType getAccessTypeOrNull(@Nullable AnnotatedElement element) {
		if ( element == null ) {
			return null;
		}
		Access elementAccess = element.getAnnotation( Access.class );
		return elementAccess == null ? null : elementAccess.value();
	}

	public static int determineEnhancementState(Class<?> containerClass, Class<?> attributeType) {
		return ( CompositeOwner.class.isAssignableFrom( containerClass ) ? AccessStrategyHelper.COMPOSITE_OWNER : 0 )
				| ( CompositeTracker.class.isAssignableFrom( attributeType ) ? AccessStrategyHelper.COMPOSITE_TRACKER_MASK : 0 )
				| ( isPersistentAttributeInterceptableType( containerClass ) ? AccessStrategyHelper.PERSISTENT_ATTRIBUTE_INTERCEPTABLE_MASK : 0 );
	}

	public static void handleEnhancedInjection(Object target, @Nullable Object value, int enhancementState, String propertyName) {
		// This sets the component relation for dirty tracking purposes
		if ( ( enhancementState & COMPOSITE_OWNER ) != 0
				&& ( ( enhancementState & COMPOSITE_TRACKER_MASK ) != 0
				&& value != null
				|| isCompositeTracker( value ) ) ) {
			asCompositeTracker( NullnessUtil.castNonNull(value) ).$$_hibernate_setOwner( propertyName, asCompositeOwner( target ) );
		}

		// This marks the attribute as initialized, so it doesn't get lazily loaded afterward
		if ( ( enhancementState & PERSISTENT_ATTRIBUTE_INTERCEPTABLE_MASK ) != 0 ) {
			PersistentAttributeInterceptor interceptor = asPersistentAttributeInterceptable( target ).$$_hibernate_getInterceptor();
			if ( interceptor instanceof BytecodeLazyAttributeInterceptor ) {
				( (BytecodeLazyAttributeInterceptor) interceptor ).attributeInitialized( propertyName );
			}
		}
	}
}
