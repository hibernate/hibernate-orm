/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.internal.exec;

import org.hibernate.internal.build.AllowSysOut;
import org.hibernate.tool.schema.spi.GenerationTarget;

/**
 * A {@link GenerationTarget} that writed DDL to {@link System#out}.
 *
 * @author Steve Ebersole
 */
public class GenerationTargetToStdout implements GenerationTarget {
	private final String delimiter;

	public GenerationTargetToStdout(String delimiter) {
		this.delimiter = delimiter;
	}

	public GenerationTargetToStdout() {
		this ( null );
	}

	@Override
	public void prepare() {
		// nothing to do
	}

	@Override
	@AllowSysOut
	public void accept(String command) {
		if ( delimiter != null ) {
			command += delimiter;
		}
		System.out.println( command );
	}

	@Override
	public void release() {
	}

}
