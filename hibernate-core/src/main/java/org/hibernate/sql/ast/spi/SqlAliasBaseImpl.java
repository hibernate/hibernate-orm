/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
		SqlTreeCreationLogger.LOGGER.debugf( "Created new SQL alias : %s", alias );
		return alias;
	}

	@Override
	public String toString() {
		return "SqlAliasBase(" + stem + " : " + aliasCount + ")";
	}
}
