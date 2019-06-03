/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

/**
 * @author Vlad Mihalcea
 */
public class MariaDB53Dialect extends MariaDBDialect {

	@Override
	int getMariaVersion() {
		return 530;
	}

	@Override
	int getVersion() {
		//this is a bit questionable: does
		//MariaBD 5.3 really support *every*
		//feature of MySQL 5.7?
		return 570;
	}
}
