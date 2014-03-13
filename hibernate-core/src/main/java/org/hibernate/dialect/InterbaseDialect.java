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

import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.dialect.function.VarArgsSQLFunction;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitHelper;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.type.StandardBasicTypes;

/**
 * An SQL dialect for Interbase.
 *
 * @author Gavin King
 */
public class InterbaseDialect extends Dialect {

	/**
	 * Constructs a InterbaseDialect
	 */
	public InterbaseDialect() {
		super();
		registerColumnType( Types.BIT, "smallint" );
		registerColumnType( Types.BIGINT, "numeric(18,0)" );
		registerColumnType( Types.SMALLINT, "smallint" );
		registerColumnType( Types.TINYINT, "smallint" );
		registerColumnType( Types.INTEGER, "integer" );
		registerColumnType( Types.CHAR, "char(1)" );
		registerColumnType( Types.VARCHAR, "varchar($l)" );
		registerColumnType( Types.FLOAT, "float" );
		registerColumnType( Types.DOUBLE, "double precision" );
		registerColumnType( Types.DATE, "date" );
		registerColumnType( Types.TIME, "time" );
		registerColumnType( Types.TIMESTAMP, "timestamp" );
		registerColumnType( Types.VARBINARY, "blob" );
		registerColumnType( Types.NUMERIC, "numeric($p,$s)" );
		registerColumnType( Types.BLOB, "blob" );
		registerColumnType( Types.CLOB, "blob sub_type 1" );
		registerColumnType( Types.BOOLEAN, "smallint" );
		
		registerFunction( "concat", new VarArgsSQLFunction( StandardBasicTypes.STRING, "(","||",")" ) );
		registerFunction( "current_date", new NoArgSQLFunction( "current_date", StandardBasicTypes.DATE, false ) );

		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, NO_BATCH );
	}

	@Override
	public String getAddColumnString() {
		return "add";
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {
		return "select " + getSelectSequenceNextValString( sequenceName ) + " from RDB$DATABASE";
	}

	@Override
	public String getSelectSequenceNextValString(String sequenceName) {
		return "gen_id( " + sequenceName + ", 1 )";
	}

	@Override
	public String getCreateSequenceString(String sequenceName) {
		return "create generator " + sequenceName;
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "delete from RDB$GENERATORS where RDB$GENERATOR_NAME = '" + sequenceName.toUpperCase() + "'";
	}

	@Override
	public String getQuerySequencesString() {
		return "select RDB$GENERATOR_NAME from RDB$GENERATORS";
	}

	@Override
	public String getForUpdateString() {
		return " with lock";
	}

	@Override
	public String getForUpdateString(String aliases) {
		return " for update of " + aliases + " with lock";
	}

	@Override
	public boolean supportsSequences() {
		return true;
	}

	@Override
    public LimitHandler buildLimitHandler(String sql, RowSelection selection) {
        return new AbstractLimitHandler(sql, selection) {
        	@Override
        	public String getProcessedSql() {
        		final boolean hasOffset = LimitHelper.hasFirstRow(selection);
        		return hasOffset ? sql + " rows ? to ?" : sql + " rows ?";
        	}

        	@Override
        	public boolean supportsLimit() {
        		return true;
        	}
        };
    }

	@Override
	public String getCurrentTimestampSelectString() {
		// TODO : not sure which (either?) is correct, could not find docs on how to do this.
		// did find various blogs and forums mentioning that select CURRENT_TIMESTAMP
		// does not work...
		return "{?= call CURRENT_TIMESTAMP }";
//		return "select CURRENT_TIMESTAMP from RDB$DATABASE";
	}

	@Override
	public boolean isCurrentTimestampSelectStringCallable() {
		return true;
	}
}
