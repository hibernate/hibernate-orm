/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.junit;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

import org.hibernate.Internal;

import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;

import org.jboss.logging.Logger;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.create;

/**
 * @author Steve Ebersole
 */
@Internal
public class JUnitHelper {
	private static final Logger log = Logger.getLogger( JUnitHelper.class );

	public static final String CALLBACKS_KEY = "CALLBACKS";

	public static ExtensionContext.Store locateExtensionStore(
			Class<? extends Extension> extensionClass,
			ExtensionContext context,
			Object scopeObject) {
		return context.getStore( create( extensionClass.getName(), scopeObject ) );
	}

	public static ExtensionContext.Store locateExtensionStore(
			ExtensionContext context,
			Object... scopeRefs) {
		return context.getStore( create( scopeRefs ) );
	}

	public static ExtensionContext.Store locateClassExtensionStore(
			Class<? extends Extension> extensionClass,
			ExtensionContext context) {
		return context.getStore( create(  ));
	}

	public static ExtensionContext.Namespace createNamespace(
			Class<? extends Extension> extensionClass,
			ExtensionContext context) {
		return ExtensionContext.Namespace.create(
				extensionClass.getName(),
				context.getRequiredTestMethod().getClass(),
				context.getRequiredTestMethod().getName()
		);
	}

	public static ExtensionContext.Namespace createClassNamespace(
			Class<? extends Extension> extensionClass,
			ExtensionContext context) {
		return create( extensionClass.getName(), context.getRequiredTestClass() );
	}

	public static void discoverCallbacks(
			ExtensionContext context,
			Class<? extends Extension> extensionType,
			Class<? extends Annotation> callbackAnnotationType) {
		final List<Method> callbacks = TestingUtil.findAnnotatedMethods( context, callbackAnnotationType );

		final ExtensionContext.Store store = context.getStore( createClassNamespace( extensionType, context ) );
		store.put( CALLBACKS_KEY, callbacks );
	}

	public static void invokeCallbacks(ExtensionContext context, Class<? extends Extension> extensionType) {
		invokeCallbacks( context, extensionType, CALLBACKS_KEY );
	}

	public static void cleanupCallbacks(ExtensionContext context, Class<? extends Extension> extensionType) {
		final ExtensionContext.Store store = context.getStore( createClassNamespace( extensionType, context ) );
		store.remove( CALLBACKS_KEY );
	}

	public static List<Method> locateCallbacks(
			ExtensionContext context,
			Class<? extends Annotation> callbackAnnotationType) {
		return TestingUtil.findAnnotatedMethods( context, callbackAnnotationType );
	}

	public static void invokeCallbacks(ExtensionContext context, Class<? extends Extension> extensionType, String callbacksKey) {
		// NOTE : callbacks are kept relative to the class rather than the instance

		final ExtensionContext.Store store = context.getStore( createClassNamespace( extensionType, context ) );
		//noinspection unchecked
		final List<Method> callbacks = (List<Method>) store.get( callbacksKey );
		if ( callbacks == null ) {
			return;
		}

		callbacks.forEach( (callback) -> {
			try {
				context.getExecutableInvoker().invoke( callback, context.getRequiredTestInstance() );
			}
			catch (Exception e) {
				log.warnf( e, "Error invoking FailureExpectedCallback handling : `%`", callback.getName() );
			}
		} );
	}

	private JUnitHelper() {
	}

	public static boolean supportsParameterInjection(ParameterContext parameterContext, Class<?>... supportedTypes) {
		for ( Class<?> supportedType : supportedTypes ) {
			if ( parameterContext.getParameter().getType().isAssignableFrom( supportedType ) ) {
				return true;
			}
		}

		return false;
	}
}
