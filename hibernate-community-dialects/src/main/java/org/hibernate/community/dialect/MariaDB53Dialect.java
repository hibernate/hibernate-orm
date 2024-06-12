/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.DatabaseVersion;

/**
 * @author Vlad Mihalcea
 *
 * @deprecated use {@code MariaDBLegacyDialect(530)}
 */
@Deprecated
public class MariaDB53Dialect extends MariaDBLegacyDialect {

	public MariaDB53Dialect() {
		super( DatabaseVersion.make( 5, 3 ) );
	}

}
