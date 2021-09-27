/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.hbm2ddl;

import org.hibernate.internal.build.AllowSysOut;

/**
 * @author Steve Ebersole
 *
 * @deprecated Everything in this package has been replaced with
 * {@link org.hibernate.tool.schema.spi.SchemaManagementTool} and friends.
 */
@Deprecated
class ScriptExporter implements Exporter {
	@Override
	public boolean acceptsImportScripts() {
		return false;
	}

	@Override
	@AllowSysOut
	public void export(String string) throws Exception {
		System.out.println( string );
	}

	@Override
	public void release() throws Exception {
	}
}
