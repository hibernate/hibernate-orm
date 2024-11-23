/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.junit;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

/**
 * @author Steve Ebersole
 */
public class LoggingInspectionsExtension implements TestInstancePostProcessor, BeforeEachCallback {
	private static final String KEY = LoggingInspectionsExtension.class.getName();

	// todo (6.0) : have this implement `AfterEachCallback` support to reset after each test?

	@Override
	public void postProcessTestInstance(
			Object testInstance,
			ExtensionContext context) {
		resolveLoggingInspectionScope( testInstance, context );
	}

	@Override
	public void beforeEach(ExtensionContext context) {
		final Store extensionStore = locateExtensionStore( context.getRequiredTestInstance(), context );
		final LoggingInspectionsScope existing = (LoggingInspectionsScope) extensionStore.get( KEY );
		if ( existing != null ) {
			existing.resetWatchers();
		}
	}

	public static LoggingInspectionsScope resolveLoggingInspectionScope(Object testInstance, ExtensionContext context) {
		final Store extensionStore = locateExtensionStore( testInstance, context );
		final Object existing = extensionStore.get( KEY );
		if ( existing != null ) {
			return (LoggingInspectionsScope) existing;
		}

		// we'll need to create it...

		// find the annotation
		final LoggingInspections loggingInspections = testInstance.getClass().getAnnotation( LoggingInspections.class );

		// Create the scope and add to context store
		final LoggingInspectionsScope scope = new LoggingInspectionsScope( loggingInspections, context );
		extensionStore.put( KEY, scope );

		return scope;
	}

	private static Store locateExtensionStore(Object testInstance, ExtensionContext context) {
		return JUnitHelper.locateExtensionStore( EntityManagerFactoryExtension.class, context, testInstance );
	}
}
