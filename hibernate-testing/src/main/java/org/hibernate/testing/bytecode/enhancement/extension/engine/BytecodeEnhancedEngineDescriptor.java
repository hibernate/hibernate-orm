/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.bytecode.enhancement.extension.engine;

import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;

/**
 * Root descriptor for the bytecode enhancement test engine.
 * <p>
 * This descriptor intentionally has no Jupiter-specific state.  {@link BytecodeEnhancedTestEngine} delegates
 * discovery and execution of Jupiter tests to nested Jupiter launcher calls and mirrors the resulting descriptors under
 * this root.
 */
public class BytecodeEnhancedEngineDescriptor extends EngineDescriptor {
	public BytecodeEnhancedEngineDescriptor(UniqueId uniqueId, String displayName) {
		super( uniqueId, displayName );
	}
}
