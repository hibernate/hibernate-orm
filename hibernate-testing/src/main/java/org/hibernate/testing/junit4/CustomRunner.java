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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.StringHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

import org.hibernate.testing.DialectCheck;
import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.Skip;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.SkipLog;

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

	/**
	 * Override the JUnit method in order to circumvent the validation check for no matching methods
	 */
	@Override
	protected void validateInstanceMethods(List<Throwable> errors) {
		validatePublicVoidNoArgMethods(After.class, false, errors);
		validatePublicVoidNoArgMethods(Before.class, false, errors);
		validateTestMethods(errors);

		computeTestMethods();
		if ( !hadAnyTests ) {
			errors.add( new Exception( "No runnable methods" ) );
		}
	}

	boolean hadAnyTests;

	protected List<FrameworkMethod> doComputation() {
        // First, build the callback metadata for the test class...
        TestClassCallbackMetadata callbackMetadata = new TestClassCallbackMetadata( getTestClass().getJavaClass() );

        // Next, get all the test methods as understood by JUnit
        final List<FrameworkMethod> methods = super.computeTestMethods();

		hadAnyTests = methods.size() > 0;

        // Now process that full list of test methods and build our custom result
        final List<FrameworkMethod> result = new ArrayList<FrameworkMethod>();
		final boolean doValidation = Boolean.getBoolean( Helper.VALIDATE_FAILURE_EXPECTED );
		int testCount = 0;

		for ( FrameworkMethod frameworkMethod : methods ) {
			// potentially ignore based on expected failure
            final FailureExpected failureExpected = Helper.locateAnnotation( FailureExpected.class, frameworkMethod, getTestClass() );
			if ( failureExpected != null && !doValidation ) {
				log.info( Helper.extractIgnoreMessage( failureExpected, frameworkMethod ) );
				continue;
			}

			// see if the test should be run based on skip/requires annotations
			final SkipMarker skipMarker = getSkipInfoIfSkipped( frameworkMethod );
			if ( skipMarker != null ) {
				SkipLog.reportSkip( skipMarker );
				continue;
			}

			testCount++;
			log.trace( "adding test " + Helper.extractTestName( frameworkMethod ) + " [#" + testCount + "]" );
			result.add( new ExtendedFrameworkMethod( frameworkMethod, testCount, skipMarker, failureExpected, callbackMetadata, this ) );
		}
		return result;
	}

	private static Dialect dialect = Dialect.getDialect();

	protected SkipMarker getSkipInfoIfSkipped(FrameworkMethod frameworkMethod) {
		// @Skip
		Skip skip = Helper.locateAnnotation( Skip.class, frameworkMethod, getTestClass() );
		if ( skip != null ) {
			if ( isMatch( skip.condition() ) ) {
				return buildSkipMarker( skip, frameworkMethod );
			}
		}

		// @SkipForDialect
		SkipForDialect skipForDialectAnn = Helper.locateAnnotation( SkipForDialect.class, frameworkMethod, getTestClass() );
		if ( skipForDialectAnn != null ) {
			for ( Class<? extends Dialect> dialectClass : skipForDialectAnn.value() ) {
				if ( skipForDialectAnn.strictMatching() ) {
					if ( dialectClass.equals( dialect.getClass() ) ) {
						return buildSkipMarker( skipForDialectAnn, frameworkMethod );
					}
				}
				else {
					if ( dialectClass.isInstance( dialect ) ) {
						return buildSkipMarker( skipForDialectAnn, frameworkMethod );
					}
				}
			}
		}

		// @RequiresDialect
		RequiresDialect requiresDialectAnn = Helper.locateAnnotation( RequiresDialect.class, frameworkMethod, getTestClass() );
		if ( requiresDialectAnn != null ) {
			boolean foundMatch = false;
			for ( Class<? extends Dialect> dialectClass : requiresDialectAnn.value() ) {
				foundMatch = requiresDialectAnn.strictMatching()
						? dialectClass.equals( dialect.getClass() )
						: dialectClass.isInstance( dialect );
				if ( foundMatch ) {
					break;
				}
			}
			if ( !foundMatch ) {
				return buildSkipMarker( requiresDialectAnn, frameworkMethod );
			}
		}

		// @RequiresDialectFeature
		RequiresDialectFeature requiresDialectFeatureAnn = Helper.locateAnnotation( RequiresDialectFeature.class, frameworkMethod, getTestClass() );
		if ( requiresDialectFeatureAnn != null ) {
			Class<? extends DialectCheck> checkClass = requiresDialectFeatureAnn.value();
			try {
				DialectCheck check = checkClass.newInstance();
				if ( !check.isMatch( dialect ) ) {
					return buildSkipMarker( requiresDialectFeatureAnn, frameworkMethod );
				}
			}
			catch (RuntimeException e) {
				throw e;
			}
			catch (Exception e) {
				throw new RuntimeException( "Unable to instantiate DialectCheck", e );
			}
		}

		return null;
	}

	private SkipMarker buildSkipMarker(Skip skip, FrameworkMethod frameworkMethod) {
		return new SkipMarker( Helper.extractTestName( frameworkMethod ), "@Skip : " + skip.message() );
	}

	private SkipMarker buildSkipMarker(SkipForDialect skip, FrameworkMethod frameworkMethod) {
		return buildSkipMarker(
				frameworkMethod,
				"@SkipForDialect match",
				skip.comment(),
				skip.jiraKey()
		);
	}

	private SkipMarker buildSkipMarker(FrameworkMethod frameworkMethod, String reason, String comment, String jiraKey) {
		StringBuilder buffer = new StringBuilder( reason );
		if ( StringHelper.isNotEmpty( comment ) ) {
			buffer.append( "; " ).append( comment );
		}

		if ( StringHelper.isNotEmpty( jiraKey ) ) {
			buffer.append( " (" ).append( jiraKey ).append( ')' );
		}

		return new SkipMarker( Helper.extractTestName( frameworkMethod ), buffer.toString() );
	}

	private SkipMarker buildSkipMarker(RequiresDialect requiresDialect, FrameworkMethod frameworkMethod) {
		return buildSkipMarker(
				frameworkMethod,
				"@RequiresDialect non-match",
				requiresDialect.comment(),
				requiresDialect.jiraKey()
		);
	}

	private SkipMarker buildSkipMarker(RequiresDialectFeature requiresDialectFeature, FrameworkMethod frameworkMethod) {
		return buildSkipMarker(
				frameworkMethod,
				"@RequiresDialectFeature non-match",
				requiresDialectFeature.comment(),
				requiresDialectFeature.jiraKey()
		);
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
