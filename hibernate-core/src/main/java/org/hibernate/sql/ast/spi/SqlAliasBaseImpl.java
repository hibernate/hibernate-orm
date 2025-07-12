/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi;

import org.hibernate.sql.ast.SqlTreeCreationLogger;

/**
 * Standard SqlAliasBase impl
 *
 * @author Steve Ebersole
 */
public class SqlAliasBaseImpl implements SqlAliasBase {
	private final String stem;
	private int aliasCount;

	public SqlAliasBaseImpl(String stem) {
		this.stem = stem;
	}

	@Override
	public String getAliasStem() {
		return stem;
	}

	@Override
	public String generateNewAlias() {
		final String alias = stem + "_" + ( aliasCount++ );
		SqlTreeCreationLogger.LOGGER.tracef( "Created new SQL alias: %s", alias );
		return alias;
	}

	@Override
	public String toString() {
		return "SqlAliasBase(" + stem + " : " + aliasCount + ")";
	}
}
