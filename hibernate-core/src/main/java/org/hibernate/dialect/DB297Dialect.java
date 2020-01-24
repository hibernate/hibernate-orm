/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

/**
 * An SQL dialect for DB2 9.7.
 *
 * @author Gail Badner
 *
 * @deprecated use {@code DB2Dialect(970)}
 */
@Deprecated
public class DB297Dialect extends DB2Dialect {

	@Override
	int getVersion() {
		return 970;
	}

}
