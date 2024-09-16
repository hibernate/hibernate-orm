/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.spi;

/**
 * Contract for hiding the differences between a passed Writer, File or URL in
 * terms of how we write output scripts.
 *
 * @author Steve Ebersole
 */
public interface ScriptTargetOutput {
	/**
	 * Prepare the script target to {@linkplain #accept(String) accept} commands
	 */
	void prepare();

	/**
	 * Accept the given command and write it to the abstracted script
	 *
	 * @param command The command
	 */
	void accept(String command);

	/**
	 * Release this output
	 */
	void release();
}
