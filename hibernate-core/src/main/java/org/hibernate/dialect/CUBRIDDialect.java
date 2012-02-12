/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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

import org.hibernate.MappingException;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.dialect.function.VarArgsSQLFunction;
import org.hibernate.type.StandardBasicTypes;

/**
 * An SQL dialect for CUBRID (8.3.x and later).
 *
 * @author Seok Jeong Il
 */
public class CUBRIDDialect extends Dialect {
    @Override
    protected String getIdentityColumnString() throws MappingException {
        return "auto_increment"; //starts with 1, implicitly
    }

    @Override
    public String getIdentitySelectString(String table, String column, int type)
            throws MappingException {
        // CUBRID 8.4.0 support last_insert_id()
        // return "select last_insert_id()";
        return "select current_val from db_serial where name = '" + ( table + "_ai_" + column ).toLowerCase() + "'";
    }

    public CUBRIDDialect() {
        super();

        registerColumnType( Types.BIT, "bit(8)" );
        registerColumnType( Types.BIGINT, "numeric(19,0)" );
        registerColumnType( Types.SMALLINT, "short" );
        registerColumnType( Types.TINYINT, "short" );
        registerColumnType( Types.INTEGER, "integer" );
        registerColumnType( Types.CHAR, "char(1)" );
        registerColumnType( Types.VARCHAR, 4000, "varchar($l)" );
        registerColumnType( Types.FLOAT, "float" );
        registerColumnType( Types.DOUBLE, "double" );
        registerColumnType( Types.DATE, "date" );
        registerColumnType( Types.TIME, "time" );
        registerColumnType( Types.TIMESTAMP, "timestamp" );
        registerColumnType( Types.VARBINARY, 2000, "bit varying($l)" );
        registerColumnType( Types.NUMERIC, "numeric($p,$s)" );
        registerColumnType( Types.BLOB, "blob" );
        registerColumnType( Types.CLOB, "string" );

        getDefaultProperties().setProperty( Environment.USE_STREAMS_FOR_BINARY, "true" );
        getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE );

        registerFunction( "substring", new StandardSQLFunction( "substr", StandardBasicTypes.STRING ) );
        registerFunction( "trim", new StandardSQLFunction( "trim" ) );
        registerFunction( "length", new StandardSQLFunction( "length", StandardBasicTypes.INTEGER ) );
        registerFunction( "bit_length", new StandardSQLFunction( "bit_length", StandardBasicTypes.INTEGER ) );
        registerFunction( "coalesce", new StandardSQLFunction( "coalesce" ) );
        registerFunction( "nullif", new StandardSQLFunction( "nullif" ) );
        registerFunction( "abs", new StandardSQLFunction( "abs" ) );
        registerFunction( "mod", new StandardSQLFunction( "mod" ) );
        registerFunction( "upper", new StandardSQLFunction( "upper" ) );
        registerFunction( "lower", new StandardSQLFunction( "lower" ) );

        registerFunction( "power", new StandardSQLFunction( "power" ) );
        registerFunction( "stddev", new StandardSQLFunction( "stddev" ) );
        registerFunction( "variance", new StandardSQLFunction( "variance" ) );
        registerFunction( "round", new StandardSQLFunction( "round" ) );
        registerFunction( "trunc", new StandardSQLFunction( "trunc" ) );
        registerFunction( "ceil", new StandardSQLFunction( "ceil" ) );
        registerFunction( "floor", new StandardSQLFunction( "floor" ) );
        registerFunction( "ltrim", new StandardSQLFunction( "ltrim" ) );
        registerFunction( "rtrim", new StandardSQLFunction( "rtrim" ) );
        registerFunction( "nvl", new StandardSQLFunction( "nvl" ) );
        registerFunction( "nvl2", new StandardSQLFunction( "nvl2" ) );
        registerFunction( "sign", new StandardSQLFunction( "sign", StandardBasicTypes.INTEGER ) );
        registerFunction( "chr", new StandardSQLFunction( "chr", StandardBasicTypes.CHARACTER ) );
        registerFunction( "to_char", new StandardSQLFunction( "to_char", StandardBasicTypes.STRING ) );
        registerFunction( "to_date", new StandardSQLFunction( "to_date", StandardBasicTypes.TIMESTAMP ) );
        registerFunction( "last_day", new StandardSQLFunction( "last_day", StandardBasicTypes.DATE ) );
        registerFunction( "instr", new StandardSQLFunction( "instr", StandardBasicTypes.INTEGER ) );
        registerFunction( "instrb", new StandardSQLFunction( "instrb", StandardBasicTypes.INTEGER ) );
        registerFunction( "lpad", new StandardSQLFunction( "lpad", StandardBasicTypes.STRING ) );
        registerFunction( "replace", new StandardSQLFunction( "replace", StandardBasicTypes.STRING ) );
        registerFunction( "rpad", new StandardSQLFunction( "rpad", StandardBasicTypes.STRING ) );
        registerFunction( "substr", new StandardSQLFunction( "substr", StandardBasicTypes.STRING ) );
        registerFunction( "substrb", new StandardSQLFunction( "substrb", StandardBasicTypes.STRING ) );
        registerFunction( "translate", new StandardSQLFunction( "translate", StandardBasicTypes.STRING ) );
        registerFunction( "add_months", new StandardSQLFunction( "add_months", StandardBasicTypes.DATE ) );
        registerFunction( "months_between", new StandardSQLFunction( "months_between", StandardBasicTypes.FLOAT ) );

        registerFunction( "current_date", new NoArgSQLFunction( "current_date", StandardBasicTypes.DATE, false ) );
        registerFunction( "current_time", new NoArgSQLFunction( "current_time", StandardBasicTypes.TIME, false ) );
        registerFunction(
                "current_timestamp",
                new NoArgSQLFunction( "current_timestamp", StandardBasicTypes.TIMESTAMP, false )
        );
        registerFunction( "sysdate", new NoArgSQLFunction( "sysdate", StandardBasicTypes.DATE, false ) );
        registerFunction( "systime", new NoArgSQLFunction( "systime", StandardBasicTypes.TIME, false ) );
        registerFunction( "systimestamp", new NoArgSQLFunction( "systimestamp", StandardBasicTypes.TIMESTAMP, false ) );
        registerFunction( "user", new NoArgSQLFunction( "user", StandardBasicTypes.STRING, false ) );
        registerFunction( "rownum", new NoArgSQLFunction( "rownum", StandardBasicTypes.LONG, false ) );
        registerFunction( "concat", new VarArgsSQLFunction( StandardBasicTypes.STRING, "", "||", "" ) );
    }

    public String getAddColumnString() {
        return "add";
    }

    public String getSequenceNextValString(String sequenceName) {
        return "select " + sequenceName + ".next_value from table({1}) as T(X)";
    }

    public String getCreateSequenceString(String sequenceName) {
        return "create serial " + sequenceName;
    }

    public String getDropSequenceString(String sequenceName) {
        return "drop serial " + sequenceName;
    }

    public boolean supportsSequences() {
        return true;
    }

    public String getQuerySequencesString() {
        return "select name from db_serial";
    }

    public boolean dropConstraints() {
        return false;
    }

    public boolean supportsLimit() {
        return true;
    }

    public String getLimitString(String sql, boolean hasOffset) {
        // CUBRID 8.3.0 support limit
        return new StringBuilder( sql.length() + 20 ).append( sql )
                .append( hasOffset ? " limit ?, ?" : " limit ?" ).toString();
    }

    public boolean bindLimitParametersInReverseOrder() {
        return true;
    }

    public boolean useMaxForLimit() {
        return true;
    }

    public boolean forUpdateOfColumns() {
        return true;
    }

    public char closeQuote() {
        return ']';
    }

    public char openQuote() {
        return '[';
    }

    public boolean hasAlterTable() {
        return false;
    }

    public String getForUpdateString() {
        return " ";
    }

    public boolean supportsUnionAll() {
        return true;
    }

    public boolean supportsCommentOn() {
        return false;
    }

    public boolean supportsTemporaryTables() {
        return false;
    }

    public boolean supportsCurrentTimestampSelection() {
        return true;
    }

    public String getCurrentTimestampSelectString() {
        return "select systimestamp from table({1}) as T(X)";
    }

    public boolean isCurrentTimestampSelectStringCallable() {
        return false;
    }
}
