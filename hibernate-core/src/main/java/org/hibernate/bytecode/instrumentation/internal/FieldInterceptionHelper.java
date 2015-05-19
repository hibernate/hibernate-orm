/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.instrumentation.internal;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.bytecode.instrumentation.internal.javassist.JavassistHelper;
import org.hibernate.bytecode.instrumentation.spi.FieldInterceptor;
import org.hibernate.engine.spi.SessionImplementor;

/**
 * Helper class for dealing with enhanced entity classes.
 *
 * These operations are expensive.  They are only meant to be used when code does not have access to a
 * SessionFactory (namely from the instrumentation tasks).  When code has access to a SessionFactory,
 * {@link org.hibernate.bytecode.spi.EntityInstrumentationMetadata} should be used instead to query the
 * instrumentation state.  EntityInstrumentationMetadata is accessed from the
 * {@link org.hibernate.persister.entity.EntityPersister} via the
 * {@link org.hibernate.persister.entity.EntityPersister#getInstrumentationMetadata()} method.
 *
 * @author Steve Ebersole
 */
public class FieldInterceptionHelper {
	private static final Set<Delegate> INSTRUMENTATION_DELEGATES = buildInstrumentationDelegates();

	private static Set<Delegate> buildInstrumentationDelegates() {
		final HashSet<Delegate> delegates = new HashSet<Delegate>();
		delegates.add( JavassistDelegate.INSTANCE );
		return delegates;
	}

	/**
	 * Utility to check to see if a given entity class is instrumented.
	 *
	 * @param entityClass The entity class to check
	 *
	 * @return {@code true} if it has been instrumented; {@code false} otherwise
	 */
	public static boolean isInstrumented(Class entityClass) {
		for ( Delegate delegate : INSTRUMENTATION_DELEGATES ) {
			if ( delegate.isInstrumented( entityClass ) ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Utility to check to see if a given object is an instance of an instrumented class.  If the instance
	 * is {@code null}, the check returns {@code false}
	 *
	 * @param object The object to check
	 *
	 * @return {@code true} if it has been instrumented; {@code false} otherwise
	 */
	public static boolean isInstrumented(Object object) {
		return object != null && isInstrumented( object.getClass() );
	}

	/**
	 * Assuming the given object is an enhanced entity, extract and return its interceptor.  Will
	 * return {@code null} if object is {@code null}, or if the object was deemed to not be
	 * instrumented
	 *
	 * @param object The object from which to extract the interceptor
	 *
	 * @return The extracted interceptor, or {@code null}
	 */
	public static FieldInterceptor extractFieldInterceptor(Object object) {
		if ( object == null ) {
			return null;
		}
		FieldInterceptor interceptor = null;
		for ( Delegate delegate : INSTRUMENTATION_DELEGATES ) {
			interceptor = delegate.extractInterceptor( object );
			if ( interceptor != null ) {
				break;
			}
		}
		return interceptor;
	}

	/**
	 * Assuming the given object is an enhanced entity, inject a field interceptor.
	 *
	 * @param entity The entity instance
	 * @param entityName The entity name
	 * @param uninitializedFieldNames The names of any uninitialized fields
	 * @param session The session
	 *
	 * @return The injected interceptor
	 */
	public static FieldInterceptor injectFieldInterceptor(
			Object entity,
			String entityName,
			Set uninitializedFieldNames,
			SessionImplementor session) {
		if ( entity == null ) {
			return null;
		}
		FieldInterceptor interceptor = null;
		for ( Delegate delegate : INSTRUMENTATION_DELEGATES ) {
			interceptor = delegate.injectInterceptor( entity, entityName, uninitializedFieldNames, session );
			if ( interceptor != null ) {
				break;
			}
		}
		return interceptor;
	}

	private static interface Delegate {
		public boolean isInstrumented(Class classToCheck);
		public FieldInterceptor extractInterceptor(Object entity);
		public FieldInterceptor injectInterceptor(Object entity, String entityName, Set uninitializedFieldNames, SessionImplementor session);
	}

	private static class JavassistDelegate implements Delegate {
		public static final JavassistDelegate INSTANCE = new JavassistDelegate();
		public static final String MARKER = "org.hibernate.bytecode.internal.javassist.FieldHandled";

		@Override
		public boolean isInstrumented(Class classToCheck) {
			for ( Class definedInterface : classToCheck.getInterfaces() ) {
				if ( MARKER.equals( definedInterface.getName() ) ) {
					return true;
				}
			}
			return false;
		}

		@Override
		public FieldInterceptor extractInterceptor(Object entity) {
			for ( Class definedInterface : entity.getClass().getInterfaces() ) {
				if ( MARKER.equals( definedInterface.getName() ) ) {
					return JavassistHelper.extractFieldInterceptor( entity );
				}
			}
			return null;
		}

		@Override
		public FieldInterceptor injectInterceptor(
				Object entity,
				String entityName,
				Set uninitializedFieldNames,
				SessionImplementor session) {
			for ( Class definedInterface : entity.getClass().getInterfaces() ) {
				if ( MARKER.equals( definedInterface.getName() ) ) {
					return JavassistHelper.injectFieldInterceptor( entity, entityName, uninitializedFieldNames, session );
				}
			}
			return null;
		}
	}

	private FieldInterceptionHelper() {
	}

}
