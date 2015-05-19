/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.hbm2ddl;

/**
 * @author Steve Ebersole
 */
class ScriptExporter implements Exporter {
	@Override
	public boolean acceptsImportScripts() {
		return false;
	}

	@Override
	public void export(String string) throws Exception {
		System.out.println( string );
	}

	@Override
	public void release() throws Exception {
	}
}
