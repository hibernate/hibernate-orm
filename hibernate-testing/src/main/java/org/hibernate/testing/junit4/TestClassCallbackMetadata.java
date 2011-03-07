/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.testing.junit4;

import org.hibernate.testing.AfterClassOnce;
import org.hibernate.testing.BeforeClassOnce;
import org.hibernate.testing.OnExpectedFailure;
import org.hibernate.testing.OnFailure;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;

/**
 * Metadata about various types of callback methods on a given test class.
 *
 * @author Steve Ebersole
 */
public class TestClassCallbackMetadata {
	private static final Object[] NO_ARGS = new Object[0];

	private LinkedHashSet<Method> beforeClassOnceMethods;
	private LinkedHashSet<Method> afterClassOnceMethods;
	private LinkedHashSet<Method> onFailureCallbacks;
	private LinkedHashSet<Method> onExpectedFailureCallbacks;

	public TestClassCallbackMetadata(Class testClass) {
		processClassHierarchy( testClass );
	}

	private void processClassHierarchy(Class testClass) {
		// NOTE recursive on itself
		for ( Method method : testClass.getDeclaredMethods() ) {
			if ( method.getAnnotation( CallbackType.BEFORE_CLASS_ONCE.annotationClass ) != null ) {
				addBeforeClassOnceCallback( method );
			}
			if ( method.getAnnotation( CallbackType.AFTER_CLASS_ONCE.annotationClass ) != null ) {
				addAfterClassOnceCallback( method );
			}
			if ( method.getAnnotation( CallbackType.ON_FAILURE.annotationClass ) != null ) {
				addOnFailureCallback( method );
			}
			if ( method.getAnnotation( CallbackType.ON_EXPECTED_FAILURE.annotationClass ) != null ) {
				addOnExpectedFailureCallback( method );
			}
		}

		Class superClass = testClass.getSuperclass();
		if ( superClass != null ) {
			processClassHierarchy( superClass );
		}
	}

	private static enum CallbackType {
		BEFORE_CLASS_ONCE( BeforeClassOnce.class ),
		AFTER_CLASS_ONCE( AfterClassOnce.class ),
		ON_FAILURE( OnFailure.class ),
		ON_EXPECTED_FAILURE( OnExpectedFailure.class );

		private final Class<? extends Annotation> annotationClass;

		private CallbackType(Class<? extends Annotation> annotationClass) {
			this.annotationClass = annotationClass;
		}

		public Class<? extends Annotation> getAnnotationClass() {
			return annotationClass;
		}

		public String buildTypeMarker() {
			return "@" + getAnnotationClass().getSimpleName();
		}
	}

	private void addBeforeClassOnceCallback(Method method) {
		validateCallbackMethod( method, CallbackType.BEFORE_CLASS_ONCE );
		beforeClassOnceMethods = new LinkedHashSet<Method>();
		beforeClassOnceMethods.add( method );
	}

	private void validateCallbackMethod(Method method, CallbackType type) {
		if ( method.getParameterTypes().length > 0 ) {
			throw new InvalidMethodForAnnotationException(
					type.buildTypeMarker() + " callback only valid on no-arg methods : "
							+ Helper.extractMethodName( method )
			);
		}
		if ( !method.isAccessible() ) {
			try {
				method.setAccessible( true );
			}
			catch (Exception e) {
				throw new InvalidMethodForAnnotationException(
						type.buildTypeMarker() + " attached to inaccessible method and unable to make accessible"
				);
			}
		}
	}

	private void addAfterClassOnceCallback(Method method) {
		validateCallbackMethod( method, TestClassCallbackMetadata.CallbackType.AFTER_CLASS_ONCE );
		afterClassOnceMethods = new LinkedHashSet<Method>();
		afterClassOnceMethods.add( method );
	}

	private void addOnFailureCallback(Method method) {
		validateCallbackMethod( method, TestClassCallbackMetadata.CallbackType.ON_FAILURE );
		onFailureCallbacks = new LinkedHashSet<Method>();
		onFailureCallbacks.add( method );
	}

	private void addOnExpectedFailureCallback(Method method) {
		validateCallbackMethod( method, TestClassCallbackMetadata.CallbackType.ON_EXPECTED_FAILURE );
		onExpectedFailureCallbacks = new LinkedHashSet<Method>();
		onExpectedFailureCallbacks.add( method );
	}


	public void performBeforeClassCallbacks(Object target) {
		performCallbacks( beforeClassOnceMethods, target );
	}

	private void performCallbacks(LinkedHashSet<Method> callbackMethods, Object target) {
		if ( callbackMethods == null ) {
			return;
		}

		for ( Method callbackMethod : callbackMethods ) {
			invokeCallback( callbackMethod, target );
		}
	}

	private void invokeCallback(Method callback, Object target) {
		try {
			callback.invoke( target, NO_ARGS );
		}
		catch (InvocationTargetException e) {
			throw new CallbackException( callback, e.getTargetException() );
		}
		catch (IllegalAccessException e) {
			throw new CallbackException( callback, e );
		}
	}

	public void performAfterClassCallbacks(Object target) {
		performCallbacks( afterClassOnceMethods, target );
	}

	public void performOnFailureCallback(Object target) {
		performCallbacks( onFailureCallbacks, target );
	}

	public void performOnExpectedFailureCallback(Object target) {
		performCallbacks( onExpectedFailureCallbacks, target );
	}
}
