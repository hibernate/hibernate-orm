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
				requiredDialects.append(requiresDialect.value()  );
				requiredDialects.append( " " );
				final int requiredVersion = requiresDialect.version();
				if ( requiredVersion > -1 ) {
					requiredDialects.append( ", version = " );
					requiredDialects.append( requiredVersion );
					requiredDialects.append( " " );
					if ( requiresDialect.value().isInstance( dialect ) ) {
						if ( requiredVersion == dialect.getVersion() ) {
							return ConditionEvaluationResult.enabled( "Matched @RequiresDialect" );
						}
						if ( requiresDialect.matchSubTypes() && dialect.getVersion() > requiredVersion ) {
							return ConditionEvaluationResult.enabled( "Matched @RequiresDialect" );
						}
					}
				}
				else {
					if ( requiresDialect.matchSubTypes() ) {
						if ( requiresDialect.value().isInstance( dialect ) ) {
							return ConditionEvaluationResult.enabled( "Matched @RequiresDialect" );
						}
					}
					else {
						if ( requiresDialect.value().equals( dialect.getClass() ) ) {
							return ConditionEvaluationResult.enabled( "Matched @RequiresDialect" );
						}
					}
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
			final int skipForVersion = effectiveSkipForDialect.version();
			if ( skipForVersion > -1 ) {
				if ( effectiveSkipForDialect.dialectClass().isInstance( dialect ) ) {
					if ( skipForVersion == dialect.getVersion() ) {
						return ConditionEvaluationResult.disabled( "Matched @SkipForDialect(group)" );
					}
					if ( effectiveSkipForDialect.matchSubTypes() && dialect.getVersion() > skipForVersion ) {
						return ConditionEvaluationResult.disabled( "Matched @SkipForDialect(group)" );
					}
				}
			}
			else {
				if ( effectiveSkipForDialect.matchSubTypes() ) {
					if ( effectiveSkipForDialect.dialectClass().isInstance( dialect ) ) {
						return ConditionEvaluationResult.disabled( "Matched @SkipForDialect(group)" );
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

	private Dialect getDialect(ExtensionContext context) {
		return DialectContext.getDialect();
	}
}
