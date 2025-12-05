/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.junit4;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;

import org.hibernate.testing.DialectCheck;
import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.RequiresDialects;
import org.hibernate.testing.Skip;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.SkipForDialects;
import org.hibernate.testing.orm.junit.DialectContext;
import org.hibernate.testing.orm.junit.DialectFeatureCheck;
import org.hibernate.testing.orm.junit.DialectFilterExtension;
import org.hibernate.testing.orm.junit.RequiresDialectFeatureGroup;
import org.hibernate.testing.orm.junit.SkipForDialectGroup;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import org.jboss.logging.Logger;

import static org.hibernate.dialect.SimpleDatabaseVersion.ZERO_VERSION;

/**
 * The Hibernate-specific {@link org.junit.runner.Runner} implementation which layers {@link ExtendedFrameworkMethod}
 * support on top of the standard JUnit {@link FrameworkMethod} for extra information after checking to make sure the
 * test should be run.
 *
 * @author Steve Ebersole
 */
public class CustomRunner extends BlockJUnit4ClassRunner {
	private static final Logger log = Logger.getLogger( CustomRunner.class );

	private TestClassMetadata testClassMetadata;

	public CustomRunner(Class<?> clazz) throws InitializationError, NoTestsRemainException {
		super( clazz );
	}

	@Override
	protected void collectInitializationErrors(List<Throwable> errors) {
		super.collectInitializationErrors( errors );
		this.testClassMetadata = new TestClassMetadata( getTestClass().getJavaClass() );
		testClassMetadata.validate( errors );
	}

	public TestClassMetadata getTestClassMetadata() {
		return testClassMetadata;
	}

	private Boolean isAllTestsIgnored;

	protected boolean isAllTestsIgnored() {
		if ( isAllTestsIgnored == null ) {
			if ( computeTestMethods().isEmpty() ) {
				isAllTestsIgnored = true;
			}
			else {
				isAllTestsIgnored = true;
				for ( FrameworkMethod method : computeTestMethods() ) {
					Ignore ignore = method.getAnnotation( Ignore.class );
					if ( ignore == null ) {
						isAllTestsIgnored = false;
						break;
					}
				}
			}
		}
		return isAllTestsIgnored;
	}

	@Override
	protected Statement withBeforeClasses(Statement statement) {
		if ( isAllTestsIgnored() ) {
			return super.withBeforeClasses( statement );
		}
		return new BeforeClassCallbackHandler(
				this,
				super.withBeforeClasses( statement )
		);
	}

	@Override
	protected Statement withAfterClasses(Statement statement) {
		if ( isAllTestsIgnored() ) {
			return super.withAfterClasses( statement );
		}
		return new AfterClassCallbackHandler(
				this,
				super.withAfterClasses( statement )
		);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.junit.runners.ParentRunner#classBlock(RunNotifier)
	 */
	@Override
	protected Statement classBlock(RunNotifier notifier) {
		log.info( BeforeClass.class.getSimpleName() + ": " + getName() );

		return super.classBlock( notifier );
	}

	@Override
	protected Statement methodBlock(FrameworkMethod method) {
		log.info( Test.class.getSimpleName() + ": " + method.getName() );

		final Statement originalMethodBlock = super.methodBlock( method );
		final ExtendedFrameworkMethod extendedFrameworkMethod = (ExtendedFrameworkMethod) method;
		return new FailureExpectedHandler(
				originalMethodBlock,
				testClassMetadata,
				extendedFrameworkMethod,
				testInstance
		);
	}

	protected Object testInstance;

	protected Object getTestInstance() throws Exception {
		if ( testInstance == null ) {
			testInstance = super.createTest();
		}
		return testInstance;
	}

	@Override
	protected Object createTest() throws Exception {
		return getTestInstance();
	}

	private List<FrameworkMethod> computedTestMethods;

	@Override
	protected List<FrameworkMethod> computeTestMethods() {
		if ( computedTestMethods == null ) {
			computedTestMethods = doComputation();
			sortMethods( computedTestMethods );
		}
		return computedTestMethods;
	}

	protected void sortMethods(List<FrameworkMethod> computedTestMethods) {
		if ( CollectionHelper.isEmpty( computedTestMethods ) ) {
			return;
		}
		Collections.sort(
				computedTestMethods, new Comparator<FrameworkMethod>() {
					@Override
					public int compare(FrameworkMethod o1, FrameworkMethod o2) {
						return o1.getName().compareTo( o2.getName() );
					}
				}
		);
	}

	protected List<FrameworkMethod> doComputation() {
		// Next, get all the test methods as understood by JUnit
		final List<FrameworkMethod> methods = super.computeTestMethods();

		// Now process that full list of test methods and build our custom result
		final List<FrameworkMethod> result = new ArrayList<FrameworkMethod>();
		final boolean doValidation = Boolean.getBoolean( Helper.VALIDATE_FAILURE_EXPECTED );
		int testCount = 0;

		Ignore virtualIgnore;

		for ( FrameworkMethod frameworkMethod : methods ) {
			// potentially ignore based on expected failure
			final FailureExpected failureExpected = Helper.locateAnnotation(
					FailureExpected.class,
					frameworkMethod,
					getTestClass()
			);
			if ( failureExpected != null && !doValidation ) {
				virtualIgnore = new IgnoreImpl( Helper.extractIgnoreMessage( failureExpected, frameworkMethod ) );
			}
			else {
				virtualIgnore = convertSkipToIgnore( frameworkMethod );
			}

			testCount++;
			log.trace( "adding test " + Helper.extractTestName( frameworkMethod ) + " [#" + testCount + "]" );
			result.add( new ExtendedFrameworkMethod( frameworkMethod, virtualIgnore, failureExpected ) );
		}
		return result;
	}

	@SuppressWarnings({"ClassExplicitlyAnnotation"})
	public static class IgnoreImpl implements Ignore {
		private final String value;

		public IgnoreImpl(String value) {
			this.value = value;
		}

		@Override
		public String value() {
			return value;
		}

		@Override
		public Class<? extends Annotation> annotationType() {
			return Ignore.class;
		}
	}

	private static Dialect dialect = determineDialect();

	private static Dialect determineDialect() {
		try {
			return DialectContext.getDialect();
		}
		catch (Exception e) {
			return new Dialect( ZERO_VERSION ) {};
		}
	}

	protected Ignore convertSkipToIgnore(FrameworkMethod frameworkMethod) {
		// @Skip
		Skip skip = Helper.locateAnnotation( Skip.class, frameworkMethod, getTestClass() );
		if ( skip != null ) {
			if ( isMatch( skip.condition() ) ) {
				return buildIgnore( skip );
			}
		}

		// @SkipForDialects & @SkipForDialect
		for ( SkipForDialect skipForDialectAnn : Helper.collectAnnotations(
				SkipForDialect.class,
				SkipForDialects.class,
				(methodAnnotation, methodAnnotations, classAnnotation, classAnnotations) -> {
					final LinkedHashMap<Class<?>, SkipForDialect> map = new LinkedHashMap<>();
					if ( classAnnotation != null ) {
						map.put( classAnnotation.value(), classAnnotation );
					}
					if ( classAnnotations != null ) {
						for ( SkipForDialect annotation : classAnnotations ) {
							map.put( annotation.value(), annotation );
						}
					}
					if ( methodAnnotation != null ) {
						map.put( methodAnnotation.value(), methodAnnotation );
					}
					if ( methodAnnotations != null ) {
						for ( SkipForDialect annotation : methodAnnotations ) {
							map.put( annotation.value(), annotation );
						}
					}
					return map.values();
				},
				frameworkMethod,
				getTestClass()
		) ) {
			if ( skipForDialectAnn.strictMatching() ) {
				if ( skipForDialectAnn.value().equals( dialect.getClass() ) ) {
					return buildIgnore( skipForDialectAnn );
				}
			}
			else {
				if ( skipForDialectAnn.value().isInstance( dialect ) ) {
					return buildIgnore( skipForDialectAnn );
				}
			}
		}

		for ( org.hibernate.testing.orm.junit.SkipForDialect effectiveSkipForDialect : Helper.collectAnnotations(
				org.hibernate.testing.orm.junit.SkipForDialect.class,
				SkipForDialectGroup.class,
				(methodAnnotation, methodAnnotations, classAnnotation, classAnnotations) -> {
					final LinkedHashMap<Class<?>, org.hibernate.testing.orm.junit.SkipForDialect> map = new LinkedHashMap<>();
					if ( classAnnotation != null ) {
						map.put( classAnnotation.dialectClass(), classAnnotation );
					}
					if ( classAnnotations != null ) {
						for ( org.hibernate.testing.orm.junit.SkipForDialect annotation : classAnnotations ) {
							map.put( annotation.dialectClass(), annotation );
						}
					}
					if ( methodAnnotation != null ) {
						map.put( methodAnnotation.dialectClass(), methodAnnotation );
					}
					if ( methodAnnotations != null ) {
						for ( org.hibernate.testing.orm.junit.SkipForDialect annotation : methodAnnotations ) {
							map.put( annotation.dialectClass(), annotation );
						}
					}
					return map.values();
				},
				frameworkMethod,
				getTestClass()
		) ) {
			final boolean versionsMatch;
			final int matchingMajorVersion = effectiveSkipForDialect.majorVersion();

			if ( matchingMajorVersion >= 0 ) {
				versionsMatch = DialectFilterExtension.versionsMatch(
						matchingMajorVersion,
						effectiveSkipForDialect.minorVersion(),
						effectiveSkipForDialect.microVersion(),
						dialect,
						effectiveSkipForDialect.versionMatchMode()
				);

				if ( versionsMatch ) {
					if ( effectiveSkipForDialect.matchSubTypes() ) {
						if ( effectiveSkipForDialect.dialectClass().isInstance( dialect ) ) {
							return buildIgnore( effectiveSkipForDialect );
						}
					}
					else {
						if ( effectiveSkipForDialect.dialectClass().equals( dialect.getClass() ) ) {
							return buildIgnore( effectiveSkipForDialect );
						}
					}
				}
			}
			else {
				if ( effectiveSkipForDialect.matchSubTypes() ) {
					if ( effectiveSkipForDialect.dialectClass().isInstance( dialect ) ) {
						return buildIgnore( effectiveSkipForDialect );
					}
				}
				else {
					if ( effectiveSkipForDialect.dialectClass().equals( dialect.getClass() ) ) {
						return buildIgnore( effectiveSkipForDialect );
					}
				}
			}
		}


		// @RequiresDialects & @RequiresDialect
		final Collection<RequiresDialect> requiresDialects = Helper.collectAnnotations(
				RequiresDialect.class,
				RequiresDialects.class,
				(methodAnnotation, methodAnnotations, classAnnotation, classAnnotations) -> {
					final LinkedHashMap<Class<?>, RequiresDialect> map = new LinkedHashMap<>();
					if ( classAnnotation != null ) {
						map.put( classAnnotation.value(), classAnnotation );
					}
					if ( classAnnotations != null ) {
						for ( RequiresDialect annotation : classAnnotations ) {
							map.put( annotation.value(), annotation );
						}
					}
					if ( methodAnnotation != null ) {
						map.put( methodAnnotation.value(), methodAnnotation );
					}
					if ( methodAnnotations != null ) {
						for ( RequiresDialect annotation : methodAnnotations ) {
							map.put( annotation.value(), annotation );
						}
					}
					return map.values();
				},
				frameworkMethod,
				getTestClass()
		);

		if ( !requiresDialects.isEmpty() && !isDialectMatchingRequired( requiresDialects ) ) {
			return buildIgnore( requiresDialects );
		}

		final Collection<org.hibernate.testing.orm.junit.RequiresDialect> effectiveRequiresDialects = Helper.collectAnnotations(
				org.hibernate.testing.orm.junit.RequiresDialect.class,
				org.hibernate.testing.orm.junit.RequiresDialects.class,
				(methodAnnotation, methodAnnotations, classAnnotation, classAnnotations) -> {
					final LinkedHashMap<Class<?>, org.hibernate.testing.orm.junit.RequiresDialect> map = new LinkedHashMap<>();
					if ( classAnnotation != null ) {
						map.put( classAnnotation.value(), classAnnotation );
					}
					if ( classAnnotations != null ) {
						for ( org.hibernate.testing.orm.junit.RequiresDialect annotation : classAnnotations ) {
							map.put( annotation.value(), annotation );
						}
					}
					if ( methodAnnotation != null ) {
						map.put( methodAnnotation.value(), methodAnnotation );
					}
					if ( methodAnnotations != null ) {
						for ( org.hibernate.testing.orm.junit.RequiresDialect annotation : methodAnnotations ) {
							map.put( annotation.value(), annotation );
						}
					}
					return map.values();
				},
				frameworkMethod,
				getTestClass()
		);

		if ( !effectiveRequiresDialects.isEmpty() && !isDialectMatchingRequired2( effectiveRequiresDialects ) ) {
			return buildIgnore2( effectiveRequiresDialects );
		}

		// @RequiresDialectFeature
		final List<RequiresDialectFeature> requiresDialectFeatures = Helper.locateAllAnnotations(
				RequiresDialectFeature.class,
				frameworkMethod,
				getTestClass()
		);

		if ( !requiresDialectFeatures.isEmpty() ) {
			for ( RequiresDialectFeature requiresDialectFeature : requiresDialectFeatures ) {
				try {
					for ( Class<? extends DialectCheck> checkClass : requiresDialectFeature.value() ) {
						if ( !checkClass.newInstance().isMatch( dialect ) ) {
							return buildIgnore( requiresDialectFeature );
						}
					}
				}
				catch (RuntimeException e) {
					throw e;
				}
				catch (Exception e) {
					throw new RuntimeException( "Unable to instantiate DialectCheck", e );
				}
			}
		}

		Collection<org.hibernate.testing.orm.junit.RequiresDialectFeature> effectiveRequiresDialectFeatures = Helper.collectAnnotations(
				org.hibernate.testing.orm.junit.RequiresDialectFeature.class,
				RequiresDialectFeatureGroup.class,
				frameworkMethod,
				getTestClass()
		);

		for ( org.hibernate.testing.orm.junit.RequiresDialectFeature effectiveRequiresDialectFeature : effectiveRequiresDialectFeatures ) {
			try {
				final Class<? extends DialectFeatureCheck> featureClass = effectiveRequiresDialectFeature.feature();
				final DialectFeatureCheck featureCheck = featureClass.getConstructor().newInstance();
				boolean testResult = featureCheck.apply( dialect );
				if ( effectiveRequiresDialectFeature.reverse() ) {
					testResult = !testResult;
				}
				if ( !testResult ) {
					return buildIgnore( effectiveRequiresDialectFeature );
				}
			}
			catch (ReflectiveOperationException e) {
				throw new RuntimeException( "Unable to instantiate DialectFeatureCheck class", e );
			}
		}

		return null;
	}

	private boolean isDialectMatchingRequired(Collection<RequiresDialect> requiresDialects) {
		boolean foundMatch = false;
		for ( RequiresDialect requiresDialectAnn : requiresDialects ) {
			foundMatch = requiresDialectAnn.strictMatching()
					? requiresDialectAnn.value().equals( dialect.getClass() )
					: requiresDialectAnn.value().isInstance( dialect );
			if ( foundMatch ) {
				break;
			}
			if ( foundMatch ) {
				break;
			}
		}
		return foundMatch;
	}

	private boolean isDialectMatchingRequired2(Collection<org.hibernate.testing.orm.junit.RequiresDialect> effectiveRequiresDialects) {
		for ( org.hibernate.testing.orm.junit.RequiresDialect requiresDialect : effectiveRequiresDialects ) {
			final boolean versionsMatch;
			final int matchingMajorVersion = requiresDialect.majorVersion();

			if ( matchingMajorVersion >= 0 ) {
				final int matchingMinorVersion = requiresDialect.minorVersion();
				final int matchingMicroVersion = requiresDialect.microVersion();

				versionsMatch = DialectFilterExtension.versionsMatch(
						matchingMajorVersion,
						matchingMinorVersion,
						matchingMicroVersion,
						dialect,
						requiresDialect.versionMatchMode()
				);
			}
			else {
				versionsMatch = true;
			}


			if ( ! requiresDialect.value().isInstance( dialect ) ) {
				continue;
			}

			if ( ! versionsMatch ) {
				continue;
			}

			if ( requiresDialect.matchSubTypes() || requiresDialect.value().equals( dialect.getClass() ) ) {
				return true;
			}
		}
		return false;
	}

	private Ignore buildIgnore(Skip skip) {
		return new IgnoreImpl( "@Skip : " + skip.message() );
	}

	private Ignore buildIgnore(SkipForDialect skip) {
		return buildIgnore( "@SkipForDialect match", skip.comment(), skip.jiraKey() );
	}

	private Ignore buildIgnore(org.hibernate.testing.orm.junit.SkipForDialect skip) {
		return buildIgnore( "@SkipForDialect match", skip.reason(), null );
	}

	private Ignore buildIgnore(String reason, String comment, String jiraKey) {
		return new IgnoreImpl( getIgnoreMessage( reason, comment, jiraKey ) );
	}

	private String getIgnoreMessage(String reason, String comment, String jiraKey) {
		StringBuilder buffer = new StringBuilder( reason );
		if ( StringHelper.isNotEmpty( comment ) ) {
			buffer.append( "; " ).append( comment );
		}

		if ( StringHelper.isNotEmpty( jiraKey ) ) {
			buffer.append( " (" ).append( jiraKey ).append( ')' );
		}

		return buffer.toString();
	}

	private Ignore buildIgnore(RequiresDialect requiresDialect) {
		return buildIgnore( "@RequiresDialect non-match", requiresDialect.comment(), requiresDialect.jiraKey() );
	}

	private Ignore buildIgnore(Collection<RequiresDialect> requiresDialects) {
		String ignoreMessage = "";
		for ( RequiresDialect requiresDialect : requiresDialects ) {
			ignoreMessage += getIgnoreMessage(
					"@RequiresDialect non-match",
					requiresDialect.comment(),
					requiresDialect.jiraKey()
			);
			ignoreMessage += System.lineSeparator();
		}
		return new IgnoreImpl( ignoreMessage );
	}

	private Ignore buildIgnore2(Collection<org.hibernate.testing.orm.junit.RequiresDialect> requiresDialects) {
		String ignoreMessage = "";
		for ( org.hibernate.testing.orm.junit.RequiresDialect requiresDialect : requiresDialects ) {
			ignoreMessage += getIgnoreMessage(
					"@RequiresDialect non-match",
					requiresDialect.comment(),
					null
			);
			ignoreMessage += System.lineSeparator();
		}
		return new IgnoreImpl( ignoreMessage );
	}

	private Ignore buildIgnore(RequiresDialectFeature requiresDialectFeature) {
		return buildIgnore(
				"@RequiresDialectFeature non-match",
				requiresDialectFeature.comment(),
				requiresDialectFeature.jiraKey()
		);
	}

	private Ignore buildIgnore(org.hibernate.testing.orm.junit.RequiresDialectFeature requiresDialectFeature) {
		return buildIgnore(
				String.format( Locale.ROOT, "Failed @RequiresDialectFeature [%s]", requiresDialectFeature.feature() ),
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
