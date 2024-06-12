/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.OracleDialect;

/**
 * An SQL dialect for Oracle 12c.
 *
 * @author zhouyanming (zhouyanming@gmail.com)
 *
 * @deprecated use {@code OracleLegacyDialect(12)}
 */
@Deprecated
public class Oracle12cDialect extends OracleDialect {

	public Oracle12cDialect() {
		super( DatabaseVersion.make( 12 ) );
	}
}
