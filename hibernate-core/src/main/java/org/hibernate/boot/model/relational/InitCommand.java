/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.relational;

import java.util.Arrays;

/**
 * A general SQL command to be used while initializing a schema.
 *
 * @author Steve Ebersole
 */
public record InitCommand(String... initCommands) {
	@Deprecated(since = "7")
	public String[] getInitCommands() {
		return initCommands;
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof InitCommand that
			&& Arrays.equals( this.initCommands, that.initCommands );
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode( initCommands );
	}
}
