/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
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
		HashSet<Delegate> delegates = new HashSet<Delegate>();
		delegates.add( JavassistDelegate.INSTANCE );
		return delegates;
	}

	private FieldInterceptionHelper() {
	}

	public static boolean isInstrumented(Class entityClass) {
		for ( Delegate delegate : INSTRUMENTATION_DELEGATES ) {
			if ( delegate.isInstrumented( entityClass ) ) {
				return true;
			}
		}
		return false;
	}

	public static boolean isInstrumented(Object entity) {
		return entity != null && isInstrumented( entity.getClass() );
	}

	public static FieldInterceptor extractFieldInterceptor(Object entity) {
		if ( entity == null ) {
			return null;
		}
		FieldInterceptor interceptor = null;
		for ( Delegate delegate : INSTRUMENTATION_DELEGATES ) {
			interceptor = delegate.extractInterceptor( entity );
			if ( interceptor != null ) {
				break;
			}
		}
		return interceptor;
	}


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
}
