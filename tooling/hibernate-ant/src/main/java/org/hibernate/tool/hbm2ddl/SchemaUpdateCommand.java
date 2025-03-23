/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.hbm2ddl;

/**
 * Pairs a SchemaUpdate SQL script with the boolean 'quiet'.  If true, it allows
 * the script to be run, ignoring all exceptions.
 *
 * @author Brett Meyer
 * @author Steve Ebersole
 *
 * @deprecated Everything in this package has been replaced with
 * {@link org.hibernate.tool.schema.spi.SchemaManagementTool} and friends.
 */
@Deprecated
public class SchemaUpdateCommand {
	private final String sql;
	private final boolean quiet;

	public SchemaUpdateCommand(String sql, boolean quiet) {
		this.sql = sql;
		this.quiet = quiet;
	}

	public String getSql() {
		return sql;
	}

	public boolean isQuiet() {
		return quiet;
	}
}
