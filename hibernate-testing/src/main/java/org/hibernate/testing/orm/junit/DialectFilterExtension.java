/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.orm.junit;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;

import org.hibernate.dialect.Dialect;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import org.jboss.logging.Logger;

/**
 * JUnit 5 extension used to add {@link RequiresDialect} and {@link SkipForDialect}
 * handling
 *
 * @author Steve Ebersole
 */
public class DialectFilterExtension implements ExecutionCondition {
	private static final Logger log = Logger.getLogger( DialectFilterExtension.class );

	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
		final Dialect dialect = getDialect( context );
		if ( dialect == null ) {
			throw new RuntimeException( "#getDialect returned null" );
		}

		log.debugf( "Checking Dialect [%s] - context = %s", dialect, context.getDisplayName() );

		final Collection<RequiresDialect> effectiveRequiresDialects = TestingUtil.collectAnnotations(
				context,
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
				}
		);

		if ( !effectiveRequiresDialects.isEmpty() ) {
			StringBuilder requiredDialects = new StringBuilder(  );

			for ( RequiresDialect requiresDialect : effectiveRequiresDialects ) {
				requiredDialects.append( requiresDialect.value()  );
				requiredDialects.append( " " );

				final boolean versionsMatch;
				final int matchingMajorVersion = requiresDialect.majorVersion();

				if ( matchingMajorVersion >= 0 ) {
					final int matchingMinorVersion = requiresDialect.minorVersion();
					final int matchingMicroVersion = requiresDialect.microVersion();

					requiredDialects.append( ", version = " );
					requiredDialects.append( matchingVersionString( matchingMajorVersion, matchingMinorVersion, matchingMicroVersion ) );
					requiredDialects.append( " " );

					versionsMatch = versionsMatch(
							matchingMajorVersion,
							matchingMinorVersion,
							matchingMicroVersion,
							dialect,
							requiresDialect.matchSubTypes()
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
					return evaluateSkipConditions( context, dialect, "Matched @RequiresDialect" );
				}
			}

			return ConditionEvaluationResult.disabled(
					String.format(
							Locale.ROOT,
							"Failed @RequiresDialect(dialect=%s) check - found %s version %s]",
							requiredDialects,
							dialect.getClass().getName(),
							dialect.getVersion()
					)
			);
		}

		return evaluateSkipConditions( context, dialect, "Passed all @SkipForDialects" );
	}

	private String matchingVersionString(int matchingMajorVersion, int matchingMinorVersion, int matchingMicroVersion) {
		final StringBuilder buffer = new StringBuilder( matchingMajorVersion );
		if ( matchingMajorVersion > 0 ) {
			buffer.append( "." ).append( matchingMinorVersion );
			if ( matchingMicroVersion > 0 ) {
				buffer.append( "." ).append( matchingMicroVersion );
			}
		}

		return buffer.toString();
	}

	public static boolean versionsMatch(
			int matchingMajorVersion,
			int matchingMinorVersion,
			int matchingMicroVersion,
			Dialect dialect,
			boolean matchNewerVersions) {
		if ( matchingMajorVersion < 0 ) {
			return false;
		}

		if ( matchingMinorVersion < 0 ) {
			matchingMinorVersion = 0;
		}

		if ( matchingMicroVersion < 0 ) {
			matchingMicroVersion = 0;
		}

		if ( matchNewerVersions ) {
			return dialect.getVersion().isSameOrAfter( matchingMajorVersion, matchingMinorVersion, matchingMicroVersion );
		}
		else {
			return dialect.getVersion().isSame( matchingMajorVersion );
		}
	}

	private ConditionEvaluationResult evaluateSkipConditions(ExtensionContext context, Dialect dialect, String enabledResult) {
		final Collection<SkipForDialect> effectiveSkips = TestingUtil.collectAnnotations(
				context,
				SkipForDialect.class,
				SkipForDialectGroup.class,
				(methodAnnotation, methodAnnotations, classAnnotation, classAnnotations) -> {
					final LinkedHashMap<Class<?>, SkipForDialect> map = new LinkedHashMap<>();
					if ( classAnnotation != null ) {
						map.put( classAnnotation.dialectClass(), classAnnotation );
					}
					if ( classAnnotations != null ) {
						for ( SkipForDialect annotation : classAnnotations ) {
							map.put( annotation.dialectClass(), annotation );
						}
					}
					if ( methodAnnotation != null ) {
						map.put( methodAnnotation.dialectClass(), methodAnnotation );
					}
					if ( methodAnnotations != null ) {
						for ( SkipForDialect annotation : methodAnnotations ) {
							map.put( annotation.dialectClass(), annotation );
						}
					}
					return map.values();
				}
		);

		for ( SkipForDialect effectiveSkipForDialect : effectiveSkips ) {
			final boolean versionsMatch;
			final int matchingMajorVersion = effectiveSkipForDialect.majorVersion();

			if ( matchingMajorVersion >= 0 ) {
				versionsMatch = versionsMatch(
						matchingMajorVersion,
						effectiveSkipForDialect.minorVersion(),
						effectiveSkipForDialect.microVersion(),
						dialect,
						effectiveSkipForDialect.matchSubTypes()
				);

				if ( versionsMatch ) {
					if ( effectiveSkipForDialect.matchSubTypes() ) {
						if ( effectiveSkipForDialect.dialectClass().isInstance( dialect ) ) {
							return ConditionEvaluationResult.disabled( "Matched @SkipForDialect" );
						}
					}
					else {
						if ( effectiveSkipForDialect.dialectClass().equals( dialect.getClass() ) ) {
							return ConditionEvaluationResult.disabled( "Matched @SkipForDialect" );
						}
					}
				}
			}
			else {
				if ( effectiveSkipForDialect.matchSubTypes() ) {
					if ( effectiveSkipForDialect.dialectClass().isInstance( dialect ) ) {
						return ConditionEvaluationResult.disabled( "Matched @SkipForDialect" );
					}
				}
				else {
					if ( effectiveSkipForDialect.dialectClass().equals( dialect.getClass() ) ) {
						return ConditionEvaluationResult.disabled( "Matched @SkipForDialect" );
					}
				}
			}
		}

		Collection<RequiresDialectFeature> effectiveRequiresDialectFeatures = TestingUtil.collectAnnotations(
				context,
				RequiresDialectFeature.class,
				RequiresDialectFeatureGroup.class
		);

		for ( RequiresDialectFeature effectiveRequiresDialectFeature : effectiveRequiresDialectFeatures ) {
			try {
				final Class<? extends DialectFeatureCheck> featureClass = effectiveRequiresDialectFeature.feature();
				final DialectFeatureCheck featureCheck = featureClass.getConstructor().newInstance();
				boolean testResult = featureCheck.apply( dialect );
				if ( effectiveRequiresDialectFeature.reverse() ) {
					testResult = !testResult;
				}
				if ( !testResult ) {
					return ConditionEvaluationResult.disabled(
							String.format(
									Locale.ROOT,
									"Failed @RequiresDialectFeature [%s]",
									featureClass ) );
				}
			}
			catch (ReflectiveOperationException e) {
				throw new RuntimeException( "Unable to instantiate DialectFeatureCheck class", e );
			}
		}
		return ConditionEvaluationResult.enabled( enabledResult );
	}

	private Dialect getDialect(ExtensionContext context) {
		return DialectContext.getDialect();
	}
}
