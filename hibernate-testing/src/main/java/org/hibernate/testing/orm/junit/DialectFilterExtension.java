/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.junit;

import java.util.List;
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

		final List<RequiresDialect> effectiveRequiresDialects = TestingUtil.findEffectiveRepeatingAnnotation(
				context,
				RequiresDialect.class,
				RequiresDialects.class
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
					return ConditionEvaluationResult.enabled( "Matched @RequiresDialect" );
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

		final List<SkipForDialect> effectiveSkips = TestingUtil.findEffectiveRepeatingAnnotation(
				context,
				SkipForDialect.class,
				SkipForDialectGroup.class
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

		List<RequiresDialectFeature> effectiveRequiresDialectFeatures = TestingUtil.findEffectiveRepeatingAnnotation(
				context,
				RequiresDialectFeature.class,
				RequiresDialectFeatureGroup.class
		);

		for ( RequiresDialectFeature effectiveRequiresDialectFeature : effectiveRequiresDialectFeatures ) {
			try {
				final DialectFeatureCheck dialectFeatureCheck = effectiveRequiresDialectFeature.feature()
						.newInstance();
				if ( !dialectFeatureCheck.apply( dialect ) ) {
					return ConditionEvaluationResult.disabled(
							String.format(
									Locale.ROOT,
									"Failed @RequiresDialectFeature [%s]",
									effectiveRequiresDialectFeature.feature()
							) );
				}
			}
			catch (InstantiationException | IllegalAccessException e) {
				throw new RuntimeException( "Unable to instantiate DialectFeatureCheck class", e );
			}
		}

		return ConditionEvaluationResult.enabled( "Passed all @SkipForDialects" );
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

	private boolean versionsMatch(
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
			return dialect.getVersion().isSince( matchingMajorVersion, matchingMinorVersion, matchingMicroVersion );
		}
		else {
			return dialect.getVersion().isSame( matchingMajorVersion );
		}
	}

	private Dialect getDialect(ExtensionContext context) {
		return DialectContext.getDialect();
	}
}
