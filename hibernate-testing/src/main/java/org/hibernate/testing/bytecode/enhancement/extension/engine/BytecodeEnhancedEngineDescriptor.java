/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
