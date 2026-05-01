/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.bytecode.enhancement.extension;


import static org.junit.platform.commons.util.AnnotationUtils.isAnnotated;

import org.hibernate.testing.bytecode.enhancement.extension.engine.BytecodeEnhancedEngineDescriptor;
import org.hibernate.testing.bytecode.enhancement.extension.engine.BytecodeEnhancedTestEngine;
import org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor;
import org.junit.platform.engine.FilterResult;
import org.junit.platform.engine.TestDescriptor;

/**
 * Post-discovery filter that assigns {@link BytecodeEnhanced} test classes to the bytecode enhancement engine.
 * <p>
 * Jupiter still participates in discovery, both for the original launcher request and for the nested launcher requests
 * performed by {@link BytecodeEnhancedTestEngine}.  This filter keeps those two uses separate:
 * <ul>
 *     <li>For the outer launcher request, {@link BytecodeEnhanced} classes are removed from Jupiter's normal engine
 *     tree so they are not executed twice.</li>
 *     <li>For nested Jupiter discovery/execution started by {@link BytecodeEnhancedTestEngine}, filtering is disabled
 *     so Jupiter can discover and run the test methods for the enhanced class.</li>
 * </ul>
 * If the enhancement engine is not enabled, the filter fails fast when it sees a {@link BytecodeEnhanced} class instead
 * of silently letting Jupiter execute the unenhanced test.
 */
public class BytecodeEnhancementPostDiscoveryFilter implements org.junit.platform.launcher.PostDiscoveryFilter {
	@Override
	public FilterResult apply(TestDescriptor testDescriptor) {
		if ( BytecodeEnhancedTestEngine.isNestedJupiterExecution() ) {
			return FilterResult.included( "Nested Jupiter execution for bytecode enhancement." );
		}
		if ( testDescriptor instanceof ClassBasedTestDescriptor descriptor ) {
			TestDescriptor root = testDescriptor;
			while ( !root.isRoot() ) {
				root = root.getParent().orElseThrow();
			}

			boolean isEnhanced = isAnnotated( descriptor.getTestClass(), BytecodeEnhanced.class );
			if ( isEnhanced && !BytecodeEnhancedTestEngine.isEnabled() ) {
				throw new IllegalStateException(
						"BytecodeEnhancedTestEngine is disabled. But the tests rely on the @BytecodeEnhanced extensions: %s. In order to run this test, make sure to exactly align your dependency to JUnit with that of hibernate-testing, and set system property '%s=true'.".formatted(
								descriptor, BytecodeEnhancedTestEngine.ENHANCEMENT_EXTENSION_ENGINE_ENABLED ) );
			}
			if ( root instanceof BytecodeEnhancedEngineDescriptor ) {
				return FilterResult.included( "Bytecode enhancement engine descriptor." );
			}
			else {
				if ( isEnhanced ) {
					testDescriptor.removeFromHierarchy();
					return FilterResult.excluded( "Not bytecode enhanced engine, but test requires bytecode enhancement." );
				}
			}
		}
		return FilterResult.included( "Ok." );
	}
}
