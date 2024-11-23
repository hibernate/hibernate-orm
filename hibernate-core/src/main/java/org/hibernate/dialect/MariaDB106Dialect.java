/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

/**
 * An SQL dialect for MariaDB 10.6 and later, provides skip locked support.
 *
 * @author Christian Beikov
 */
public class MariaDB106Dialect extends MariaDB103Dialect {

	public MariaDB106Dialect() {
		super();
	}

	@Override
	public boolean supportsSkipLocked() {
		return true;
	}
}
