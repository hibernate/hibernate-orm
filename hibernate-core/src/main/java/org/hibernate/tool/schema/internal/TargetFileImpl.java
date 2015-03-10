/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.tool.schema.internal;

import java.io.FileWriter;
import java.io.IOException;

import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.hibernate.tool.schema.spi.Target;

/**
 * @author Steve Ebersole
 */
public class TargetFileImpl implements Target {
	private FileWriter fileWriter;

	public TargetFileImpl(String outputFile) {
		try {
			this.fileWriter = new FileWriter( outputFile );
		}
		catch (IOException e) {
			throw new SchemaManagementException( "Unable to open FileWriter [" + outputFile + "]", e );
		}
	}

	@Override
	public boolean acceptsImportScriptActions() {
		return true;
	}

	@Override
	public void prepare() {
	}

	@Override
	public void accept(String action) {
		try {
			fileWriter.write( action );
			fileWriter.write( "\n" );
		}
		catch (IOException e) {
			throw new SchemaManagementException( "Unable to write to FileWriter", e );
		}
	}

	@Override
	public void release() {
		if ( fileWriter != null ) {
			try {
				fileWriter.close();
			}
			catch (IOException ignore) {
			}
		}
	}
}
