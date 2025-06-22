/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.internal.exec;

import java.io.IOException;
import java.io.Writer;

import org.hibernate.tool.schema.spi.CommandAcceptanceException;
import org.hibernate.tool.schema.spi.ScriptTargetOutput;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractScriptTargetOutput implements ScriptTargetOutput {

	protected abstract Writer writer();

	@Override
	public void prepare() {
	}

	@Override
	public void accept(String command) {
		try {
			writer().write( command );
			writer().write( System.lineSeparator() );
			writer().flush();
		}
		catch (IOException e) {
			throw new CommandAcceptanceException( "Could not write \"" + command + "\" to target script file", e );
		}
	}

	@Override
	public void release() {
	}
}
