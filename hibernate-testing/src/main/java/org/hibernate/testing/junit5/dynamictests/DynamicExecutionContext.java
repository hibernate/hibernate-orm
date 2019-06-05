/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.junit5.dynamictests;

import java.lang.reflect.Method;

/**
 * Simple contract for a sepcific execution group of dynamic test nodes that allows
 * the {@link org.junit.jupiter.api.TestFactory} to check whether given test nodes
 * should be generated or not.
 *
 * @author Chris Cranford
 */
public interface DynamicExecutionContext {
	/**
	 * Allows applying filter criteria against the test class.
	 *
	 * @param testClass The test class.
	 * @return boolean true if the test class should generate nodes, false otherwise.
	 */
	default boolean isExecutionAllowed(Class<? extends AbstractDynamicTest> testClass) {
		return true;
	}

	/**
	 * Allows applying filter criteria against the dynamic test method.
	 *
	 * @param method The test method.
	 * @return boolean true if the test method should generate a node, false otherwise.
	 */
	default boolean isExecutionAllowed(Method method) {
		return true;
	}

	/**
	 * Return the name of the dynamic node container associated with this execution context.
	 *
	 * @param testClass The test class.
	 * @return The name of the dynamic node container.
	 */
	default String getTestContainerName(Class<? extends AbstractDynamicTest> testClass) {
		return testClass.getName();
	}
}
