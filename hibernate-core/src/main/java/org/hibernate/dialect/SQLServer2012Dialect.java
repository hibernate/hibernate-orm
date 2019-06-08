/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

/**
 * A dialect for Microsoft SQL Server 2012
 *
 * @author Brett Meyer
 */
public class SQLServer2012Dialect extends SQLServerDialect {

	public SQLServer2012Dialect() {
		super(11);
	}

}
