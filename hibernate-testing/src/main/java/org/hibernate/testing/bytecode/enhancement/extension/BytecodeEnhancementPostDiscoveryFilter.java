/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.bytecode.enhancement.extension;


import static org.junit.platform.commons.util.AnnotationUtils.isAnnotated;

import org.hibernate.testing.bytecode.enhancement.extension.engine.BytecodeEnhancedEngineDescriptor;
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
			if ( root instanceof BytecodeEnhancedEngineDescriptor ) {
				if ( !isEnhanced ) {
					return FilterResult.excluded( "Not bytecode enhanced." );
				}
			}
			else {
				if ( isEnhanced ) {
					testDescriptor.removeFromHierarchy();
					return FilterResult.excluded( "Not bytecode enhanced." );
				}
			}
		}
		return FilterResult.included( "Ok." );
	}
}
