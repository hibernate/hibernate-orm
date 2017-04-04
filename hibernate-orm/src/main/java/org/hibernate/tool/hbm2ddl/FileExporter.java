/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.hbm2ddl;

import java.io.FileWriter;
import java.io.IOException;

/**
 * @author Steve Ebersole
 *
 * @deprecated Everything in this package has been replaced with
 * {@link org.hibernate.tool.schema.spi.SchemaManagementTool} and friends.
 */
@Deprecated
class FileExporter implements Exporter {
	private final FileWriter writer;

	public FileExporter(String outputFile) throws IOException {
		this.writer = new FileWriter( outputFile );
	}

	@Override
	public boolean acceptsImportScripts() {
		return false;
	}

	@Override
	public void export(String string) throws Exception {
		writer.write( string + '\n');
	}

	@Override
	public void release() throws Exception {
		writer.flush();
		writer.close();
	}
}
