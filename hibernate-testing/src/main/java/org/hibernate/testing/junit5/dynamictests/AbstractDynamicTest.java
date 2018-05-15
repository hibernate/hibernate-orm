/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.junit5.dynamictests;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.junit.platform.commons.util.ReflectionUtils.findMethods;

/**
 * Abstract base class that can be used to generate dynamic test-lets.
 *
 * @see DynamicBeforeAll
 * @see DynamicAfterAll
 * @see DynamicBeforeEach
 * @see DynamicAfterEach
 * @see DynamicTest
 *
 * @author Chris Cranford
 */
public abstract class AbstractDynamicTest<T extends DynamicExecutionContext> {
	@TestFactory
	@SuppressWarnings("unchecked")
	public List<DynamicNode> generateDynamicTestNodes() throws Exception {
		final Class<? extends AbstractDynamicTest> testClass = getClass();
		final List<DynamicNode> dynamicNodes = new ArrayList<>();

		final List<Method> beforeAllMethods = findMethods(
				testClass,
				method -> method.isAnnotationPresent( DynamicBeforeAll.class )
		);

		final List<Method> beforeEachMethods = findMethods(
				testClass,
				method -> method.isAnnotationPresent( DynamicBeforeEach.class )
		);

		final List<Method> afterAllMethods = findMethods(
				testClass,
				method -> method.isAnnotationPresent( DynamicAfterAll.class )
		);

		final List<Method> afterEachMethods = findMethods(
				testClass,
				method -> method.isAnnotationPresent( DynamicAfterEach.class )
		);

		final List<Method> testMethods = findMethods(
				testClass,
				method -> method.isAnnotationPresent( DynamicTest.class )
		);

		for ( final T context : getExecutionContexts() ) {
			if ( testClass.isAnnotationPresent( Disabled.class ) || !context.isExecutionAllowed( testClass ) ) {
				continue;
			}

			final AbstractDynamicTest testInstance = testClass.newInstance();
			final List<org.junit.jupiter.api.DynamicTest> tests = new ArrayList<>();

			// First invoke all @DynamicBeforeAll annotated methods
			beforeAllMethods.forEach( method -> {
				if ( !method.isAnnotationPresent( Disabled.class ) && context.isExecutionAllowed( method ) ) {
					tests.add( dynamicTest( method.getName(), () -> method.invoke( testInstance ) ) );
				}
			} );

			// Iterate @DynamicTest methods and invoke them, if applicable.
			//
			// The before/after methods aren't tested as they're ran paired with the actual
			// @DynamicTest method.  So to control whether the test runs, only the @DynamicTest
			// method is checked.
			testMethods.forEach( method -> {
				if ( !method.isAnnotationPresent( Disabled.class ) && context.isExecutionAllowed( method ) ) {
					final DynamicTest dynamicTestAnnotation = method.getAnnotation( DynamicTest.class );
					final Class<? extends Throwable> expectedException = dynamicTestAnnotation.expected();
					tests.add(
							dynamicTest(
									method.getName(),
									() -> {
										// invoke @DynamicBeforeEach
										for ( Method beforeEachMethod : beforeEachMethods ) {
											beforeEachMethod.invoke( testInstance );
										}

										Throwable exception = null;
										try {
											method.invoke( testInstance );

											// If the @DynamicTest annotation specifies an expected exception
											// and it wasn't thrown during the method invocation, we want to
											// assert here and fail the test node accordingly.
											assertEquals(
													DynamicTest.None.class,
													expectedException,
													"Expected: " + expectedException.getName()
											);
										}
										catch ( InvocationTargetException t ) {
											// only throw if the exception was not expected.
											if ( !expectedException.isInstance( t.getTargetException() ) ) {
												if ( t.getTargetException() != null ) {
													// in this use case, we only really care about the cause
													// we can safely ignore the wrapper exception here.
													exception = t.getTargetException();
													throw t.getTargetException();
												}
												else {
													exception = t;
													throw t;
												}
											}
										}
										catch ( Throwable t ) {
											exception = t;
											throw t;
										}
										finally {
											try {
												for ( Method afterEachMethod : afterEachMethods ) {
													afterEachMethod.invoke( testInstance );
												}
											}
											catch( Throwable t ) {
												if ( exception == null ) {
													throw t;
												}
											}
										}
									}
							)
					);
				}
			} );

			// Lastly invoke all @DynamicAfterAll annotated methods
			afterAllMethods.forEach( method -> {
				if ( context.isExecutionAllowed( method ) ) {
					tests.add( dynamicTest( method.getName(), () -> method.invoke( testInstance ) ) );
				}
			} );

			// Only if the tests are not empty do we construct a container and inject the scope
			if ( !tests.isEmpty() ) {
				testInstance.injectExecutionContext( context );
				dynamicNodes.add( dynamicContainer( context.getTestContainerName( testClass ), tests ) );
			}
		}

		return dynamicNodes;
	}

	protected void injectExecutionContext(T context) {

	}

	@SuppressWarnings("unchecked")
	protected Collection<T> getExecutionContexts() {
		return Collections.singletonList( (T) new DynamicExecutionContext() {} );
	}
}
