package org.hibernate.dialect;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;
import org.hibernate.MappingException;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitHelper;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorHSQLDBDatabaseImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.type.StandardBasicTypes;

public class ImpalaDialect extends Dialect {

    private final class ImpalaLimitHandler extends AbstractLimitHandler {
        @Override
        public String processSql(String sql, RowSelection selection) {
            final boolean hasOffset = LimitHelper.hasFirstRow( selection );
            return sql + (hasOffset ? " limit ? offset ?" : " limit ?");
        }

        @Override
        public boolean supportsLimit() {
            return true;
        }

        @Override
        public boolean bindLimitParametersFirst() {
            return true;
        }
    }

    private final LimitHandler limitHandler;


    /**
     * Constructs a ImpalaDialect
     */
    public ImpalaDialect() {
        super();

        registerColumnType( Types.BIGINT, "bigint" );
        registerColumnType( Types.BOOLEAN, "boolean" );
        registerColumnType( Types.CHAR, "char($l)" );
        registerColumnType( Types.DECIMAL, "decimal($p,$s)" );
        registerColumnType( Types.DOUBLE, "double" );
        registerColumnType( Types.FLOAT, "float" );
        registerColumnType( Types.INTEGER, "int" );
        registerColumnType( Types.SMALLINT, "smallint" );
        registerColumnType( Types.VARCHAR, "string" );
        registerColumnType( Types.BLOB, "string" );
        registerColumnType( Types.CLOB, "string" );
        registerColumnType( Types.TINYINT, "tinyint" );
        registerColumnType( Types.TIMESTAMP, "timestamp" );

        // Impala Mathematical Functions
        registerFunction( "abs", new StandardSQLFunction("abs") );
        registerFunction( "acos", new StandardSQLFunction( "acos", StandardBasicTypes.DOUBLE ) );
        registerFunction( "asin", new StandardSQLFunction( "asin", StandardBasicTypes.DOUBLE ) );
        registerFunction( "atan", new StandardSQLFunction("atan", StandardBasicTypes.DOUBLE) );
        registerFunction( "atan2", new StandardSQLFunction("atan2", StandardBasicTypes.DOUBLE) );
        registerFunction( "bin", new StandardSQLFunction( "bin", StandardBasicTypes.STRING ) );
        registerFunction( "ceil", new StandardSQLFunction("ceil") );
        registerFunction( "dceil", new StandardSQLFunction("dceil") );
        registerFunction( "ceiling", new StandardSQLFunction("ceiling") );
        registerFunction( "conv", new StandardSQLFunction("conv") );
        registerFunction( "cos", new StandardSQLFunction( "cos", StandardBasicTypes.DOUBLE ) );
        registerFunction( "cot", new StandardSQLFunction( "cot", StandardBasicTypes.DOUBLE ) );
        registerFunction( "cosh", new StandardSQLFunction( "cosh", StandardBasicTypes.DOUBLE ) );
        registerFunction( "degrees", new StandardSQLFunction( "degrees", StandardBasicTypes.DOUBLE ) );
        registerFunction( "e", new StandardSQLFunction( "e", StandardBasicTypes.DOUBLE ) );
        registerFunction( "exp", new StandardSQLFunction( "exp", StandardBasicTypes.DOUBLE ) );
        registerFunction( "factorial", new StandardSQLFunction( "factorial", StandardBasicTypes.INTEGER) );
        registerFunction( "floor", new StandardSQLFunction( "floor", StandardBasicTypes.INTEGER ) );
        registerFunction( "dfloor", new StandardSQLFunction( "dfloor", StandardBasicTypes.INTEGER ) );
        registerFunction( "fmod", new StandardSQLFunction("fmod") );
        registerFunction( "fnv_hash", new StandardSQLFunction( "fnv_hash", StandardBasicTypes.INTEGER ) );
        registerFunction( "greatest", new StandardSQLFunction("greatest") );
        registerFunction( "hex", new StandardSQLFunction( "hex", StandardBasicTypes.STRING ) );
        registerFunction( "is_inf", new StandardSQLFunction( "is_inf", StandardBasicTypes.BOOLEAN ) );
        registerFunction( "is_nan", new StandardSQLFunction( "is_nan", StandardBasicTypes.BOOLEAN ) );
        registerFunction( "least", new StandardSQLFunction("least") );
        registerFunction( "ln", new StandardSQLFunction( "ln", StandardBasicTypes.DOUBLE ) );
        registerFunction( "log", new StandardSQLFunction( "log", StandardBasicTypes.DOUBLE ) );
        registerFunction( "log10", new StandardSQLFunction( "log10", StandardBasicTypes.DOUBLE ) );
        registerFunction( "log2", new StandardSQLFunction( "log2", StandardBasicTypes.DOUBLE ) );
        registerFunction( "max_int", new StandardSQLFunction( "max_int", StandardBasicTypes.INTEGER ) );
        registerFunction( "max_bigint", new StandardSQLFunction( "max_bigint", StandardBasicTypes.BIG_INTEGER ) );
        registerFunction( "max_smallint", new StandardSQLFunction( "max_smallint", StandardBasicTypes.INTEGER ) );
        registerFunction( "max_tinyint", new StandardSQLFunction( "max_tinyint", StandardBasicTypes.INTEGER ) );
        registerFunction( "min_int", new StandardSQLFunction( "min_int", StandardBasicTypes.INTEGER ) );
        registerFunction( "min_bigint", new StandardSQLFunction( "min_bigint", StandardBasicTypes.BIG_INTEGER ) );
        registerFunction( "min_smallint", new StandardSQLFunction( "min_smallint", StandardBasicTypes.INTEGER ) );
        registerFunction( "min_tinyint", new StandardSQLFunction( "min_tinyint", StandardBasicTypes.INTEGER ) );
        registerFunction( "mod", new StandardSQLFunction( "mod" ) );
        registerFunction( "murmur_hash", new StandardSQLFunction( "murmur_hash", StandardBasicTypes.BIG_INTEGER ) );
        registerFunction( "negative", new StandardSQLFunction( "negative" ) );
        registerFunction( "pi", new StandardSQLFunction( "pi", StandardBasicTypes.DOUBLE ) );
        registerFunction( "pmod", new StandardSQLFunction( "pmod" ) );
        registerFunction( "positive", new StandardSQLFunction( "positive" ) );
        registerFunction( "pow", new StandardSQLFunction( "pow", StandardBasicTypes.DOUBLE ) );
        registerFunction( "power", new StandardSQLFunction( "power", StandardBasicTypes.DOUBLE ) );
        registerFunction( "dpow", new StandardSQLFunction( "dpow", StandardBasicTypes.DOUBLE ) );
        registerFunction( "fpow", new StandardSQLFunction( "fpow", StandardBasicTypes.DOUBLE ) );
        registerFunction( "precision", new StandardSQLFunction( "precision", StandardBasicTypes.INTEGER ) );
        registerFunction( "quotient", new StandardSQLFunction( "quotient", StandardBasicTypes.INTEGER ) );
        registerFunction( "radians", new StandardSQLFunction( "radians", StandardBasicTypes.DOUBLE ) );
        registerFunction( "rand", new StandardSQLFunction( "rand", StandardBasicTypes.DOUBLE ) );
        registerFunction( "random", new StandardSQLFunction( "random", StandardBasicTypes.DOUBLE ) );
        registerFunction( "round", new StandardSQLFunction( "round" ) );
        registerFunction( "dround", new StandardSQLFunction( "dround" ) );
        registerFunction( "scale", new StandardSQLFunction( "scale", StandardBasicTypes.INTEGER ) );
        registerFunction( "sign", new StandardSQLFunction( "sign", StandardBasicTypes.INTEGER ) );
        registerFunction( "sin", new StandardSQLFunction( "sin", StandardBasicTypes.DOUBLE ) );
        registerFunction( "sinh", new StandardSQLFunction( "sinh", StandardBasicTypes.DOUBLE ) );
        registerFunction( "sqrt", new StandardSQLFunction( "sqrt", StandardBasicTypes.DOUBLE ) );
        registerFunction( "tan", new StandardSQLFunction( "tan", StandardBasicTypes.DOUBLE ) );
        registerFunction( "tanh", new StandardSQLFunction("tanh", StandardBasicTypes.DOUBLE) );
        registerFunction( "truncate", new StandardSQLFunction( "truncate" ) );
        registerFunction( "dtrunc", new StandardSQLFunction( "dtrunc" ) );
        registerFunction( "trunc", new StandardSQLFunction( "trunc" ) );
        registerFunction( "unhex", new StandardSQLFunction( "unhex", StandardBasicTypes.STRING ) );
        registerFunction( "width_bucket", new StandardSQLFunction( "width_bucket" ) );

        // Impala Bit Functions
        registerFunction( "bitand", new StandardSQLFunction( "bitand" ) );
        registerFunction( "bitor", new StandardSQLFunction( "bitor" ) );
        registerFunction( "bitnot", new StandardSQLFunction( "bitnot" ) );
        registerFunction( "bitxor", new StandardSQLFunction( "bitxor" ) );
        registerFunction( "countset", new StandardSQLFunction( "countset" ) );
        registerFunction( "getbit", new StandardSQLFunction( "getbit" ) );
        registerFunction( "rotateleft", new StandardSQLFunction( "rotateleft" ) );
        registerFunction( "rotateright", new StandardSQLFunction( "rotateright" ) );
        registerFunction( "setbit", new StandardSQLFunction( "setbit" ) );
        registerFunction( "shiftleft", new StandardSQLFunction( "shiftleft" ) );
        registerFunction( "shiftright", new StandardSQLFunction( "shiftright" ) );

        // Impala Date and Time Functions
        registerFunction( "add_months", new StandardSQLFunction( "add_months", StandardBasicTypes.TIMESTAMP ) );
        registerFunction( "adddate", new StandardSQLFunction( "adddate", StandardBasicTypes.TIMESTAMP ) );
        registerFunction( "current_timestamp", new StandardSQLFunction( "current_timestamp", StandardBasicTypes.TIMESTAMP ) );
        registerFunction( "date_add", new StandardSQLFunction( "date_add", StandardBasicTypes.TIMESTAMP ) );
        registerFunction( "date_part", new StandardSQLFunction( "date_part", StandardBasicTypes.TIMESTAMP ) );
        registerFunction( "date_sub", new StandardSQLFunction( "date_sub", StandardBasicTypes.TIMESTAMP ) );
        registerFunction( "date_trunc", new StandardSQLFunction( "date_trunc", StandardBasicTypes.TIMESTAMP ) );
        registerFunction( "datediff", new StandardSQLFunction( "datediff", StandardBasicTypes.INTEGER ) );
        registerFunction( "day", new StandardSQLFunction( "day", StandardBasicTypes.INTEGER ) );
        registerFunction( "dayname", new StandardSQLFunction( "dayname", StandardBasicTypes.STRING ) );
        registerFunction( "dayofweek", new StandardSQLFunction( "dayofweek", StandardBasicTypes.INTEGER ) );
        registerFunction( "dayofmonth", new StandardSQLFunction( "dayofmonth", StandardBasicTypes.INTEGER ) );
        registerFunction( "dayofyear", new StandardSQLFunction( "dayofyear", StandardBasicTypes.INTEGER ) );
        registerFunction( "days_add", new StandardSQLFunction( "days_add", StandardBasicTypes.TIMESTAMP ) );
        registerFunction( "days_sub", new StandardSQLFunction( "days_sub", StandardBasicTypes.TIMESTAMP ) );
        registerFunction( "extract", new StandardSQLFunction( "extract", StandardBasicTypes.INTEGER ) );
        registerFunction( "from_timestamp", new StandardSQLFunction( "from_timestamp", StandardBasicTypes.STRING ) );
        registerFunction( "from_unixtime", new StandardSQLFunction( "from_unixtime", StandardBasicTypes.STRING ) );
        registerFunction( "from_utc_timestamp", new StandardSQLFunction( "from_utc_timestamp", StandardBasicTypes.TIMESTAMP ) );
        registerFunction( "hour", new StandardSQLFunction( "hour", StandardBasicTypes.INTEGER ) );
        registerFunction( "hours_add", new StandardSQLFunction( "hours_add", StandardBasicTypes.TIMESTAMP ) );
        registerFunction( "hours_sub", new StandardSQLFunction( "hours_sub", StandardBasicTypes.TIMESTAMP ) );
        registerFunction( "int_months_between", new StandardSQLFunction( "int_months_between", StandardBasicTypes.INTEGER ) );
        registerFunction( "microseconds_add", new StandardSQLFunction( "microseconds_add", StandardBasicTypes.TIMESTAMP ) );
        registerFunction( "microseconds_sub", new StandardSQLFunction( "microseconds_sub", StandardBasicTypes.TIMESTAMP ) );
        registerFunction( "millisecond", new StandardSQLFunction( "millisecond", StandardBasicTypes.INTEGER ) );
        registerFunction( "milliseconds_add", new StandardSQLFunction( "milliseconds_add", StandardBasicTypes.TIMESTAMP ) );
        registerFunction( "milliseconds_sub", new StandardSQLFunction( "milliseconds_sub", StandardBasicTypes.TIMESTAMP ) );
        registerFunction( "minute", new StandardSQLFunction( "minute", StandardBasicTypes.INTEGER ) );
        registerFunction( "minutes_add", new StandardSQLFunction( "minutes_add", StandardBasicTypes.TIMESTAMP ) );
        registerFunction( "minutes_sub", new StandardSQLFunction( "minutes_sub", StandardBasicTypes.TIMESTAMP ) );
        registerFunction( "month", new StandardSQLFunction( "month", StandardBasicTypes.INTEGER ) );
        registerFunction( "monthname", new StandardSQLFunction( "monthname", StandardBasicTypes.STRING ) );
        registerFunction( "months_add", new StandardSQLFunction( "months_add", StandardBasicTypes.TIMESTAMP ) );
        registerFunction( "months_between", new StandardSQLFunction( "months_between", StandardBasicTypes.TIMESTAMP ) );
        registerFunction( "months_sub", new StandardSQLFunction( "months_sub", StandardBasicTypes.TIMESTAMP ) );
        registerFunction( "nanoseconds_add", new StandardSQLFunction( "nanoseconds_add", StandardBasicTypes.TIMESTAMP ) );
        registerFunction( "nanoseconds_sub", new StandardSQLFunction( "nanoseconds_sub", StandardBasicTypes.TIMESTAMP ) );
        registerFunction( "next_day", new StandardSQLFunction( "next_day", StandardBasicTypes.TIMESTAMP ) );
        registerFunction( "now", new StandardSQLFunction( "now", StandardBasicTypes.TIMESTAMP ) );
        registerFunction( "quarter", new StandardSQLFunction( "quarter", StandardBasicTypes.INTEGER ) );
        registerFunction( "second", new StandardSQLFunction( "second", StandardBasicTypes.INTEGER ) );
        registerFunction( "seconds_add", new StandardSQLFunction( "seconds_add", StandardBasicTypes.TIMESTAMP ) );
        registerFunction( "seconds_sub", new StandardSQLFunction( "seconds_sub", StandardBasicTypes.TIMESTAMP ) );
        registerFunction( "subdate", new StandardSQLFunction( "subdate", StandardBasicTypes.TIMESTAMP ) );
        registerFunction( "timeofday", new StandardSQLFunction( "timeofday", StandardBasicTypes.STRING ) );
        registerFunction( "timestamp_cmp", new StandardSQLFunction( "timestamp_cmp", StandardBasicTypes.INTEGER ) );
        registerFunction( "to_date", new StandardSQLFunction( "to_date", StandardBasicTypes.STRING ) );
        registerFunction( "to_timestamp", new StandardSQLFunction( "to_timestamp", StandardBasicTypes.TIMESTAMP ) );
        registerFunction( "to_utc_timestamp", new StandardSQLFunction( "to_utc_timestamp", StandardBasicTypes.TIMESTAMP ) );
        registerFunction( "unix_timestamp", new StandardSQLFunction( "unix_timestamp", StandardBasicTypes.INTEGER ) );
        registerFunction( "utc_timestamp", new StandardSQLFunction( "utc_timestamp", StandardBasicTypes.INTEGER ) );
        registerFunction( "weekofyear", new StandardSQLFunction( "weekofyear", StandardBasicTypes.INTEGER ) );
        registerFunction( "weeks_add", new StandardSQLFunction( "weeks_add", StandardBasicTypes.TIMESTAMP ) );
        registerFunction( "weeks_sub", new StandardSQLFunction( "weeks_sub", StandardBasicTypes.TIMESTAMP ) );
        registerFunction( "year", new StandardSQLFunction( "year", StandardBasicTypes.INTEGER ) );
        registerFunction( "years_add", new StandardSQLFunction( "years_add", StandardBasicTypes.TIMESTAMP ) );
        registerFunction( "years_sub", new StandardSQLFunction( "years_sub", StandardBasicTypes.TIMESTAMP ) );

        // Impala Conditional Functions
        registerFunction( "coalesce", new StandardSQLFunction( "coalesce" ) );
        registerFunction( "decode", new StandardSQLFunction( "decode" ) );
        registerFunction( "if", new StandardSQLFunction( "if" ) );
        registerFunction( "ifnull", new StandardSQLFunction( "ifnull" ) );
        registerFunction( "isfalse", new StandardSQLFunction( "isfalse", StandardBasicTypes.BOOLEAN ) );
        registerFunction( "nullvalue", new StandardSQLFunction( "nullvalue", StandardBasicTypes.BOOLEAN ) );
        registerFunction( "nonnullvalue", new StandardSQLFunction( "nonnullvalue", StandardBasicTypes.BOOLEAN ) );
        registerFunction( "istrue", new StandardSQLFunction( "istrue", StandardBasicTypes.BOOLEAN ) );
        registerFunction( "isnotfalse", new StandardSQLFunction( "isnotfalse", StandardBasicTypes.BOOLEAN ) );
        registerFunction( "isnottrue", new StandardSQLFunction( "isnottrue", StandardBasicTypes.BOOLEAN ) );
        registerFunction( "zeroifnull", new StandardSQLFunction( "zeroifnull" ) );
        registerFunction( "nvl2", new StandardSQLFunction( "nvl2" ) );
        registerFunction( "nvl", new StandardSQLFunction( "nvl" ) );
        registerFunction( "nullifzero", new StandardSQLFunction( "nullifzero" ) );
        registerFunction( "nullif", new StandardSQLFunction( "nullif" ) );
        registerFunction( "isnull", new StandardSQLFunction( "isnull" ) );

        // Impala String Functions
        registerFunction( "ascii", new StandardSQLFunction( "ascii", StandardBasicTypes.INTEGER ) );
        registerFunction( "base64encode", new StandardSQLFunction( "base64encode", StandardBasicTypes.STRING ) );
        registerFunction( "base64decode", new StandardSQLFunction( "base64decode", StandardBasicTypes.STRING ) );
        registerFunction( "left", new StandardSQLFunction( "left", StandardBasicTypes.STRING ) );
        registerFunction( "initcap", new StandardSQLFunction( "initcap", StandardBasicTypes.STRING ) );
        registerFunction( "group_concat", new StandardSQLFunction( "group_concat", StandardBasicTypes.STRING ) );
        registerFunction( "concat_ws", new StandardSQLFunction( "concat_ws", StandardBasicTypes.STRING ) );
        registerFunction( "concat", new StandardSQLFunction( "concat", StandardBasicTypes.STRING ) );
        registerFunction( "chr", new StandardSQLFunction( "chr", StandardBasicTypes.STRING ) );
        registerFunction( "btrim", new StandardSQLFunction( "btrim", StandardBasicTypes.STRING ) );
        registerFunction( "instr", new StandardSQLFunction( "instr", StandardBasicTypes.INTEGER ) );
        registerFunction( "find_in_set", new StandardSQLFunction( "find_in_set", StandardBasicTypes.INTEGER ) );
        registerFunction( "char_length", new StandardSQLFunction( "char_length", StandardBasicTypes.INTEGER ) );
        registerFunction( "levenshtein", new StandardSQLFunction( "levenshtein", StandardBasicTypes.INTEGER ) );
        registerFunction( "length", new StandardSQLFunction( "length", StandardBasicTypes.INTEGER ) );
        registerFunction( "locate", new StandardSQLFunction( "locate", StandardBasicTypes.INTEGER ) );
        registerFunction( "lcase", new StandardSQLFunction( "lcase", StandardBasicTypes.STRING ) );
        registerFunction( "lower", new StandardSQLFunction( "lower", StandardBasicTypes.STRING ) );
        registerFunction( "right", new StandardSQLFunction( "right", StandardBasicTypes.STRING ) );
        registerFunction( "reverse", new StandardSQLFunction( "reverse", StandardBasicTypes.STRING ) );
        registerFunction( "replace", new StandardSQLFunction( "replace", StandardBasicTypes.STRING ) );
        registerFunction( "repeat", new StandardSQLFunction( "repeat", StandardBasicTypes.STRING ) );
        registerFunction( "regexp_replace", new StandardSQLFunction( "regexp_replace", StandardBasicTypes.STRING ) );
        registerFunction( "regexp_extract", new StandardSQLFunction( "regexp_extract", StandardBasicTypes.STRING ) );
        registerFunction( "regexp_escape", new StandardSQLFunction( "regexp_escape", StandardBasicTypes.STRING ) );
        registerFunction( "parse_url", new StandardSQLFunction( "parse_url", StandardBasicTypes.STRING ) );
        registerFunction( "ltrim", new StandardSQLFunction( "ltrim", StandardBasicTypes.STRING ) );
        registerFunction( "lpad", new StandardSQLFunction( "lpad", StandardBasicTypes.STRING ) );
        registerFunction( "regexp_like", new StandardSQLFunction( "regexp_like", StandardBasicTypes.BOOLEAN ) );
        registerFunction( "ucase", new StandardSQLFunction( "ucase", StandardBasicTypes.STRING ) );
        registerFunction( "upper", new StandardSQLFunction( "upper", StandardBasicTypes.STRING ) );
        registerFunction( "trim", new StandardSQLFunction( "trim", StandardBasicTypes.STRING ) );
        registerFunction( "translate", new StandardSQLFunction( "translate", StandardBasicTypes.STRING ) );
        registerFunction( "substring", new StandardSQLFunction( "substring", StandardBasicTypes.STRING ) );
        registerFunction( "substr", new StandardSQLFunction( "substr", StandardBasicTypes.STRING ) );
        registerFunction( "strright", new StandardSQLFunction( "strright", StandardBasicTypes.STRING ) );
        registerFunction( "strleft", new StandardSQLFunction( "strleft", StandardBasicTypes.STRING ) );
        registerFunction( "split_part", new StandardSQLFunction( "split_part", StandardBasicTypes.STRING ) );
        registerFunction( "space", new StandardSQLFunction( "space", StandardBasicTypes.STRING ) );
        registerFunction( "rtrim", new StandardSQLFunction( "rtrim", StandardBasicTypes.STRING ) );
        registerFunction( "rpad", new StandardSQLFunction( "rpad", StandardBasicTypes.STRING ) );

        // Impala Miscellaneous Functions
        registerFunction( "effective_user", new StandardSQLFunction( "effective_user", StandardBasicTypes.STRING ) );
        registerFunction( "current_database", new StandardSQLFunction( "current_database", StandardBasicTypes.STRING ) );
        registerFunction( "version", new StandardSQLFunction( "version", StandardBasicTypes.STRING ) );
        registerFunction( "uuid", new StandardSQLFunction( "uuid", StandardBasicTypes.STRING ) );
        registerFunction( "user", new StandardSQLFunction( "user", StandardBasicTypes.STRING ) );
        registerFunction( "sleep", new StandardSQLFunction( "sleep", StandardBasicTypes.STRING ) );
        registerFunction( "logged_in_user", new StandardSQLFunction( "logged_in_user", StandardBasicTypes.STRING ) );
        registerFunction( "pid", new StandardSQLFunction( "pid", StandardBasicTypes.INTEGER ) );

        // Impala Aggregate Functions
        registerFunction( "min", new StandardSQLFunction( "min" ) );
        registerFunction( "max", new StandardSQLFunction( "max" ) );
        registerFunction( "appx_median", new StandardSQLFunction( "appx_median" ) );
        registerFunction( "sum", new StandardSQLFunction( "sum", StandardBasicTypes.DOUBLE ) );
        registerFunction( "stddev_samp", new StandardSQLFunction( "stddev_samp", StandardBasicTypes.DOUBLE ) );
        registerFunction( "stddev_pop", new StandardSQLFunction( "stddev_pop", StandardBasicTypes.DOUBLE ) );
        registerFunction( "stddev", new StandardSQLFunction( "stddev", StandardBasicTypes.DOUBLE ) );
        registerFunction( "ndv", new StandardSQLFunction( "ndv", StandardBasicTypes.DOUBLE ) );
        registerFunction( "avg", new StandardSQLFunction( "avg", StandardBasicTypes.DOUBLE ) );
        registerFunction( "count", new StandardSQLFunction( "count", StandardBasicTypes.BIG_INTEGER ) );
        registerFunction( "group_concat", new StandardSQLFunction( "group_concat", StandardBasicTypes.STRING ) );

        getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE );

        limitHandler = new ImpalaLimitHandler();
    }

    @Override
    public String getAddColumnString() {
        return "add column";
    }

    @Override
    public boolean supportsLockTimeouts() {
        return false;
    }

    @Override
    public String getForUpdateString() {
        return "";
    }

    @Override
    public LimitHandler getLimitHandler() {
        return limitHandler;
    }

    @Override
    public boolean supportsLimit() {
        return true;
    }

    @Override
    public String getLimitString(String sql, boolean hasOffset) {
        return sql + (hasOffset ? " limit ? offset ? " : " limit ?");
    }

    @Override
    public boolean bindLimitParametersFirst() {
        return true;
    }

    @Override
    public boolean supportsIfExistsAfterTableName() {
        return false;
    }

    @Override
    public boolean supportsIfExistsBeforeTableName() {
        return true;
    }

    @Override
    public boolean supportsColumnCheck() {
        return true;
    }

    @Override
    public boolean supportsSequences() {
        return true;
    }

    @Override
    public boolean supportsPooledSequences() {
        return true;
    }

    @Override
    protected String getCreateSequenceString(String sequenceName) {
        return "create sequence " + sequenceName + " start with 1";
    }

    @Override
    protected String getCreateSequenceString(String sequenceName, int initialValue, int incrementSize) throws MappingException {
        if ( supportsPooledSequences() ) {
            return "create sequence " + sequenceName + " start with " + initialValue + " increment by " + incrementSize;
        }
        throw new MappingException( getClass().getName() + " does not support pooled sequences" );
    }

    @Override
    public SequenceInformationExtractor getSequenceInformationExtractor() {
        return SequenceInformationExtractorHSQLDBDatabaseImpl.INSTANCE;
    }

    @Override
    public String getSelectClauseNullString(int sqlType) {
        String literal;
        switch ( sqlType ) {
            case Types.BIGINT:
                literal = "cast(null as bigint)";
                break;
            case Types.BOOLEAN:
                literal = "cast(null as boolean)";
                break;
            case Types.CHAR:
                literal = "cast(null as string)";
                break;
            case Types.DECIMAL:
                literal = "cast(null as decimal)";
                break;
            case Types.DOUBLE:
                literal = "cast(null as double)";
                break;
            case Types.FLOAT:
                literal = "cast(null as float)";
                break;
            case Types.INTEGER:
                literal = "cast(null as int)";
                break;
            case Types.SMALLINT:
                literal = "cast(null as smallint)";
                break;
            case Types.VARCHAR:
                literal = "cast(null as string)";
                break;
            case Types.BLOB:
                literal = "cast(null as string)";
                break;
            case Types.CLOB:
                literal = "cast(null as string)";
                break;
            case Types.TINYINT:
                literal = "cast(null as tinyint)";
                break;
            case Types.TIMESTAMP:
                literal = "cast(null as timestamp)";
                break;
            default:
                literal = "cast(null as string)";
        }
        return literal;
    }

    @Override
    public boolean supportsUnionAll() {
        return true;
    }

    @Override
    public boolean supportsCurrentTimestampSelection() {
        return true;
    }

    @Override
    public boolean isCurrentTimestampSelectStringCallable() {
        return false;
    }

    @Override
    public String getCurrentTimestampSelectString() {
        return "select current_timestamp()";
    }

    @Override
    public String getCurrentTimestampSQLFunctionName() {
        return "current_timestamp";
    }

    @Override
    public boolean supportsCommentOn() {
        return true;
    }

    // Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    @Override
    public boolean supportsEmptyInList() {
        return false;
    }

    @Override
    public boolean requiresCastingOfParametersInSelectClause() {
        return true;
    }

    @Override
    public boolean doesReadCommittedCauseWritersToBlockReaders() {
        return true;
    }

    @Override
    public boolean doesRepeatableReadCauseReadersToBlockWriters() {
        return true;
    }

    @Override
    public boolean supportsLobValueChangePropogation() {
        return false;
    }

    @Override
    public String toBooleanValueString(boolean bool) {
        return String.valueOf( bool );
    }

    @Override
    public NameQualifierSupport getNameQualifierSupport() {
        return NameQualifierSupport.SCHEMA;
    }

    @Override
    public boolean supportsNamedParameters(DatabaseMetaData databaseMetaData) throws SQLException {
        return false;
    }

    @Override
    public boolean dropConstraints() {
        return false;
    }

    @Override
    public String getCascadeConstraintsString() {
        return " CASCADE ";
    }
}
