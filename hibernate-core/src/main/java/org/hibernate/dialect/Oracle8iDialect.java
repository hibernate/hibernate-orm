/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

/**
 * A dialect for Oracle 8i databases.
 *
 * @deprecated use {@code OracleDialect(8)}
 */
@Deprecated
public class Oracle8iDialect extends OracleDialect {

	public Oracle8iDialect() {
		super(8);
	}

}

