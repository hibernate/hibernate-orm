/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.DatabaseVersion;

/**
 * @deprecated use {@code MariaDBLegacyDialect(1000)}
 */
@Deprecated
public class MariaDB10Dialect extends MariaDBLegacyDialect {

	public MariaDB10Dialect() {
		super( DatabaseVersion.make( 10 ) );
	}

}
