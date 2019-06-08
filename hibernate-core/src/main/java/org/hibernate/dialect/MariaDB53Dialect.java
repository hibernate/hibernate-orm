/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

/**
 * @author Vlad Mihalcea
 *
 * @deprecated use {@code MariaDBDialect(530)}
 */
@Deprecated
public class MariaDB53Dialect extends MariaDBDialect {

	public MariaDB53Dialect() {
		super(530);
	}

}
