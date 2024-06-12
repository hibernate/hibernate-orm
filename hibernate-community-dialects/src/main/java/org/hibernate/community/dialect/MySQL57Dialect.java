/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.DatabaseVersion;

/**
 * @author Gail Badner
 *
 * @deprecated use {@code MySQLLegacyDialect(570)}
 */
@Deprecated
public class MySQL57Dialect extends MySQLLegacyDialect {

	public MySQL57Dialect() {
		super( DatabaseVersion.make( 5, 7 ) );
	}

}
