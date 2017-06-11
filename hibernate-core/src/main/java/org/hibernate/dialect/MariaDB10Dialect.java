/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.type.StandardBasicTypes;

public class MariaDB10Dialect extends MariaDB53Dialect {

	public MariaDB10Dialect() {
		super();

		registerFunction( "regexp_replace", new StandardSQLFunction( "regexp_replace", StandardBasicTypes.STRING ) );
		registerFunction( "regexp_instr", new StandardSQLFunction( "regexp_instr", StandardBasicTypes.INTEGER ) );
		registerFunction( "regexp_substr", new StandardSQLFunction( "regexp_substr", StandardBasicTypes.STRING ) );
		registerFunction( "weight_string", new StandardSQLFunction( "weight_string", StandardBasicTypes.STRING ) );
		registerFunction( "to_base64", new StandardSQLFunction( "to_base64", StandardBasicTypes.STRING ) );
		registerFunction( "from_base64", new StandardSQLFunction( "from_base64", StandardBasicTypes.STRING ) );
	}

	@Override
	public boolean supportsIfExistsBeforeConstraintName() {
		return true;
	}
}
