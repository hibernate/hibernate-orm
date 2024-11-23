/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.junit;

import java.lang.reflect.Method;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.create;

/**
 * @author Steve Ebersole
 */
public class MessageKeyInspectionExtension implements TestInstancePostProcessor, BeforeEachCallback {
	public static final String KEY = LoggingInspectionsExtension.class.getName();

	@Override
	public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
		// Process the MessageKeyInspection annotation that happens at the class-level

		final ExtensionContext.Store instanceStore = resolveInstanceStore( testInstance, context );

		final Object existing = instanceStore.get( KEY );
		if ( existing != null ) {
			// odd, but there would be nothing to do
			return;
		}

		// find the annotation, create the watcher and add it to the context
		final MessageKeyInspection inspection = testInstance.getClass().getAnnotation( MessageKeyInspection.class );
		final MessageKeyWatcherImpl watcher = new MessageKeyWatcherImpl( inspection.messageKey() );
		watcher.addLogger( inspection.logger() );

		instanceStore.put( KEY, watcher );
	}

	public static ExtensionContext.Store resolveInstanceStore(Object testInstance, ExtensionContext context) {
		final ExtensionContext.Namespace instanceStoreNamespace = create( testInstance );
		return context.getStore( instanceStoreNamespace );
	}

	@Override
	public void beforeEach(ExtensionContext context) {
		final Method method = context.getRequiredTestMethod();

		final ExtensionContext.Store methodStore = resolveMethodStore( context );
		final MessageKeyWatcher existing = (MessageKeyWatcher) methodStore.get( KEY );
		if ( existing != null ) {
			prepareForUse( existing );
			// already there - nothing to do
			return;
		}

		// if the test-method is annotated, use a one-off watcher for that message
		final MessageKeyInspection inspectionAnn = method.getAnnotation( MessageKeyInspection.class );
		if ( inspectionAnn != null ) {
			final MessageKeyWatcherImpl watcher = new MessageKeyWatcherImpl( inspectionAnn.messageKey() );
			watcher.addLogger( inspectionAnn.logger() );
			methodStore.put( KEY, watcher );
			prepareForUse( watcher );
			return;
		}

		// look for a class/instance-level watcher
		final ExtensionContext.Store instanceStore = resolveInstanceStore( context.getRequiredTestInstance(), context );
		final MessageKeyWatcher instanceLevelWatcher = (MessageKeyWatcher) instanceStore.get( KEY );
		if ( instanceLevelWatcher != null ) {
			methodStore.put( KEY, instanceLevelWatcher );
			prepareForUse( instanceLevelWatcher );
		}
	}

	private void prepareForUse(MessageKeyWatcher watcher) {
		watcher.reset();
	}

	public static ExtensionContext.Store resolveMethodStore(ExtensionContext context) {
		final ExtensionContext.Namespace instanceStoreNamespace = create( context.getRequiredTestMethod() );
		return context.getStore( instanceStoreNamespace );
	}

	public static MessageKeyWatcher getWatcher(ExtensionContext context) {
		final ExtensionContext.Store methodStore = resolveMethodStore( context );
		final Object ref = methodStore.get( KEY );
		if ( ref == null ) {
			throw new IllegalStateException( "No watcher available" );
		}
		return (MessageKeyWatcher) ref;
	}

}
