/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.LockOptions;

/**
 * @author Vlad Mihalcea
 */
public class MySQL8Dialect extends MySQL57Dialect {

	public MySQL8Dialect() {
		// MySQL doesn't add the new reserved keywords to their JDBC driver to preserve backward compatibility.

		registerKeyword("CUME_DIST");
		registerKeyword("DENSE_RANK");
		registerKeyword("EMPTY");
		registerKeyword("EXCEPT");
		registerKeyword("FIRST_VALUE");
		registerKeyword("GROUPS");
		registerKeyword("JSON_TABLE");
		registerKeyword("LAG");
		registerKeyword("LAST_VALUE");
		registerKeyword("LEAD");
		registerKeyword("NTH_VALUE");
		registerKeyword("NTILE");
		registerKeyword("PERSIST");
		registerKeyword("PERCENT_RANK");
		registerKeyword("PERSIST_ONLY");
		registerKeyword("RANK");
		registerKeyword("ROW_NUMBER");
	}

	@Override
	int getVersion() {
		return 800;
	}
}
