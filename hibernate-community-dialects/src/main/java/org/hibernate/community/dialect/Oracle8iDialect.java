/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.DatabaseVersion;

/**
 * A dialect for Oracle 8i databases.
 *
 * @deprecated use {@code OracleLegacyDialect(8)}
 */
@Deprecated
public class Oracle8iDialect extends OracleLegacyDialect {

	public Oracle8iDialect() {
		super( DatabaseVersion.make( 8 ) );
	}

}

