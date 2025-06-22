/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.bytecode.enhancement.extension.engine;

import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.descriptor.JupiterEngineDescriptor;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;

public class BytecodeEnhancedEngineDescriptor extends JupiterEngineDescriptor {
	public BytecodeEnhancedEngineDescriptor(UniqueId uniqueId, JupiterConfiguration configuration) {
		super( uniqueId, configuration );
	}

	public BytecodeEnhancedEngineDescriptor(JupiterEngineDescriptor engineDescriptor) {
		super( engineDescriptor.getUniqueId(), engineDescriptor.getConfiguration() );
		for ( TestDescriptor child : engineDescriptor.getChildren() ) {
			addChild( child );
		}
	}
}
