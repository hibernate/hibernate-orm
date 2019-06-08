/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;


/**
 * An SQL dialect for DB2/390 version 8.
 *
 * @author Tobias Sternvik
 */
public class DB2390V8Dialect extends DB2390Dialect {

	public DB2390V8Dialect() {
		super(8);
	}
}
