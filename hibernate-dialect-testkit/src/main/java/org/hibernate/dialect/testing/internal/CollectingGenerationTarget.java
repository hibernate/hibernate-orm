/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.testing.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.tool.schema.spi.GenerationTarget;

/// Collects schema commands without executing them.
///
/// @author Steve Ebersole
final class CollectingGenerationTarget implements GenerationTarget {
	private final List<String> commands = new ArrayList<>();

	@Override
	public void prepare() {
	}

	@Override
	public void accept(String command) {
		commands.add( command );
	}

	@Override
	public void release() {
	}

	List<String> commands() {
		return List.copyOf( commands );
	}
}
