/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

/**
 * @deprecated use {@code MariaDBDialect(1020)}
 */
@Deprecated
public class MariaDB102Dialect extends MariaDBDialect {

	public MariaDB102Dialect() {
		super(1020);
	}

}
