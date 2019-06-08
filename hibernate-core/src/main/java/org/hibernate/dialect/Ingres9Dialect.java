/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

/**
 * A SQL dialect for Ingres 9.3 and later versions.
 * <p/>
 * Changes:
 * <ul>
 * <li>Support for the SQL functions current_time, current_timestamp and current_date added</li>
 * <li>Type mapping of <code>Types.TIMESTAMP</code> changed from "timestamp with time zone" to "timestamp(9) with time zone"</li>
 * <li>Improved handling of "SELECT...FOR UPDATE" statements</li>
 * <li>Added support for pooled sequences</li>
 * <li>Added support for SELECT queries with limit and offset</li>
 * <li>Added getIdentitySelectString</li>
 * <li>Modified concatination operator</li>
 * </ul>
 *
 * @author Enrico Schenk
 * @author Raymond Fan
 */
public class Ingres9Dialect extends IngresDialect {

	public Ingres9Dialect() {
		super(930);
	}

}
