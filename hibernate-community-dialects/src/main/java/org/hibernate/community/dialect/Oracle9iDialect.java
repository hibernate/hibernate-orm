/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.DatabaseVersion;

/**
 * A dialect for Oracle 9i databases.
 * <p>
 * Specifies to not use "ANSI join syntax" because 9i does not
 * seem to properly handle it in all cases.
 *
 * @author Steve Ebersole
 *
 * @deprecated use {@code OracleLegacyDialect(9)}
 */
@Deprecated
public class Oracle9iDialect extends OracleLegacyDialect {

	public Oracle9iDialect() {
		super( DatabaseVersion.make( 9 ) );
	}

}
