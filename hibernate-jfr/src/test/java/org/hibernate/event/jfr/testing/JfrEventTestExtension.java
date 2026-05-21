/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.jfr.testing;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

/**
 * JUnit 5 extension that manages JFR recording lifecycle for test classes
 * annotated with {@link JfrEventTest}.
 *
 * <p>Before each test method, reads the {@link EnableEvent} annotations to
 * determine which JFR event types to record, finds the {@link JfrEvents} field
 * on the test instance, and starts the recording.
 * After each test method, stops the recording and cleans up temporary files.</p>
 */
class JfrEventTestExtension implements BeforeEachCallback, AfterEachCallback {

	@Override
	public void beforeEach(final ExtensionContext context) throws Exception {
		final List<String> eventNames = readEnabledEventNames( context );
		for ( final JfrEvents jfrEvents : findJfrEventsFields( context ) ) {
			jfrEvents.configure( eventNames );
		}
	}

	@Override
	public void afterEach(final ExtensionContext context) throws Exception {
		for ( final JfrEvents jfrEvents : findJfrEventsFields( context ) ) {
			jfrEvents.cleanup();
		}
	}

	/**
	 * Extracts event type names from all {@link EnableEvent} annotations
	 * on the current test method.
	 */
	private static List<String> readEnabledEventNames(final ExtensionContext context) {
		final var annotations = AnnotationSupport.findRepeatableAnnotations(
				context.getRequiredTestMethod(),
				EnableEvent.class
		);
		final List<String> names = new ArrayList<>( annotations.size() );
		for ( final EnableEvent annotation : annotations ) {
			names.add( annotation.value() );
		}
		return names;
	}

	/**
	 * Finds all {@link JfrEvents} fields in the test instance's class hierarchy
	 * and returns their values.
	 */
	private static List<JfrEvents> findJfrEventsFields(final ExtensionContext context) throws IllegalAccessException {
		final Object testInstance = context.getRequiredTestInstance();
		final List<JfrEvents> result = new ArrayList<>();
		// walk the class hierarchy to handle potential JUnit proxying
		Class<?> clazz = testInstance.getClass();
		while ( clazz != null && clazz != Object.class ) {
			for ( final Field field : clazz.getDeclaredFields() ) {
				if ( JfrEvents.class.equals( field.getType() ) ) {
					field.setAccessible( true );
					final JfrEvents value = (JfrEvents) field.get( testInstance );
					if ( value == null ) {
						throw new IllegalStateException(
								"JfrEvents field '" + field.getName() + "' in " + clazz.getName()
										+ " is null — initialize it with 'new JfrEvents()'" );
					}
					result.add( value );
				}
			}
			clazz = clazz.getSuperclass();
		}
		return result;
	}
}
