/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.type.StandardBasicTypes;

/**
 * An SQL dialect for CockroachDB 20.1 and later. Adds support for a few date/time functions.
 */
public class CockroachDB201Dialect extends CockroachDB192Dialect {
	public CockroachDB201Dialect() {
		super();
		registerFunction( "current_time", new NoArgSQLFunction("current_time", StandardBasicTypes.TIME, false) );
		registerFunction( "localtime", new NoArgSQLFunction("localtime", StandardBasicTypes.TIME, false) );
		registerFunction( "localtimestamp", new NoArgSQLFunction("localtimestamp", StandardBasicTypes.TIMESTAMP, false) );
		registerFunction( "timeofday", new NoArgSQLFunction("timeofday", StandardBasicTypes.STRING) );
	}
}
