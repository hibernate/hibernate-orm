/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.hibernate.testing.junit5.AnnotationUtil;
import org.hibernate.testing.junit5.DialectFeatureCheck;
import org.hibernate.testing.junit5.RequiresDialect;
import org.hibernate.testing.junit5.RequiresDialectFeature;
import org.hibernate.testing.junit5.RequiresDialectFeatureGroup;
import org.hibernate.testing.junit5.RequiresDialects;
import org.hibernate.testing.junit5.SkipForDialect;
import org.hibernate.testing.junit5.SkipForDialectGroup;
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

		final List<RequiresDialect> effectiveRequiresDialects = AnnotationUtil.findEffectiveRepeatingAnnotation(
				context,
				RequiresDialect.class,
				RequiresDialects.class
		);

		if ( !effectiveRequiresDialects.isEmpty() ) {
			StringBuilder requiredDialects = new StringBuilder(  );
			for ( RequiresDialect requiresDialect : effectiveRequiresDialects ) {
				requiredDialects.append(requiresDialect.value()  );
				requiredDialects.append( " " );
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

			return ConditionEvaluationResult.disabled(
					String.format(
							Locale.ROOT,
							"Failed @RequiresDialect(dialect=%s) check - found %s]",
							requiredDialects.toString(),
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
		final Optional<org.hibernate.testing.orm.SessionFactoryScope> sessionFactoryScopeWrapper = SessionFactoryExtension.findSessionFactoryScope(
				context.getRequiredTestInstance(),
				context
		);

		final org.hibernate.testing.orm.SessionFactoryScope sessionFactoryScope = sessionFactoryScopeWrapper.orElseThrow(
				() -> new IllegalStateException( "Could not locate SessionFactoryScope in ExtensionContext" )
		);

		final SessionFactoryImplementor sessionFactory = sessionFactoryScope.getSessionFactory();
		return sessionFactory.getJdbcServices().getJdbcEnvironment().getDialect();
	}
}
