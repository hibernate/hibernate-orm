/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.relational;

/**
 * A general SQL command to be used while initializing a schema.
 *
 * @author Steve Ebersole
 */
public class InitCommand {
	private final String[] initCommands;

	public InitCommand(String... initCommands) {
		this.initCommands = initCommands;
	}

	public String[] getInitCommands() {
		return initCommands;
	}
}
