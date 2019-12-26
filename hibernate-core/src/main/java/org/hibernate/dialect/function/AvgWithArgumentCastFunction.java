/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.function;

import java.sql.Types;

/**
 * Some databases strictly return the type of the aggregation value for <tt>AVG</tt> which is
 * problematic in the case of averaging integers because the decimals will be dropped.  The usual workaround
 * is to cast the integer argument as some form of double/decimal.
 *
 * @author Steve Ebersole
 */
public class AvgWithArgumentCastFunction extends StandardAnsiSqlAggregationFunctions.AvgFunction {
	private final String castType;

	/**
	 * Constructs a AvgWithArgumentCastFunction
	 *
	 * @param castType The type to cast the avg argument to
	 */
	public AvgWithArgumentCastFunction(String castType) {
		this.castType = castType;
	}

	@Override
	protected String renderArgument(String argument, int firstArgumentJdbcType) {
		if ( firstArgumentJdbcType == Types.DOUBLE || firstArgumentJdbcType == Types.FLOAT ) {
			return argument;
		}
		else {
			return "cast(" + argument + " as " + castType + ")";
		}
	}
}
