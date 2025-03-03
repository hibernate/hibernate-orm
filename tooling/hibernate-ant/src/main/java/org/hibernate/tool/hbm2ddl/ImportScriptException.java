/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.hbm2ddl;

import org.hibernate.HibernateException;

/**
 * @author Steve Ebersole
 *
 * @deprecated Everything in this package has been replaced with
 * {@link org.hibernate.tool.schema.spi.SchemaManagementTool} and friends.
 */
@Deprecated
public class ImportScriptException extends HibernateException {
	public ImportScriptException(String s) {
		super( s );
	}

	public ImportScriptException(String string, Throwable root) {
		super( string, root );
	}
}
