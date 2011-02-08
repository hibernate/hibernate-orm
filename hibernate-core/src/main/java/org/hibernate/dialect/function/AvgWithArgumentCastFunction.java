/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.dialect.function;
import java.sql.Types;

/**
 * Some databases strictly return the type of the of the aggregation value for <tt>AVG</tt> which is
 * problematic in the case of averaging integers because the decimals will be dropped.  The usual workaround
 * is to cast the integer argument as some form of double/decimal.
 *
 * @author Steve Ebersole
 */
public class AvgWithArgumentCastFunction extends StandardAnsiSqlAggregationFunctions.AvgFunction {
	private final String castType;

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
