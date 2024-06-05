/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.DatabaseVersion;

/**
 * An SQL dialect for MySQL 5.5.x specific features.
 *
 * @author Vlad Mihalcea
 *
 * @deprecated use {@code MySQLLegacyDialect(550)}
 */
@Deprecated
public class MySQL55Dialect extends MySQLLegacyDialect {

	public MySQL55Dialect() {
		super( DatabaseVersion.make( 5, 5 ) );
	}

}
