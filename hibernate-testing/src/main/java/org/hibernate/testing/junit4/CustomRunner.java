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

import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.Skip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * The Hibernate-specific {@link org.junit.runner.Runner} implementation which layers {@link ExtendedFrameworkMethod}
 * support on top of the standard JUnit {@link FrameworkMethod} for extra information after checking to make sure the
 * test should be run.
 *
 * @author Steve Ebersole
 * @see Processor
 */
public class CustomRunner extends BlockJUnit4ClassRunner {
	private static final Logger log = LoggerFactory.getLogger( CustomRunner.class );

	private List<FrameworkMethod> computedTestMethods;

	public CustomRunner(Class<?> clazz) throws InitializationError, NoTestsRemainException {
		super( clazz );
	}

	public List<FrameworkMethod> getComputedTestMethods() {
		return computedTestMethods;
	}

	public int getNumberOfComputedTestMethods() {
		return getComputedTestMethods().size();
	}

	@Override
	protected List<FrameworkMethod> computeTestMethods() {
		if ( computedTestMethods == null ) {
			computedTestMethods = doComputation();
		}
		return computedTestMethods;
	}

	protected List<FrameworkMethod> doComputation() {
        // First, build the callback metadata for the test class...
        TestClassCallbackMetadata callbackMetadata = new TestClassCallbackMetadata( getTestClass().getJavaClass() );

        // Next, get all the test methods as understood by JUnit
        final List<FrameworkMethod> methods = super.computeTestMethods();

        // Now process that full list of test methods and build our custom result
        final List<FrameworkMethod> result = new ArrayList<FrameworkMethod>();
		final boolean doValidation = Boolean.getBoolean( Helper.VALIDATE_FAILURE_EXPECTED );
		int testCount = 0;

		for ( FrameworkMethod frameworkMethod : methods ) {
			// potentially ignore based on expected failure
            final FailureExpected failureExpected = Helper.locateFailureExpectedAnnotation( frameworkMethod );
			if ( failureExpected != null && !doValidation ) {
				log.info( Helper.extractIgnoreMessage( failureExpected, frameworkMethod ) );
				continue;
			}

			// next, check specific exclusions
			final Skip skip = Helper.locateSkipAnnotation( frameworkMethod );
			if ( skip != null ) {
				if ( isMatch( skip.condition() ) ) {
					log.info( Helper.extractIgnoreMessage( skip, frameworkMethod ) );
					continue;
				}
			}

			testCount++;
			log.trace( "adding test " + Helper.extractTestName( frameworkMethod ) + " [#" + testCount + "]" );
			result.add( new ExtendedFrameworkMethod( frameworkMethod, failureExpected, callbackMetadata, this ) );
		}
		return result;
	}

	private boolean isMatch(Class<? extends Skip.Matcher> condition) {
		try {
			Skip.Matcher matcher = condition.newInstance();
			return matcher.isMatch();
		}
		catch (Exception e) {
			throw new MatcherInstantiationException( condition, e );
		}
	}

	private static class MatcherInstantiationException extends RuntimeException {
		private MatcherInstantiationException(Class<? extends Skip.Matcher> matcherClass, Throwable cause) {
			super( "Unable to instantiate specified Matcher [" + matcherClass.getName(), cause );
		}
	}
}
