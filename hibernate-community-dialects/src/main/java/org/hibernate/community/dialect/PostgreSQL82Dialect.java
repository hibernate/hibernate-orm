/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.DatabaseVersion;

/**
 * An SQL dialect for Postgres 8.2 and later, adds support for "if exists" when dropping tables
 * 
 * @author edalquist
 *
 * @deprecated use {@code PostgreSQLLegacyDialect(820)}
 */
@Deprecated
public class PostgreSQL82Dialect extends PostgreSQLLegacyDialect {

	public PostgreSQL82Dialect() {
		super( DatabaseVersion.make( 8, 2 ) );
	}

}
