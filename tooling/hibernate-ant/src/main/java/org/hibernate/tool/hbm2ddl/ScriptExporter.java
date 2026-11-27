package org.hibernate.tool.hbm2ddl;

import org.hibernate.internal.build.AllowSysOut;

/**
 * @author Steve Ebersole
 *
 * @deprecated Everything in this package has been replaced with
 * {@link org.hibernate.tool.schema.spi.SchemaManagementTool} and friends.
 */
@Deprecated
class ScriptExporter {
	public boolean acceptsImportScripts() {
		return false;
	}

	@AllowSysOut
	public void export(String string) throws Exception {
		System.out.println( string );
	}

	public void release() throws Exception {
	}
}
