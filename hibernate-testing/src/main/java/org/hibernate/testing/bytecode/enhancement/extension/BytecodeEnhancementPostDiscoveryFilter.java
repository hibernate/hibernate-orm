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

public class BytecodeEnhancementPostDiscoveryFilter implements org.junit.platform.launcher.PostDiscoveryFilter {
	@Override
	public FilterResult apply(TestDescriptor testDescriptor) {
		if ( testDescriptor instanceof ClassBasedTestDescriptor ) {
			ClassBasedTestDescriptor descriptor = (ClassBasedTestDescriptor) testDescriptor;

			TestDescriptor root = testDescriptor;
			while ( !root.isRoot() ) {
				root = root.getParent().get();
			}

			boolean isEnhanced = isAnnotated( descriptor.getTestClass(), BytecodeEnhanced.class );
			if ( isEnhanced && !BytecodeEnhancedTestEngine.isEnabled() ) {
				throw new IllegalStateException(
						"BytecodeEnhancedTestEngine is disabled. But the tests rely on the @BytecodeEnhanced extensions: %s. In order to run this test, make sure to exactly align your dependency to JUnit with that of hibernate-testing, and set system property '%s=true'.".formatted(
								descriptor, BytecodeEnhancedTestEngine.ENHANCEMENT_EXTENSION_ENGINE_ENABLED ) );
			}
			if ( root instanceof BytecodeEnhancedEngineDescriptor ) {
				if ( !isEnhanced ) {
					return FilterResult.excluded( "Not bytecode enhanced." );
				}
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
