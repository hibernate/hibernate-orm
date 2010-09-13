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
package org.hibernate.dialect;

import java.sql.Types;

import org.hibernate.dialect.function.AnsiTrimEmulationFunction;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.type.StandardBasicTypes;

/**
 * An SQL dialect targetting Sybase Adaptive Server Enterprise (ASE) 15 and higher.
 * <p/>
 * TODO : verify if this also works with 12/12.5
 * 
 * @author Gavin King
 */
public class SybaseASE15Dialect extends AbstractTransactSQLDialect {
	public SybaseASE15Dialect() {
		super();

		registerColumnType( Types.LONGVARBINARY, "image" );
		registerColumnType( Types.LONGVARCHAR, "text" );

		registerFunction( "second", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "datepart(second, ?1)") );
		registerFunction( "minute", new SQLFunctionTemplate(StandardBasicTypes.INTEGER, "datepart(minute, ?1)") );
		registerFunction( "hour", new SQLFunctionTemplate(StandardBasicTypes.INTEGER, "datepart(hour, ?1)") );
		registerFunction( "extract", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "datepart(?1, ?3)" ) );
		registerFunction( "mod", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "?1 % ?2" ) );
		registerFunction( "bit_length", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "datalength(?1) * 8" ) );
		registerFunction( "trim", new AnsiTrimEmulationFunction( AnsiTrimEmulationFunction.LTRIM, AnsiTrimEmulationFunction.RTRIM, "str_replace" ) ); 
	}

	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public boolean supportsCascadeDelete() {
		return false;
	}
	public int getMaxAliasLength() {
		return 30;
	}
    /**
     * By default, Sybase string comparisons are case-insensitive.
	 * <p/>
     * If the DB is configured to be case-sensitive, then this return
	 * value will be incorrect.
     */
    public boolean areStringComparisonsCaseInsensitive() {
        return true;
    }

	/**
	 * Actually Sybase does not support LOB locators at al.
	 *
	 * @return false.
	 */
	public boolean supportsExpectedLobUsagePattern() {
		return false;
	}
	public String getCrossJoinSeparator() {
		return ", ";
	}
}
