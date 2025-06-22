/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.internal.exec;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.hibernate.internal.build.AllowSysOut;
import org.hibernate.tool.schema.spi.SchemaManagementException;

/**
 * @author Steve Ebersole
 */
public class ScriptTargetOutputToStdout extends AbstractScriptTargetOutput {
	private Writer writer;

	@Override
	protected Writer writer() {
		if ( writer == null ) {
			throw new SchemaManagementException( "Illegal state : writer null - not prepared" );
		}
		return writer;
	}

	@Override
	@AllowSysOut
	public void prepare() {
		super.prepare();
		this.writer = new OutputStreamWriter( System.out );
	}

	@Override
	public void accept(String command) {
		super.accept( command );
	}

	@Override
	public void release() {
		if ( writer != null ) {
			try {
				writer.close();
			}
			catch (IOException e) {
				throw new SchemaManagementException( "Unable to close file writer : " + e );
			}
			finally {
				writer = null;
			}
		}
	}
}
