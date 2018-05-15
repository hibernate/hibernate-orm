/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.internal.util.StringHelper;

import org.hibernate.testing.junit5.dynamictests.AbstractDynamicTest;
import org.hibernate.testing.junit5.envers.Strategy;

/**
 * An abstract base test case class regardless of whether test is to be bootstrapped as
 * a native Hibernate session factory or JPA entity manager factory.
 *
 * @author Chris Cranford
 */
public class AbstractEnversDynamicTest extends AbstractDynamicTest<EnversDynamicExecutionContext> {
	private static final Class<?>[] NO_CLASSES = new Class<?>[0];
	private static final String[] NO_MAPPINGS = new String[0];

	protected String auditStrategyName;

	@Override
	protected Collection<EnversDynamicExecutionContext> getExecutionContexts() {
		List<EnversDynamicExecutionContext> contexts = new ArrayList<>();
		for ( Strategy strategy : Strategy.values() ) {
			contexts.add( new EnversDynamicExecutionContext( strategy ) );
		}
		return contexts;
	}

	/**
	 * Return a list of annotated classes that should be added to the metadata configuration.
	 */
	protected Class<?>[] getAnnotatedClasses() {
		return NO_CLASSES;
	}

	/**
	 * Return a list of HBM mappings that should be added to the metadata configuration.
	 */
	protected String[] getMappings() {
		return NO_MAPPINGS;
	}

	/**
	 * Return the base package path for HBM mappings.  The default is {@code 'org/hibernate/test'}.
	 */
	protected String getBaseForMappings() {
		return "org/hibernate/envers/test/";
	}

	/**
	 * Returns whether the schema should be created or not.
	 */
	protected boolean exportSchema() {
		return true;
	}

	/**
	 * Add additional configuration settings to configure the test.
	 *
	 * @param settings A map of string-based configuration parameters.
	 */
	protected void addSettings(Map<String, Object> settings) {
		settings.put( EnversSettings.USE_REVISION_ENTITY_WITH_NATIVE_ID, Boolean.FALSE.toString() );
		if ( !StringHelper.isEmpty( auditStrategyName ) ) {
			settings.put( EnversSettings.AUDIT_STRATEGY, auditStrategyName );
		}
	}

}
