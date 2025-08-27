/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.internal.exec;

import org.hibernate.tool.schema.spi.GenerationTarget;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.hibernate.tool.schema.spi.ScriptTargetOutput;

/**
 * A {@link GenerationTarget} that writes DDL to scripts.
 *
 * @author Steve Ebersole
 */
public class GenerationTargetToScript implements GenerationTarget {

	private final ScriptTargetOutput scriptTarget;
	private final String delimiter;

	public GenerationTargetToScript(
			ScriptTargetOutput scriptTarget,
			String delimiter) {
		if ( scriptTarget == null ) {
			throw new SchemaManagementException( "ScriptTargetOutput cannot be null" );
		}
		this.scriptTarget = scriptTarget;
		this.delimiter = delimiter;
	}

	@Override
	public void prepare() {
		scriptTarget.prepare();
	}

	@Override
	public void accept(String command) {
		if ( delimiter != null ) {
			command += delimiter;
		}
		scriptTarget.accept( command );
	}

	@Override
	public void release() {
		scriptTarget.release();
	}

}
