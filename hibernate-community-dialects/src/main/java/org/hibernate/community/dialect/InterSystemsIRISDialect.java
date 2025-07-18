/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect;

import org.hibernate.LockMode;
import org.hibernate.ScrollMode;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.NationalizationSupport;
import org.hibernate.dialect.function.ExtractFunction;
import org.hibernate.dialect.pagination.InterSystemsIRISLimitHandler;
import org.hibernate.engine.jdbc.env.spi.*;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.query.sqm.FetchClauseType;
import org.hibernate.query.sqm.mutation.internal.temptable.*;
import org.hibernate.query.sqm.produce.function.FunctionParameterType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.type.*;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;

import java.sql.*;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.InterSystemsIRISIdentityColumnSupport;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.lock.OptimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.OptimisticLockingStrategy;
import org.hibernate.dialect.lock.PessimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.PessimisticReadSelectLockingStrategy;
import org.hibernate.dialect.lock.PessimisticReadUpdateLockingStrategy;
import org.hibernate.dialect.lock.PessimisticWriteSelectLockingStrategy;
import org.hibernate.dialect.lock.PessimisticWriteUpdateLockingStrategy;
import org.hibernate.dialect.lock.SelectLockingStrategy;
import org.hibernate.dialect.lock.UpdateLockingStrategy;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.exception.internal.InterSystemsIRISSQLExceptionConversionDelegate;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.*;
import static org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers.useArgType;

import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.dialect.temptable.TemporaryTable;
import org.hibernate.dialect.temptable.TemporaryTableKind;
import org.hibernate.service.ServiceRegistry;

import static org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor.extractUsingTemplate;
import static org.hibernate.type.SqlTypes.TIMESTAMP_UTC;

import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.type.descriptor.sql.internal.CapacityDependentDdlType;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.query.sqm.mutation.internal.temptable.AfterUseAction;
import org.hibernate.query.sqm.mutation.internal.temptable.BeforeUseAction;

/**
 * A Hibernate dialect for InterSystems IRIS
 * intended for  Hibernate 6.6+  and jdk 1.8+
 *
 * @author Wei Xia
 */
public class InterSystemsIRISDialect extends Dialect {


    private BasicType<Date> dateType;
    private BasicType<String> stringType;
    private BasicType<Date> timestampType;
    private BasicType<Double> doubleType;
    private BasicType<Integer> integerType;
    private BasicType<Date> timeType;
    private SqmFunctionRegistry functionRegistry;
    private LimitHandler limitHandler = InterSystemsIRISLimitHandler.INSTANCE;


    public InterSystemsIRISDialect() {
        this((DatabaseVersion) null);
    }


    public InterSystemsIRISDialect(DatabaseVersion version) {
        super(version);
        registerDefaultKeywords();

    }


    public InterSystemsIRISDialect(DialectResolutionInfo info) {
        super(info);
        registerDefaultKeywords();

    }

    /**
     * Register SQL Functions
     */
    @Override
    public void initializeFunctionRegistry(FunctionContributions functionContributions) {
        super.initializeFunctionRegistry(functionContributions);
        final TypeConfiguration typeConfiguration = functionContributions.getTypeConfiguration();
        functionRegistry = functionContributions.getFunctionRegistry();
        CommonFunctionFactory functionFactory = new CommonFunctionFactory(functionContributions);
        BasicTypeRegistry basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();
        dateType = basicTypeRegistry.resolve(StandardBasicTypes.DATE);
        stringType = basicTypeRegistry.resolve(StandardBasicTypes.STRING);
        timestampType = basicTypeRegistry.resolve(StandardBasicTypes.TIMESTAMP);
        doubleType = basicTypeRegistry.resolve(StandardBasicTypes.DOUBLE);
        integerType = basicTypeRegistry.resolve(StandardBasicTypes.INTEGER);
        timeType = basicTypeRegistry.resolve(StandardBasicTypes.TIME);


        functionFactory.math();
        functionFactory.trigonometry();
        functionFactory.ascii();
        functionFactory.bitLength_pattern("length(?1)*8");
        functionFactory.char_chr();
        functionFactory.chr_char();
        functionFactory.length_characterLength();
        functionFactory.cot();
        functionFactory.coalesce();
        functionFactory.concat_pipeOperator();
        functionRegistry.namedDescriptorBuilder("convert")
                .setExactArgumentCount(2)
                .setParameterTypes(ANY, ANY)
                .register();
        functionFactory.nowCurdateCurtime();
        functionRegistry.noArgsBuilder("database")
                .setInvariantType(stringType)
                .setUseParenthesesWhenNoArgs(true)
                .register();

        functionRegistry.namedDescriptorBuilder("dateadd")
                .setReturnTypeResolver(useArgType(3))
                .setExactArgumentCount(3)
                .setParameterTypes(ANY, NUMERIC, ANY)
                .setArgumentListSignature("(STRING string, NUMERIC unit, ANY datetime)")
                .register();


        functionRegistry.namedDescriptorBuilder("datediff")
                .setInvariantType(stringType)
                .setExactArgumentCount(3)
                .setParameterTypes(ANY, ANY, ANY)
                .setArgumentListSignature("(ANY value, ANY date, ANY date)")
                .register();


        functionRegistry.namedDescriptorBuilder("datename")
                .setInvariantType(stringType)
                .setExactArgumentCount(1)
                .setParameterTypes(ANY)
                .setArgumentListSignature("(ANY value)")
                .register();

        functionRegistry.namedDescriptorBuilder("datepart")
                .setExactArgumentCount(2)
                .setParameterTypes(ANY, ANY)
                .setArgumentListSignature("(ANY value, ANY value)")
                .register();

        functionRegistry.namedDescriptorBuilder("day")
                .setInvariantType(integerType)
                .setExactArgumentCount(1)
                .setParameterTypes(ANY)
                .register();

        functionRegistry.namedDescriptorBuilder("dayname")
                .setInvariantType(stringType)
                .setExactArgumentCount(1)
                .setParameterTypes(ANY)
                .register();

        functionRegistry.namedDescriptorBuilder("dayofmonth")
                .setInvariantType(integerType)
                .setExactArgumentCount(1)
                .setParameterTypes(ANY)
                .register();


        functionRegistry.namedDescriptorBuilder("dayofweek")
                .setInvariantType(integerType)
                .setExactArgumentCount(1)
                .setParameterTypes(ANY)
                .register();

        functionRegistry.namedDescriptorBuilder("dayofyear")
                .setInvariantType(integerType)
                .setExactArgumentCount(1)
                .setParameterTypes(ANY)
                .register();


        functionRegistry.namedDescriptorBuilder("%exact")
                .setInvariantType(integerType)
                .setExactArgumentCount(1)
                .setParameterTypes(ANY)
                .register();

        functionRegistry.namedDescriptorBuilder("%external")
                .setExactArgumentCount(1)
                .setParameterTypes(ANY)
                .register();

        functionRegistry.namedDescriptorBuilder("$extract")
                .setInvariantType(stringType)
                .setArgumentCountBetween(1, 3)
                .setParameterTypes(STRING, INTEGER)
                .register();


        functionRegistry.namedDescriptorBuilder("$find")
                .setArgumentCountBetween(2, 3)
                .setInvariantType(integerType)
                .setParameterTypes(STRING, STRING, INTEGER)
                .register();

        functionRegistry.namedDescriptorBuilder("getdate")
                .setArgumentCountBetween(0, 1)
                .setInvariantType(timestampType)
                .setParameterTypes(INTEGER)
                .setUseParenthesesWhenNoArgs(true)
                .setArgumentListSignature("([INTEGER precision])")
                .register();


        functionRegistry.namedDescriptorBuilder("ifnull")
                .setArgumentCountBetween(2, 3)
                .setParameterTypes(ANY, ANY, ANY)
                .register();

        functionRegistry.namedDescriptorBuilder("%internal")
                .setExactArgumentCount(1)
                .setParameterTypes(ANY)
                .register();

        functionRegistry.namedDescriptorBuilder("%isnull")
                .setExactArgumentCount(2)
                .setParameterTypes(ANY, ANY)
                .register();

        functionRegistry.namedDescriptorBuilder("%isnumeric")
                .setExactArgumentCount(1)
                .setParameterTypes(ANY)
                .setInvariantType(integerType)
                .register();


        functionRegistry.namedDescriptorBuilder("lcase")
                .setExactArgumentCount(1)
                .setParameterTypes(STRING)
                .setInvariantType(stringType)
                .register();

        functionRegistry.namedDescriptorBuilder("log")
                .setExactArgumentCount(1)

                .setReturnTypeResolver(useArgType(1))
                .register();


        functionRegistry.namedDescriptorBuilder("log10")
                .setExactArgumentCount(1)

                .setReturnTypeResolver(useArgType(1))
                .register();


        functionRegistry.namedDescriptorBuilder("lower")
                .setExactArgumentCount(1)
                .setParameterTypes(STRING)
                .setInvariantType(stringType)
                .register();

        functionRegistry.namedDescriptorBuilder("upper")
                .setExactArgumentCount(1)
                .setParameterTypes(STRING)
                .setInvariantType(stringType)
                .register();


        functionRegistry.namedDescriptorBuilder("nullif")
                .setExactArgumentCount(2)
                .setParameterTypes(ANY, ANY)
                .setReturnTypeResolver(useArgType(1))
                .register();

        functionRegistry.namedDescriptorBuilder("nvl")
                .setExactArgumentCount(2)
                .setParameterTypes(ANY, ANY)
                .setReturnTypeResolver(useArgType(1))
                .register();

        functionRegistry.namedDescriptorBuilder("round")
                .setReturnTypeResolver(useArgType(1))
                .setArgumentCountBetween(2, 3)
                .setParameterTypes(NUMERIC, INTEGER, BOOLEAN)
                .register();

        functionRegistry.namedDescriptorBuilder("truncate")
                .setExactArgumentCount(2)
                .setParameterTypes(NUMERIC, INTEGER)
                .setReturnTypeResolver(useArgType(1))
                .setArgumentListSignature("(NUMERIC number, INTEGER places)")
                .register();

        functionRegistry.namedDescriptorBuilder("string")
                .setInvariantType(stringType)
                .setMinArgumentCount(1)
                .setArgumentTypeResolver(
                        StandardFunctionArgumentTypeResolvers.impliedOrInvariant(typeConfiguration, STRING)
                )
                .setArgumentListSignature("(STRING(string1[,string2][,...][,stringN]))")
                .register();


        functionRegistry.namedDescriptorBuilder("to_number")
                .setExactArgumentCount(1)
                .setParameterTypes(STRING)
                .setArgumentListSignature("STRING string")
                .register();


        functionRegistry.namedDescriptorBuilder("to_char")
                .setArgumentCountBetween(1, 2)
                .setParameterTypes(ANY, STRING)
                .setInvariantType(stringType)
                .register();

        functionRegistry.namedDescriptorBuilder("to_date")
                .setArgumentCountBetween(1, 2)
                .setParameterTypes(STRING, STRING)
                .setInvariantType(dateType)
                .register();


        functionRegistry.namedDescriptorBuilder("%sqlstring")
                .setArgumentCountBetween(1, 2)
                .setParameterTypes(STRING, INTEGER)
                .setInvariantType(stringType)
                .register();

        functionRegistry.namedDescriptorBuilder("%sqlupper")
                .setArgumentCountBetween(1, 2)
                .setParameterTypes(STRING, INTEGER)
                .setInvariantType(stringType)
                .register();

        functionRegistry.namedDescriptorBuilder("$piece")
                .setArgumentCountBetween(1, 3)
                .setParameterTypes(STRING, STRING, INTEGER, INTEGER)
                .setInvariantType(stringType)
                .register();

        functionRegistry.namedDescriptorBuilder("%ODBCIN")
                .setExactArgumentCount(1)
                .setParameterTypes(ANY)
                .register();

        functionRegistry.namedDescriptorBuilder("%odbcout")
                .setExactArgumentCount(1)
                .setParameterTypes(ANY)
                .register();

        functionRegistry.namedDescriptorBuilder("timestampadd")
                .setExactArgumentCount(3)
                .setParameterTypes(ANY, INTEGER, TIME)
                .setInvariantType(timestampType)
                .register();


        functionRegistry.namedDescriptorBuilder("timestampdiff")
                .setExactArgumentCount(3)
                .setParameterTypes(ANY, TIME, TIME)
                .setInvariantType(integerType)
                .register();


        functionRegistry.namedDescriptorBuilder("ucase")
                .setExactArgumentCount(1)
                .setParameterTypes(STRING)
                .setInvariantType(stringType)
                .setArgumentListSignature("STRING string")
                .register();

        functionRegistry.namedDescriptorBuilder("user")
                .setInvariantType(timestampType)
                .setParameterTypes(INTEGER)
                .setUseParenthesesWhenNoArgs(true)
                .register();

        functionRegistry.namedDescriptorBuilder("xmlconcat")
                .setInvariantType(stringType)
                .setMinArgumentCount(1)
                .setArgumentTypeResolver(
                        StandardFunctionArgumentTypeResolvers.impliedOrInvariant(typeConfiguration, STRING)
                )
                .setArgumentListSignature("(STRING(string1[,string2][,...][,stringN]))")
                .register();


        functionRegistry.namedDescriptorBuilder("xmlelement")
                .setArgumentCountBetween(1, 2)
                .setParameterTypes(ANY)
                .register();

        functionRegistry.namedDescriptorBuilder("$list")
                .setArgumentCountBetween(1, 3)
                .setInvariantType(stringType)
                .register();

        functionRegistry.namedDescriptorBuilder("$listdata")
                .setArgumentCountBetween(1, 2)
                .register();

        functionRegistry.namedDescriptorBuilder("$listfind")
                .setArgumentCountBetween(2, 3)
                .register();

        functionRegistry.namedDescriptorBuilder("$listget")
                .setArgumentCountBetween(1, 3)
                .register();

        functionRegistry.namedDescriptorBuilder("$listlength")
                .setExactArgumentCount(1)
                .register();


        functionRegistry.namedDescriptorBuilder("current_timestamp")
                .setUseParenthesesWhenNoArgs(false)
                .setInvariantType(timestampType)
                .register();


        functionContributions.getFunctionRegistry().register(
                "extract",
                new ExtractFunction(this, typeConfiguration)
        );

        functionRegistry.namedDescriptorBuilder("current_time")
                .setUseParenthesesWhenNoArgs(false)
                .setInvariantType(timeType)
                .register();

        functionRegistry.namedDescriptorBuilder("current_date")
                .setUseParenthesesWhenNoArgs(false)
                .setInvariantType(dateType)
                .register();


        functionRegistry.namedDescriptorBuilder("char_length")
                .setExactArgumentCount(1)
                .setParameterTypes(ANY)
                .setInvariantType(integerType)
                .setArgumentListSignature("ANY any")
                .register();

        functionRegistry.registerBinaryTernaryPattern(
                "locate",
                integerType,
                "position(?1 in ?2)",
                "position(?1 in substring(?2 from ?3))",
                FunctionParameterType.STRING,
                FunctionParameterType.STRING,
                FunctionParameterType.INTEGER,
                typeConfiguration
        );

        functionRegistry.registerPattern(
                "Extract",
                "?1(?2)",
                integerType
        );

        functionFactory.leftRight();
        functionFactory.characterLength_len();
        functionFactory.hourMinuteSecond();
        functionFactory.yearMonthDay();
        functionFactory.daynameMonthname();
        functionFactory.nowCurdateCurtime();
        functionFactory.substr();
        functionFactory.substring();
        functionFactory.sysdate();
        functionFactory.yearMonthDay();
        functionFactory.weekQuarter();
        functionFactory.position();
        functionFactory.weekQuarter();
        functionFactory.repeat_replicate();
        functionFactory.trim1();
        functionFactory.pi();
        functionFactory.space();
        functionFactory.lowerUpper();


    }

    @Override
    protected void initDefaultProperties() {
        getDefaultProperties().setProperty(Environment.USE_SQL_COMMENTS, "false");
    }


    @Override
    protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
        super.registerColumnTypes(typeContributions, serviceRegistry);

        final DdlTypeRegistry ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();

        ddlTypeRegistry.addDescriptor(simpleSqlType(SqlTypes.BINARY));
        ddlTypeRegistry.addDescriptor(simpleSqlType(SqlTypes.BIGINT));
        ddlTypeRegistry.addDescriptor(simpleSqlType(SqlTypes.BIT));
        ddlTypeRegistry.addDescriptor(simpleSqlType(SqlTypes.CHAR));
        ddlTypeRegistry.addDescriptor(simpleSqlType(SqlTypes.DATE));
        ddlTypeRegistry.addDescriptor(simpleSqlType(SqlTypes.DECIMAL));
        ddlTypeRegistry.addDescriptor(simpleSqlType(SqlTypes.DOUBLE));
        ddlTypeRegistry.addDescriptor(simpleSqlType(SqlTypes.FLOAT));
        ddlTypeRegistry.addDescriptor(simpleSqlType(SqlTypes.INTEGER));
        ddlTypeRegistry.addDescriptor(simpleSqlType(SqlTypes.LONGVARBINARY));
        ddlTypeRegistry.addDescriptor(simpleSqlType(SqlTypes.LONGVARCHAR));
        ddlTypeRegistry.addDescriptor(simpleSqlType(SqlTypes.NUMERIC));
        ddlTypeRegistry.addDescriptor(simpleSqlType(SqlTypes.REAL));
        ddlTypeRegistry.addDescriptor(simpleSqlType(SqlTypes.SMALLINT));
        ddlTypeRegistry.addDescriptor(simpleSqlType(SqlTypes.TIMESTAMP));
        ddlTypeRegistry.addDescriptor(simpleSqlType(TIMESTAMP_UTC));
        ddlTypeRegistry.addDescriptor(simpleSqlType(SqlTypes.TIME));
        ddlTypeRegistry.addDescriptor(simpleSqlType(SqlTypes.TINYINT));
        ddlTypeRegistry.addDescriptor(
                sqlTypeBuilder(SqlTypes.VARBINARY, SqlTypes.LONGVARBINARY, SqlTypes.VARBINARY)
                        .withTypeCapacity(getMaxVarbinaryLength(), columnType(SqlTypes.VARBINARY))
                        .build()
        );
        ddlTypeRegistry.addDescriptor(
                sqlTypeBuilder(SqlTypes.VARCHAR, SqlTypes.LONGVARCHAR, SqlTypes.VARCHAR)
                        .withTypeCapacity(getMaxVarcharLength(), columnType(SqlTypes.VARCHAR))
                        .build()
        );

        ddlTypeRegistry.addDescriptor(
                sqlTypeBuilder(SqlTypes.NVARCHAR, SqlTypes.LONGVARCHAR, SqlTypes.VARCHAR)
                        .withTypeCapacity(getMaxVarcharLength(), columnType(SqlTypes.NVARCHAR))
                        .build()
        );
        ddlTypeRegistry.addDescriptor(simpleSqlType(SqlTypes.BLOB));
        ddlTypeRegistry.addDescriptor(simpleSqlType(SqlTypes.CLOB));
        ddlTypeRegistry.addDescriptor(simpleSqlType(SqlTypes.BOOLEAN));


    }

    private DdlTypeImpl simpleSqlType(int sqlTypeCode) {
        return new DdlTypeImpl(sqlTypeCode, columnType(sqlTypeCode), castType(sqlTypeCode), this);
    }

    private CapacityDependentDdlType.Builder sqlTypeBuilder(int sqlTypeCode, int biggestSqlTypeCode, int castTypeCode) {
        return CapacityDependentDdlType.builder(
                sqlTypeCode,
                columnType(biggestSqlTypeCode),
                castType(castTypeCode),
                this
        );
    }

    //sql type to column type mapping
    @Override
    protected String columnType(int sqlTypeCode) {
        switch (sqlTypeCode) {
            case SqlTypes.BOOLEAN:
                return "bit";
            case SqlTypes.TINYINT:
                return "tinyint";
            case SqlTypes.SMALLINT:
                return "smallint";
            case SqlTypes.BIT:
                return "bit";
            case SqlTypes.CHAR:
                return "char(1)";
            case SqlTypes.INTEGER:
                return "integer";
            case SqlTypes.BIGINT:
                return "BigInt";
            case SqlTypes.DOUBLE:
                return "double";
            case SqlTypes.FLOAT:
                return "float";
            case SqlTypes.REAL:
                return "real";
            case SqlTypes.NUMERIC:
                return "numeric($p,$s)";
            case SqlTypes.DECIMAL:
                //return "decimal";
                return "decimal($p,$s)";
            case SqlTypes.DATE:
                return "date";
            case SqlTypes.TIME:
                return "time";
            case SqlTypes.TIMESTAMP:
                return "timestamp";
            case TIMESTAMP_UTC:
                return "timestamp";
            case SqlTypes.VARCHAR:
                return "varchar($l)";
            case SqlTypes.NVARCHAR:
                return "varchar($l)";
            case SqlTypes.BINARY:
                return "varbinary($l)";
            case SqlTypes.VARBINARY:
                return "varbinary($l)";
            case SqlTypes.LONGVARBINARY:
                return "longvarbinary";
            case SqlTypes.LONGVARCHAR:
                return "longvarchar";
            case SqlTypes.BLOB:
                return "longvarbinary";
            case SqlTypes.CLOB:
                return "longvarchar";
        }
        return super.columnType(sqlTypeCode);
    }


    @Override
    public boolean supportsSubselectAsInPredicateLHS() {
        return false;
    }

    public boolean supportsSubqueryOnMutatingTable() {
        return false;
    }

    @Override
    public NationalizationSupport getNationalizationSupport() {
        return NationalizationSupport.IMPLICIT;
    }


    @Override
    public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
        return new StandardSqlAstTranslatorFactory() {
            @Override
            protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
                    SessionFactoryImplementor sessionFactory, Statement statement) {
                return new InterSystemsIRISqlAstTranslator<>(sessionFactory, statement);
            }
        };
    }


    @Override
    protected void registerDefaultKeywords() {
        for (String keyword : getIRISKeywords()) {
            registerKeyword(keyword);
        }
    }

    private Set<String> getIRISKeywords() {
        final Set<String> keywords = new HashSet<>();
        keywords.add("ADD");
        keywords.add("ALL");
        keywords.add("ALLOCATE");
        keywords.add("ALTER");
        keywords.add("AND");
        keywords.add("ANY");
        keywords.add("ARE");
        keywords.add("AS");
        keywords.add("ASC");
        keywords.add("ASSERTION");
        keywords.add("AT");
        keywords.add("AUTHORIZATION");
        keywords.add("AVG");
        keywords.add("BEGIN");
        keywords.add("BETWEEN");
        keywords.add("BIT");
        keywords.add("BIT_LENGTH");
        keywords.add("BOTH");
        keywords.add("BY");
        keywords.add("CASCADED");
        keywords.add("CASE");
        keywords.add("CAST");
        keywords.add("CHAR");
        keywords.add("CHARACTER");
        keywords.add("CHARACTER_LENGTH");
        keywords.add("CHAR_LENGTH");
        keywords.add("CHECK");
        keywords.add("CLOSE");
        keywords.add("COALESCE");
        keywords.add("COLLATE");
        keywords.add("COMMIT");
        keywords.add("CONNECT");
        keywords.add("CONNECTION");
        keywords.add("CONSTRAINT");
        keywords.add("CONSTRAINTS");
        keywords.add("CONTINUE");
        keywords.add("CONVERT");
        keywords.add("CORRESPONDING");
        keywords.add("COUNT");
        keywords.add("CREATE");
        keywords.add("CROSS");
        keywords.add("CURRENT");
        keywords.add("CURRENT_DATE");
        keywords.add("CURRENT_TIME");
        keywords.add("CURRENT_TIMESTAMP");
        keywords.add("CURRENT_USER");
        keywords.add("CURSOR");
        keywords.add("DATE");
        keywords.add("DEALLOCATE");
        keywords.add("DEC");
        keywords.add("DECIMAL");
        keywords.add("DECLARE");
        keywords.add("DEFAULT");
        keywords.add("DEFERRABLE");
        keywords.add("DEFERRED");
        keywords.add("DELETE");
        keywords.add("DESC");
        keywords.add("DESCRIBE");
        keywords.add("DESCRIPTOR");
        keywords.add("DIAGNOSTICS");
        keywords.add("DISCONNECT");
        keywords.add("DISTINCT");
        keywords.add("DOMAIN");
        keywords.add("DOUBLE");
        keywords.add("DROP");
        keywords.add("ELSE");
        keywords.add("END");
        keywords.add("ENDEXEC");
        keywords.add("ESCAPE");
        keywords.add("EXCEPT");
        keywords.add("EXCEPTION");
        keywords.add("EXEC");
        keywords.add("EXECUTE");
        keywords.add("EXISTS");
        keywords.add("EXTERNAL");
        keywords.add("EXTRACT");
        keywords.add("FALSE");
        keywords.add("FETCH");
        keywords.add("FIRST");
        keywords.add("FLOAT");
        keywords.add("FOR");
        keywords.add("FOREIGN");
        keywords.add("FOUND");
        keywords.add("FROM");
        keywords.add("FULL");
        keywords.add("GET");
        keywords.add("GLOBAL");
        keywords.add("GRANT");
        keywords.add("GROUP");
        keywords.add("HAVING");
        keywords.add("HOUR");
        keywords.add("IDENTITY");
        keywords.add("IMMEDIATE");
        keywords.add("IN");
        keywords.add("INDICATOR");
        keywords.add("INITIALLY");
        keywords.add("INNER");
        keywords.add("INPUT");
        keywords.add("INSENSITIVE");
        keywords.add("INSERT");
        keywords.add("INT");
        keywords.add("INTEGER");
        keywords.add("INTERSECT");
        keywords.add("INTERVAL");
        keywords.add("INTO");
        keywords.add("IS");
        keywords.add("ISOLATION");
        keywords.add("JOIN");
        keywords.add("LANGUAGE");
        keywords.add("LAST");
        keywords.add("LEADING");
        keywords.add("LEFT");
        keywords.add("LEVEL");
        keywords.add("LIKE");
        keywords.add("LOCAL");
        keywords.add("LOWER");
        keywords.add("MATCH");
        keywords.add("MAX");
        keywords.add("MIN");
        keywords.add("MINUTE");
        keywords.add("MODULE");
        keywords.add("NAMES");
        keywords.add("NATIONAL");
        keywords.add("NATURAL");
        keywords.add("NCHAR");
        keywords.add("NEXT");
        keywords.add("NO");
        keywords.add("NOT");
        keywords.add("NULL");
        keywords.add("NULLIF");
        keywords.add("NUMERIC");
        keywords.add("OCTET_LENGTH");
        keywords.add("OF");
        keywords.add("ON");
        keywords.add("ONLY");
        keywords.add("OPEN");
        keywords.add("OPTION");
        keywords.add("OR");
        keywords.add("OUTER");
        keywords.add("OUTPUT");
        keywords.add("OVERLAPS");
        keywords.add("PAD");
        keywords.add("PARTIAL");
        keywords.add("PREPARE");
        keywords.add("PRIOR");
        keywords.add("PRIVILEGES");
        keywords.add("PROCEDURE");
        keywords.add("PUBLIC");
        keywords.add("READ");
        keywords.add("REAL");
        keywords.add("REFERENCES");
        keywords.add("RELATIVE");
        keywords.add("RESTRICT");
        keywords.add("REVOKE");
        keywords.add("RIGHT");
        keywords.add("ROLLBACK");
        keywords.add("ROW");
        keywords.add("ROWS");
        keywords.add("SCHEMA");
        keywords.add("SCROLL");
        keywords.add("SECOND");
        keywords.add("SECTION");
        keywords.add("SELECT");
        keywords.add("SESSION_USER");
        keywords.add("SET");
        keywords.add("SHARD");
        keywords.add("SMALLINT");
        keywords.add("SOME");
        keywords.add("SPACE");
        keywords.add("SQLERROR");
        keywords.add("SQLSTATE");
        keywords.add("STATISTICS");
        keywords.add("SUBSTRING");
        keywords.add("SUM");
        keywords.add("SYSDATE");
        keywords.add("SYSTEM_USER");
        keywords.add("TABLE");
        keywords.add("TEMPORARY");
        keywords.add("THEN");
        keywords.add("TIME");
        keywords.add("TIMEZONE_HOUR");
        keywords.add("TIMEZONE_MINUTE");
        keywords.add("TO");
        keywords.add("TOP");
        keywords.add("TRAILING");
        keywords.add("TRANSLATION");
        keywords.add("TRIM");
        keywords.add("TRUE");
        keywords.add("UNION");
        keywords.add("UNIQUE");
        keywords.add("UPDATE");
        keywords.add("UPPER");
        keywords.add("USER");
        keywords.add("USING");
        keywords.add("VALUES");
        keywords.add("VARCHAR");
        keywords.add("VARYING");
        keywords.add("WHEN");
        keywords.add("WHENEVER");
        keywords.add("WHERE");
        keywords.add("WITH");
        keywords.add("WORK");
        keywords.add("WRITE");

        return Collections.unmodifiableSet(keywords);
    }


    // DDL support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    @Override
    public boolean hasAlterTable() {
        // Does this dialect support the ALTER TABLE syntax?
        return true;
    }

    @Override
    public boolean qualifyIndexName() {
        // Do we need to qualify index names with the schema name?
        return false;
    }


    @Override
    public String getAddColumnString() {
        // The syntax used to add a column to a table
        return " add column";
    }

    @Override
    public String getCascadeConstraintsString() {
        // Completely optional cascading drop clause.
        return "";
    }

    @Override
    public boolean dropConstraints() {
        // Do we need to drop constraints before dropping tables in this dialect?
        return true;
    }

    @Override
    public boolean supportsCascadeDelete() {
        return true;
    }

    @Override
    public boolean hasSelfReferentialForeignKeyBug() {
        return true;
    }


    @Override
    public SqmMultiTableMutationStrategy getFallbackSqmMutationStrategy(
            EntityMappingType rootEntityDescriptor,
            RuntimeModelCreationContext runtimeModelCreationContext) {

        TemporaryTable idTable = TemporaryTable.createIdTable(
                rootEntityDescriptor,
                basename -> TemporaryTable.ID_TABLE_PREFIX + basename,
                this,
                runtimeModelCreationContext
        );

        return new PersistentTableMutationStrategy(
                idTable,
                runtimeModelCreationContext.getSessionFactory()
        );

    }


    @Override
    public boolean canCreateSchema() {
        return false;
    }

    @Override
    public SqmMultiTableInsertStrategy getFallbackSqmInsertStrategy(
            EntityMappingType rootEntityDescriptor,
            RuntimeModelCreationContext runtimeModelCreationContext) {

        return new LocalTemporaryTableInsertStrategy(
                TemporaryTable.createEntityTable(
                        rootEntityDescriptor,
                        name -> TemporaryTable.ENTITY_TABLE_PREFIX + name,
                        this,
                        runtimeModelCreationContext
                ),
                runtimeModelCreationContext.getSessionFactory()
        );
    }


    @Override
    public TemporaryTableKind getSupportedTemporaryTableKind() {
        return TemporaryTableKind.GLOBAL;
    }

    @Override
    public String getTemporaryTableCreateCommand() {
        return "create table if not exists";
    }

    @Override
    public String getTemporaryTableDropCommand() {
        return "drop table";
    }

    @Override
    public AfterUseAction getTemporaryTableAfterUseAction() {
        return AfterUseAction.CLEAN;
    }

    @Override
    public BeforeUseAction getTemporaryTableBeforeUseAction() {
        return BeforeUseAction.CREATE;
    }


    // IDENTITY support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @Override
    public IdentityColumnSupport getIdentityColumnSupport() {
        return new InterSystemsIRISIdentityColumnSupport();
    }


    @Override
    public boolean supportsOuterJoinForUpdate() {
        return false;
    }

    @Override
    public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {

        // Just to make some tests happy, but InterSystems IRIS doesn't really support this.
        // need to use READ_COMMITTED as isolation level

        // InterSystems InterSystemsIRIS does not current support "SELECT ... FOR UPDATE" syntax...
        // Set your transaction mode to READ_COMMITTED before using
        if (lockMode == LockMode.PESSIMISTIC_FORCE_INCREMENT) {
            return new PessimisticForceIncrementLockingStrategy(lockable, lockMode);
        } else if (lockMode == LockMode.PESSIMISTIC_WRITE) {
            return lockable.isVersioned()
                    ? new PessimisticWriteUpdateLockingStrategy(lockable, lockMode)
                    : new PessimisticWriteSelectLockingStrategy(lockable, lockMode);
        } else if (lockMode == LockMode.PESSIMISTIC_READ) {
            return lockable.isVersioned()
                    ? new PessimisticReadUpdateLockingStrategy(lockable, lockMode)
                    : new PessimisticReadSelectLockingStrategy(lockable, lockMode);
        } else if (lockMode == LockMode.OPTIMISTIC) {
            return new OptimisticLockingStrategy(lockable, lockMode);
        } else if (lockMode == LockMode.OPTIMISTIC_FORCE_INCREMENT) {
            return new OptimisticForceIncrementLockingStrategy(lockable, lockMode);
        } else if (lockMode.greaterThan(LockMode.READ)) {
            return new UpdateLockingStrategy(lockable, lockMode);
        } else {
            return new SelectLockingStrategy(lockable, lockMode);
        }
    }


    // The syntax used to add a foreign key constraint to a table.
    @Override
    public String getAddForeignKeyConstraintString(
            String constraintName,
            String[] foreignKey,
            String referencedTable,
            String[] primaryKey,
            boolean referencesPrimaryKey) {
        final String cols = String.join(", ", foreignKey);
        final String referencedCols = String.join(", ", primaryKey);
        return String.format(
                " add constraint %s foreign key (%s) references %s (%s)",
                constraintName,
                cols,
                referencedTable,
                referencedCols
        );
    }

    @Override
    public String getDropForeignKeyString() {
        return " drop foreign key ";
    }


    // LIMIT support (also TOP) ~~~~~~~~~~~~~~~~~~~

    @Override
    public LimitHandler getLimitHandler() {
        return limitHandler;
    }


    // callable statement support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    @Override
    public int registerResultSetOutParameter(CallableStatement statement, int col) throws SQLException {
        return col;
    }

    @Override
    public ResultSet getResultSet(CallableStatement ps) throws SQLException {
        ps.execute();
        return (ResultSet) ps.getObject(1);
    }

    @Override
    public String getLowercaseFunction() {
        // The name of the SQL function that transforms a string to lowercase
        return "lower";
    }

    @Override
    public String getNullColumnString() {
        return "";
    }


    // miscellaneous support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    @Override
    public String getNoColumnsInsertString() {
        // The keyword used to insert a row without specifying
        // any column values
        return " default values";
    }

    @Override
    public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
        return new InterSystemsIRISSQLExceptionConversionDelegate(this);

    }

    @Override
    public ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor() {
        return EXTRACTOR;
    }

    /**
     * The InterSystemsIRIS ViolatedConstraintNameExtracter.
     */

    private static final ViolatedConstraintNameExtractor EXTRACTOR =
            new TemplatedViolatedConstraintNameExtractor(sqle -> {
                return extractUsingTemplate("(", ")", sqle.getMessage());
            });


    /**
     * ddl like ""value" integer null check ("value">=2 AND "value"<=10)" isn't supported
     */
    @Override
    public boolean supportsColumnCheck() {
        return false;
    }

    /**
     * select count(distinct a,b,c) from hasi
     * isn't supported ;)
     */
    @Override
    public boolean supportsTupleDistinctCounts() {
        return false;
    }

    @Override
    public ScrollMode defaultScrollMode() {
        return super.defaultScrollMode();
    }

    @Override
    public boolean supportsExistsInSelect() {
        return false;
    }

    @Override
    public IdentifierHelper buildIdentifierHelper(
            IdentifierHelperBuilder builder, DatabaseMetaData dbMetaData) throws SQLException {
        if (dbMetaData == null) {
            builder.setUnquotedCaseStrategy(IdentifierCaseStrategy.MIXED);
            builder.setQuotedCaseStrategy(IdentifierCaseStrategy.UPPER);
        } else {
            builder.applyIdentifierCasing(dbMetaData);
        }

        builder.applyReservedWords(getKeywords());
        builder.setNameQualifierSupport(getNameQualifierSupport());
        builder.setAutoQuoteKeywords(true);
        return super.buildIdentifierHelper(builder, dbMetaData);
    }


    @Override
    protected void registerKeyword(String word) {
        super.getKeywords().add(word);
    }

    @Override
    public boolean supportsNullPrecedence() {
        return false;
    }

    public boolean supportsLockTimeouts() {
        return false;
    }

    public boolean supportsFetchClause(FetchClauseType type) {
        return true;
    }

    @Override
    public boolean supportsUnboundedLobLocatorMaterialization() {
        return false;
    }

    @Override
    public boolean supportsExpectedLobUsagePattern() {
        return false;
    }

    @Override
    public boolean supportsLobValueChangePropagation() {
        return false;
    }

    @Override
    public boolean supportsCurrentTimestampSelection() {
        return true;
    }

    @Override
    public String getCurrentTimestampSelectString() {
        return "SELECT CURRENT_TIMESTAMP";
    }

    @Override
    public boolean isCurrentTimestampSelectStringCallable() {
        return false;
    }

    public boolean supportsValuesListForInsert() {
        return false;
    }

}
