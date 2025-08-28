/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.internal.exec;

import java.io.Writer;

import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.hibernate.tool.schema.spi.ScriptTargetOutput;

/**
 * ScriptTargetOutput implementation for supplied Writer references.
 * <p>
 * Specifically, we are handed this Writer so we do not want to close it.
 *
 * @author Steve Ebersole
 */
public class ScriptTargetOutputToWriter extends AbstractScriptTargetOutput implements ScriptTargetOutput {
	private final Writer writer;

	/**
	 * Constructs a ScriptTargetOutputToWriter
	 *
	 * @param writer The writer to write to
	 */
	public ScriptTargetOutputToWriter(Writer writer) {
		if ( writer == null ) {
			throw new SchemaManagementException( "Writer cannot be null" );
		}
		this.writer = writer;
	}

	@Override
	protected Writer writer() {
		return writer;
	}
}
