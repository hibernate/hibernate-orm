/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm;

import java.util.ArrayList;

import org.hibernate.tool.schema.spi.GenerationTarget;

/**
 * @author Steve Ebersole
 */
public class JournalingGenerationTarget implements GenerationTarget {
	private final ArrayList<String> commands = new ArrayList<>();

	@Override
	public void prepare() {
	}

	@Override
	public void accept(String command) {
		commands.add( command );
	}

	public ArrayList<String> getCommands() {
		return commands;
	}

	@Override
	public void release() {
	}

	public void clear() {
		commands.clear();
	}
}
