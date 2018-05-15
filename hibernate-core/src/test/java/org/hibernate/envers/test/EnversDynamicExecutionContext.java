/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test;

import java.lang.reflect.Method;

import org.hibernate.dialect.Dialect;
import org.hibernate.envers.strategy.AuditStrategy;

import org.hibernate.testing.junit5.SkipForDialect;
import org.hibernate.testing.junit5.dynamictests.AbstractDynamicTest;
import org.hibernate.testing.junit5.dynamictests.DynamicExecutionContext;
import org.hibernate.testing.junit5.envers.RequiresAuditStrategy;
import org.hibernate.testing.junit5.envers.Strategy;

/**
 * @author Chris Cranford
 */
public class EnversDynamicExecutionContext implements DynamicExecutionContext {
	private final Strategy strategy;

	EnversDynamicExecutionContext(Strategy strategy) {
		this.strategy = strategy;
	}

	public Strategy getStrategy() {
		return strategy;
	}

	@Override
	public boolean isExecutionAllowed(Class<? extends AbstractDynamicTest> testClass) {
		if ( !isAuditStrategy( testClass.getAnnotation( RequiresAuditStrategy.class ) ) ) {
			return false;
		}
		if ( isDialectMatch( testClass.getAnnotationsByType( SkipForDialect.class ) ) ) {
			return false;
		}
		return true;
	}

	@Override
	public boolean isExecutionAllowed(Method method) {
		if ( !isAuditStrategy( method.getAnnotation( RequiresAuditStrategy.class ) ) ) {
			return false;
		}
		if ( isDialectMatch( method.getAnnotationsByType( SkipForDialect.class ) ) ) {
			return false;
		}
		return true;
	}

	@Override
	public String getTestContainerName(Class<? extends AbstractDynamicTest> testClass) {
		return testClass.getName() + " (" + strategy.name() + ")";
	}

	/**
	 * Checks whether or not this execution context matches the specified {@link AuditStrategy}.
	 *
	 * @param annotation The {@link RequiresAuditStrategy} annotation.
	 * @return boolean true if the audit strategy matches; false otherwise.
	 */
	private boolean isAuditStrategy(RequiresAuditStrategy annotation) {
		if ( annotation == null ) {
			// if the annotation isn't present, allow it.
			return true;
		}

		for ( Class<? extends AuditStrategy> strategyClass : annotation.value() ) {
			if ( strategy.isStrategy( strategyClass ) ) {
				return true;
			}
		}

		return false;
	}

	private boolean isDialectMatch(SkipForDialect[] skipForDialectAnnotations) {
		for ( SkipForDialect skipForDialect : skipForDialectAnnotations ) {
			if ( skipForDialect.matchSubTypes() ) {
				if ( Dialect.getDialect().getClass().isAssignableFrom( skipForDialect.dialectClass() ) ) {
					return true;
				}
			}
			else {
				if ( Dialect.getDialect().getClass().equals( skipForDialect.dialectClass() ) ) {
					return true;
				}
			}
		}
		return false;
	}
}
