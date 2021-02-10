/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.junit5;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.hibernate.dialect.Dialect;

import org.hibernate.testing.orm.junit.DialectFeatureCheck;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.RequiresDialectFeatureGroup;
import org.hibernate.testing.orm.junit.RequiresDialects;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.orm.junit.SkipForDialectGroup;
import org.hibernate.testing.orm.junit.TestingUtil;
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
				if ( requiresDialect.value().isInstance( dialect ) ) {
					if ( requiresDialect.matchSubTypes() ) {
						if ( dialect.getVersion() >= requiresDialect.version() ) {
							return ConditionEvaluationResult.enabled( "Matched @RequiresDialect" );
						}
					}
					else {
						if ( requiresDialect.version() == dialect.getVersion() ) {
							return ConditionEvaluationResult.enabled( "Matched @RequiresDialect" );
						}
					}
				}
			}

			return ConditionEvaluationResult.disabled(
					String.format(
							Locale.ROOT,
							"Failed @RequiresDialect(dialect=%s) check - found %s]",
							requiredDialects.toString(),
							dialect.getClass().getName()
					)
			);
		}

		final List<SkipForDialect> effectiveSkips = TestingUtil.findEffectiveRepeatingAnnotation(
				context,
				SkipForDialect.class,
				SkipForDialectGroup.class
		);

		for ( SkipForDialect effectiveSkipForDialect : effectiveSkips ) {
			int version = effectiveSkipForDialect.version();
			if ( version > -1 ) {
				if ( effectiveSkipForDialect.dialectClass().isInstance( dialect ) ) {
					if ( effectiveSkipForDialect.matchSubTypes() ) {
						if ( dialect.getVersion() <= version ) {
							return ConditionEvaluationResult.disabled( "Matched @SkipForDialect(group)" );
						}
					}
					else {
						if ( dialect.getVersion() == version ) {
							return ConditionEvaluationResult.disabled( "Matched @SkipForDialect" );
						}
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
				final boolean applicable = dialectFeatureCheck.apply( dialect );
				final boolean reverse = effectiveRequiresDialectFeature.reverse();
				if ( !( applicable ^ reverse ) ) {
					return ConditionEvaluationResult.disabled(
							String.format(
									Locale.ROOT,
									"Failed @RequiresDialectFeature [feature: %s, reverse: %s]",
									effectiveRequiresDialectFeature.feature(),
									effectiveRequiresDialectFeature.reverse()
							) );
				}
			}
			catch (InstantiationException | IllegalAccessException e) {
				throw new RuntimeException( "Unable to instantiate DialectFeatureCheck class", e );
			}
		}

		return ConditionEvaluationResult.enabled( "Passed all @RequiresDialect(s), @SkipForDialect(s) and @RequiresDialectFeature(group)" );
	}

	private Dialect getDialect(ExtensionContext context) {
		return DialectContext.getDialect();
	}
}
