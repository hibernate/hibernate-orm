/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.DatabaseVersion;

/**
 * An SQL dialect for DB2 9.7.
 *
 * @author Gail Badner
 * @deprecated use {@code DB2LegacyDialect(970)}
 */
@Deprecated
public class DB297Dialect extends DB2LegacyDialect {

	public DB297Dialect() {
		super( DatabaseVersion.make( 9, 7 ) );
	}

}
