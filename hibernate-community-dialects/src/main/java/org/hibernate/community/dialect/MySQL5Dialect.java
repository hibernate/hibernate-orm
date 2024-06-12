/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.DatabaseVersion;

/**
 * An SQL dialect for MySQL 5.x specific features.
 *
 * @author Steve Ebersole
 *
 * @deprecated use {@code MySQLLegacyDialect(500)}
 */
@Deprecated
public class MySQL5Dialect extends MySQLLegacyDialect {

	public MySQL5Dialect() {
		super( DatabaseVersion.make( 5 ) );
	}

}
