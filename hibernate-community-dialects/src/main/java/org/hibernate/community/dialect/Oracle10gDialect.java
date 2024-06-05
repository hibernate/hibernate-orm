/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.DatabaseVersion;

/**
 * A dialect specifically for use with Oracle 10g.
 * <p>
 * The main difference between this dialect and
 * {@link Oracle9iDialect} is the use of
 * "ANSI join syntax".
 *
 * @author Steve Ebersole
 *
 * @deprecated use {@code OracleLegacyDialect(10)}
 */
@Deprecated
public class Oracle10gDialect extends OracleLegacyDialect {

	public Oracle10gDialect() {
		super( DatabaseVersion.make( 10 ) );
	}
}
