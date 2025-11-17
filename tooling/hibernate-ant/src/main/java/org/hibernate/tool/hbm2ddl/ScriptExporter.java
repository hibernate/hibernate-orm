/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
