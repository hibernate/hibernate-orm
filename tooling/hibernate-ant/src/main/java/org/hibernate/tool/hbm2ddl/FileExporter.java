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
class FileExporter {
	private final FileWriter writer;

	public FileExporter(String outputFile) throws IOException {
		this.writer = new FileWriter( outputFile );
	}

	public boolean acceptsImportScripts() {
		return false;
	}

	public void export(String string) throws Exception {
		writer.write( string + '\n');
	}

	public void release() throws Exception {
		writer.flush();
		writer.close();
	}
}
