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
		if ( !context.getTestInstance().isPresent() ) {
			assert !context.getTestMethod().isPresent();

			return ConditionEvaluationResult.enabled(
					"No test-instance was present - " +
							"likely that test was not defined with a per-class test lifecycle; " +
							"skipping Dialect checks for this context [" + context.getDisplayName() + "]"
			);
		}

		final Dialect dialect = getDialect( context );
		if ( dialect == null ) {
			throw new RuntimeException( "#getDialect returned null" );
		}

		log.debugf( "Checking Dialect [%s] - context = %s", dialect, context.getDisplayName() );

		final Optional<RequiresDialect> effectiveRequiresDialect = AnnotationUtil.findEffectiveAnnotation(
				context,
				RequiresDialect.class
		);
		if ( effectiveRequiresDialect.isPresent() ) {
			if ( effectiveRequiresDialect.get().matchSubTypes() ) {
				if ( effectiveRequiresDialect.get().dialectClass().isInstance( dialect ) ) {
					return ConditionEvaluationResult.enabled( "Matched @RequiresDialect" );
				}
			}
			else {
				if ( effectiveRequiresDialect.get().dialectClass().equals( dialect.getClass() ) ) {
					return ConditionEvaluationResult.enabled( "Matched @RequiresDialect" );
				}
			}

			return ConditionEvaluationResult.disabled(
					String.format(
							Locale.ROOT,
							"Failed @RequiresDialect(dialect=%s, matchSubTypes=%s) check - found %s]",
							effectiveRequiresDialect.get().dialectClass().getName(),
							effectiveRequiresDialect.get().matchSubTypes(),
							dialect.getClass().getName()
					)
			);
		}

		final List<SkipForDialect> effectiveSkips = AnnotationUtil.findEffectiveRepeatingAnnotation(
				context,
				SkipForDialect.class,
				SkipForDialectGroup.class
		);

		for ( SkipForDialect effectiveSkipForDialect : effectiveSkips ) {
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

		List<RequiresDialectFeature> effectiveRequiresDialectFeatures = AnnotationUtil.findEffectiveRepeatingAnnotation(
				context,
				RequiresDialectFeature.class,
				RequiresDialectFeatureGroup.class
		);

		for ( RequiresDialectFeature effectiveRequiresDialectFeature : effectiveRequiresDialectFeatures ) {
			try {
				final DialectFeatureCheck dialectFeatureCheck = effectiveRequiresDialectFeature.feature()
						.newInstance();
				if ( !dialectFeatureCheck.apply( getDialect( context ) ) ) {
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
		final Optional<SessionFactoryScope> sfScope = SessionFactoryScopeExtension.findSessionFactoryScope( context );
		if ( !sfScope.isPresent() ) {
			final Optional<EntityManagerFactoryScope> emScope = EntityManagerFactoryScopeExtension.findEntityManagerFactoryScope( context );
			if ( !emScope.isPresent() ) {
				final Optional<DialectAccess> dialectAccess = Optional.ofNullable(
						(DialectAccess) context.getStore( DialectAccess.NAMESPACE )
								.get( context.getRequiredTestInstance() ) );
				if ( !dialectAccess.isPresent() ) {
					throw new RuntimeException(
							"Could not locate any DialectAccess implementation in JUnit ExtensionContext" );
				}
				return dialectAccess.get().getDialect();
			}
			return emScope.get().getDialect();
		}

		return sfScope.get().getDialect();
	}
}
