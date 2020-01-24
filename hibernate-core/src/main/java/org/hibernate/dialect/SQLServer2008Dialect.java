/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

/**
 * A dialect for Microsoft SQL Server 2008 with JDBC Driver 3.0 and above
 *
 * @author Gavin King
 *
 * @deprecated use {@code SQLServerDialect(10)}
 */
@Deprecated
public class SQLServer2008Dialect extends SQLServerDialect {

	public SQLServer2008Dialect() {
		super(10);
	}

}
