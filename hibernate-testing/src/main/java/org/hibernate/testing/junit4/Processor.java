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

import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.SkipLog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A custom JUnit {@link MethodRule} which essentially acts as "around advice" for test method execution.  Works
 * in conjunction with the information collected as part of {@link CustomRunner}.
 *
 * @author Steve Ebersole
 * @see CustomRunner
 */
public class Processor implements MethodRule {
	private static final Logger log = LoggerFactory.getLogger( Processor.class );

	public Statement apply(final Statement statement, final FrameworkMethod frameworkMethod, final Object target) {
        log.trace( "Preparing to start test {}", Helper.extractTestName( frameworkMethod ) );
		if ( ! ExtendedFrameworkMethod.class.isInstance( frameworkMethod ) ) {
			throw new IllegalStateException(
                    "Use of " + getClass().getName() + " only supported in combination with use of "
                            + CustomRunner.class.getName()
            );
		}

        final ExtendedFrameworkMethod extendedFrameworkMethod = (ExtendedFrameworkMethod) frameworkMethod;

		final SkipMarker skipMarker = extendedFrameworkMethod.getSkipMarker();
		if ( skipMarker != null ) {
			SkipLog.reportSkip( skipMarker );
			return new Statement() {
				@Override
				public void evaluate() throws Throwable {
				}
			};
		}

        final FailureExpected failureExpected = extendedFrameworkMethod.getFailureExpectedAnnotation();

		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				if ( extendedFrameworkMethod.isFirstInTestClass() ) {
                    extendedFrameworkMethod.getCallbackMetadata().performBeforeClassCallbacks( target );
				}
				try {
					statement.evaluate();
                    // reaching here is expected, unless the test is marked as an expected failure
                    if ( failureExpected != null ) {
                        throw new FailureExpectedTestPassedException( extendedFrameworkMethod );
                    }
				}
                catch ( FailureExpectedTestPassedException e ) {
                    // just pass this along
                    throw e;
                }
				catch ( Throwable e ) {
                    // on error handling is very different based on whether the test was marked as an expected failure
                    if ( failureExpected != null ) {
                        // handle the expected failure case
                        log.info(
                                "Ignoring expected failure [{}] : {}",
                                Helper.extractTestName( frameworkMethod ),
                                Helper.extractMessage( failureExpected )
                        );
                        extendedFrameworkMethod.getCallbackMetadata().performOnExpectedFailureCallback( target );
                        // most importantly, do not propagate exception...
                    }
                    else {
                        // handle the non-expected failure case
                        extendedFrameworkMethod.getCallbackMetadata().performOnFailureCallback( target );
                        throw e;
                    }
				}
				finally {
					if ( extendedFrameworkMethod.isLastInTestClass() ) {
                        extendedFrameworkMethod.getCallbackMetadata().performAfterClassCallbacks( target );
					}
				}
			}
		};
	}

	public static class FailureExpectedTestPassedException extends Exception {
		public FailureExpectedTestPassedException(FrameworkMethod frameworkMethod) {
			super( "Test marked as FailureExpected, but did not fail : " + Helper.extractTestName( frameworkMethod ) );
		}
	}
}
