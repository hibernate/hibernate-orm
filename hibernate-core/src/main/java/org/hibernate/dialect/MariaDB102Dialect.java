/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.type.StandardBasicTypes;

import java.sql.Types;

public class MariaDB102Dialect extends MariaDB10Dialect {

	public MariaDB102Dialect() {
		super();

		this.registerColumnType( Types.JAVA_OBJECT, "json" );
		this.registerFunction( "json_valid", new StandardSQLFunction( "json_valid", StandardBasicTypes.NUMERIC_BOOLEAN ) );

	}

	@Override
	public boolean supportsColumnCheck() {
		return true;
	}
}
