/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.hbm2ddl;

/**
* @author Steve Ebersole
 *
 * @deprecated Everything in this package has been replaced with
 * {@link org.hibernate.tool.schema.spi.SchemaManagementTool} and friends.
*/
@Deprecated
interface Exporter {
	public boolean acceptsImportScripts();
	public void export(String string) throws Exception;
	public void release() throws Exception;
}
