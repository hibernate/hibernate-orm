/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.DatabaseVersion;

/**
 * An SQL dialect for Postgres 9 and later.
 * Adds support for "if exists" when dropping constraints
 * 
 * @author edalquist
 *
 * @deprecated use {@code PostgreSQLLegacyDialect(900)}
 */
@Deprecated
public class PostgreSQL9Dialect extends PostgreSQLLegacyDialect {

	public PostgreSQL9Dialect() {
		super( DatabaseVersion.make( 9 ) );
	}

}
