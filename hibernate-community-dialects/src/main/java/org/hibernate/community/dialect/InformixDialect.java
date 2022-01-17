/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.Replacer;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.CaseLeastGreatestEmulation;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.community.dialect.identity.InformixIdentityColumnSupport;
import org.hibernate.community.dialect.pagination.FirstLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.community.dialect.pagination.SkipFirstLimitHandler;
import org.hibernate.community.dialect.sequence.InformixSequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.community.dialect.unique.InformixUniqueDelegate;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.query.TemporalUnit;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.internal.temptable.AfterUseAction;
import org.hibernate.dialect.temptable.TemporaryTable;
import org.hibernate.query.sqm.mutation.internal.temptable.BeforeUseAction;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableMutationStrategy;
import org.hibernate.dialect.temptable.TemporaryTableKind;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.query.sqm.sql.SqmTranslator;
import org.hibernate.query.sqm.sql.SqmTranslatorFactory;
import org.hibernate.query.sqm.sql.StandardSqmTranslatorFactory;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.community.dialect.sequence.SequenceInformationExtractorInformixDatabaseImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.type.StandardBasicTypes;

import java.sql.Types;

import static org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor.extractUsingTemplate;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.INTEGER;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.STRING;

/**
 * Dialect for Informix 7.31.UD3 with Informix
 * JDBC driver 2.21JC3 and above.
 *
 * @author Steve Molitor
 */
public class InformixDialect extends Dialect {

	private final DatabaseVersion version;
	private final UniqueDelegate uniqueDelegate;
	private final LimitHandler limitHandler;

	public InformixDialect(DialectResolutionInfo info) {
		this( info.makeCopy() );
		registerKeywords( info );
	}

	public InformixDialect() {
		this( DatabaseVersion.make( 7, 0 ) );
	}

	/**
	 * Creates new <code>InformixDialect</code> instance. Sets up the JDBC /
	 * Informix type mappings.
	 */
	public InformixDialect(DatabaseVersion version) {
		super();
		this.version = version;

		registerColumnType( Types.TINYINT, "smallint" );
		registerColumnType( Types.BIGINT, "int8" );

		//Ingres ignores the precision argument in
		//float(n) and just always defaults to
		//double precision.
		//TODO: return 'smallfloat' when n <= 24

		registerColumnType( Types.TIME, "datetime hour to second" );
		registerColumnType( Types.TIMESTAMP, "datetime year to fraction($p)" );
		registerColumnType( Types.TIMESTAMP_WITH_TIMEZONE, "datetime year to fraction($p)" );

		//these types have no defined length
		registerColumnType( Types.BINARY, "byte" );
		registerColumnType( Types.VARBINARY, "byte" );

		registerColumnType( Types.VARCHAR, 255, "varchar($l)" );
		registerColumnType( Types.VARCHAR, getMaxVarcharLength(), "lvarchar($l)" );
		registerColumnType( Types.VARCHAR, "text" );

		uniqueDelegate = new InformixUniqueDelegate( this );

		limitHandler = getVersion().isBefore( 10 )
				? FirstLimitHandler.INSTANCE
				//according to the Informix documentation for
				//version 11 and above, parameters are supported
				//but I have not tested this at all!
				: new SkipFirstLimitHandler( getVersion().isSameOrAfter( 11 ) );
	}

	@Override
	public int getMaxVarbinaryLength() {
		//there's no varbinary type, only byte
		return -1;
	}

	@Override
	public int getMaxVarcharLength() {
		//the maximum length of an lvarchar
		return 32_739;
	}

	@Override
	public DatabaseVersion getVersion() {
		return version;
	}

	@Override
	public int getDefaultDecimalPrecision() {
		//the maximum
		return 32;
	}

	@Override
	public int getDefaultTimestampPrecision() {
		//the maximum
		return 5;
	}

	@Override
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry( queryEngine );

		CommonFunctionFactory.instr( queryEngine );
		CommonFunctionFactory.substr( queryEngine );
		CommonFunctionFactory.substring_substr( queryEngine );
		//also natively supports ANSI-style substring()
		CommonFunctionFactory.trunc( queryEngine );
		CommonFunctionFactory.trim2( queryEngine );
		CommonFunctionFactory.space( queryEngine );
		CommonFunctionFactory.reverse( queryEngine );
		CommonFunctionFactory.octetLength( queryEngine );
		CommonFunctionFactory.degrees( queryEngine );
		CommonFunctionFactory.radians( queryEngine );
		CommonFunctionFactory.sinh( queryEngine );
		CommonFunctionFactory.tanh( queryEngine );
		CommonFunctionFactory.cosh( queryEngine );
		CommonFunctionFactory.moreHyperbolic( queryEngine );
		CommonFunctionFactory.log10( queryEngine );
		CommonFunctionFactory.initcap( queryEngine );
		CommonFunctionFactory.yearMonthDay( queryEngine );
		CommonFunctionFactory.ceiling_ceil( queryEngine );
		CommonFunctionFactory.concat_pipeOperator( queryEngine );
		CommonFunctionFactory.ascii( queryEngine );
		CommonFunctionFactory.char_chr( queryEngine );
		CommonFunctionFactory.addMonths( queryEngine );
		CommonFunctionFactory.monthsBetween( queryEngine );
		CommonFunctionFactory.stddev( queryEngine );
		CommonFunctionFactory.variance( queryEngine );

		queryEngine.getSqmFunctionRegistry().registerBinaryTernaryPattern(
				"locate",
				queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER ),
				"instr(?1,?2)",
				"instr(?1,?2,?3)",
				STRING, STRING, INTEGER
		).setArgumentListSignature("(string, pattern[, start])");

		//coalesce() and nullif() both supported since Informix 12

		queryEngine.getSqmFunctionRegistry().register( "least", new CaseLeastGreatestEmulation( true ) );
		queryEngine.getSqmFunctionRegistry().register( "greatest", new CaseLeastGreatestEmulation( false ) );
	}

	@Override
	public SqmTranslatorFactory getSqmTranslatorFactory() {
		return new StandardSqmTranslatorFactory() {
			@Override
			public SqmTranslator<SelectStatement> createSelectTranslator(
					SqmSelectStatement<?> sqmSelectStatement,
					QueryOptions queryOptions,
					DomainParameterXref domainParameterXref,
					QueryParameterBindings domainParameterBindings,
					LoadQueryInfluencers loadQueryInfluencers,
					SqlAstCreationContext creationContext) {
				return new InformixSqmToSqlAstConverter<>(
						sqmSelectStatement,
						queryOptions,
						domainParameterXref,
						domainParameterBindings,
						loadQueryInfluencers,
						creationContext
				);
			}
		};
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement) {
				return new InformixSqlAstTranslator<>( sessionFactory, statement );
			}
		};
	}

	/**
	 * Informix has no extract() function, but we can
	 * partially emulate it by using the appropriate
	 * named functions, and by using to_char() with
	 * a format string.
	 *
	 * The supported fields are
	 * {@link TemporalUnit#HOUR},
	 * {@link TemporalUnit#MINUTE},
	 * {@link TemporalUnit#SECOND},
	 * {@link TemporalUnit#DAY},
	 * {@link TemporalUnit#MONTH},
	 * {@link TemporalUnit#YEAR},
	 * {@link TemporalUnit#QUARTER},
	 * {@link TemporalUnit#DAY_OF_MONTH},
	 * {@link TemporalUnit#DAY_OF_WEEK}.
	 */
	@Override
	public String extractPattern(TemporalUnit unit) {
		switch (unit) {
			case SECOND:
				return "to_number(to_char(?2,'%S'))";
			case MINUTE:
				return "to_number(to_char(?2,'%M'))";
			case HOUR:
				return "to_number(to_char(?2,'%H'))";
			case DAY_OF_WEEK:
				return "(weekday(?2)+1)";
			case DAY_OF_MONTH:
				return "day(?2)";
			default:
				//I think week() returns the ISO week number
				return "?1(?2)";
		}
	}

	@Override
	public String getAddColumnString() {
		return "add";
	}

	/**
	 * Informix constraint name must be at the end.
	 * <p/>
	 * {@inheritDoc}
	 */
	@Override
	public String getAddForeignKeyConstraintString(
			String constraintName,
			String[] foreignKey,
			String referencedTable,
			String[] primaryKey,
			boolean referencesPrimaryKey) {
		final StringBuilder result = new StringBuilder( 30 )
				.append( " add constraint " )
				.append( " foreign key (" )
				.append( String.join( ", ", foreignKey ) )
				.append( ") references " )
				.append( referencedTable );

		if ( !referencesPrimaryKey ) {
			result.append( " (" )
					.append( String.join( ", ", primaryKey ) )
					.append( ')' );
		}

		result.append( " constraint " ).append( constraintName );

		return result.toString();
	}

	public String getAddForeignKeyConstraintString(
			String constraintName,
			String foreignKeyDefinition) {
		return " add constraint " + foreignKeyDefinition
				+ " constraint " + constraintName;
	}

	/**
	 * Informix constraint name must be at the end.
	 * <p/>
	 * {@inheritDoc}
	 */
	@Override
	public String getAddPrimaryKeyConstraintString(String constraintName) {
		return " add constraint primary key constraint " + constraintName + " ";
	}

	@Override
	public SequenceSupport getSequenceSupport() {
		return InformixSequenceSupport.INSTANCE;
	}

	@Override
	public String getQuerySequencesString() {
		return "select systables.tabname as sequence_name,syssequences.* from syssequences join systables on syssequences.tabid=systables.tabid where tabtype='Q'";
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SequenceInformationExtractorInformixDatabaseImpl.INSTANCE;
	}

	@Override
	public LimitHandler getLimitHandler() {
		return limitHandler;
	}

	@Override
	public boolean supportsOrderByInSubquery() {
		// This is just a guess
		return false;
	}

	@Override
	public boolean supportsWindowFunctions() {
		return getVersion().isSameOrAfter( 12, 10 );
	}

	@Override
	public boolean supportsLateral() {
		return getVersion().isSameOrAfter( 12, 10 );
	}

	@Override
	public ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor() {
		return EXTRACTOR;
	}

	private static final ViolatedConstraintNameExtractor EXTRACTOR =
			new TemplatedViolatedConstraintNameExtractor( sqle -> {
				String constraintName;
				switch ( JdbcExceptionHelper.extractErrorCode( sqle ) ) {
					case -268:
						constraintName = extractUsingTemplate(
								"Unique constraint (",
								") violated.",
								sqle.getMessage()
						);
						break;
					case -691:
						constraintName = extractUsingTemplate(
								"Missing key in referenced table for referential constraint (",
								").",
								sqle.getMessage()
						);
						break;
					case -692:
						constraintName = extractUsingTemplate(
								"Key value for constraint (",
								") is still being referenced.",
								sqle.getMessage()
						);
						break;
					default:
						return null;
				}

				// strip table-owner because Informix always returns constraint names as "<table-owner>.<constraint-name>"
				final int i = constraintName.indexOf( '.' );
				if ( i != -1 ) {
					constraintName = constraintName.substring( i + 1 );
				}
				return constraintName;
			} );

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
		return "select distinct current timestamp from informix.systables";
	}

	@Override
	public SqmMultiTableMutationStrategy getFallbackSqmMutationStrategy(
			EntityMappingType rootEntityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		return new LocalTemporaryTableMutationStrategy(
				TemporaryTable.createIdTable(
						rootEntityDescriptor,
						basename -> TemporaryTable.ID_TABLE_PREFIX + basename,
						this,
						runtimeModelCreationContext
				),
				runtimeModelCreationContext.getSessionFactory()
		);
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
		return TemporaryTableKind.LOCAL;
	}

	@Override
	public String getTemporaryTableCreateOptions() {
		return "with no log";
	}

	@Override
	public String getTemporaryTableCreateCommand() {
		return "create temp table";
	}

	@Override
	public AfterUseAction getTemporaryTableAfterUseAction() {
		return AfterUseAction.NONE;
	}

	@Override
	public BeforeUseAction getTemporaryTableBeforeUseAction() {
		return BeforeUseAction.CREATE;
	}

	@Override
	public UniqueDelegate getUniqueDelegate() {
		return uniqueDelegate;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new InformixIdentityColumnSupport();
	}

	@Override
	public void appendBooleanValueString(SqlAppender appender, boolean bool) {
		appender.appendSql( bool ? "'t'" : "'f'" );
	}

	@Override
	public String currentDate() {
		return "today";
	}

	@Override
	public String currentTimestamp() {
		return "current";
	}

	@Override
	public void appendDatetimeFormat(SqlAppender appender, String format) {
		//Informix' own variation of MySQL
		appender.appendSql( datetimeFormat( format ).result() );
	}

	public static Replacer datetimeFormat(String format) {
		return new Replacer( format, "'", "" )
				.replace("%", "%%")

				//year
				.replace("yyyy", "%Y")
				.replace("yyy", "%Y")
				.replace("yy", "%y")
				.replace("y", "Y")

				//month of year
				.replace("MMMM", "%B")
				.replace("MMM", "%b")
				.replace("MM", "%m")
				.replace("M", "%c") //????

				//day of week
				.replace("EEEE", "%A")
				.replace("EEE", "%a")
				.replace("ee", "%w")
				.replace("e", "%w")

				//day of month
				.replace("dd", "%d")
				.replace("d", "%e")

				//am pm
				.replace("aa", "%p") //?????
				.replace("a", "%p") //?????

				//hour
				.replace("hh", "%I")
				.replace("HH", "%H")
				.replace("h", "%I")
				.replace("H", "%H")

				//minute
				.replace("mm", "%M")
				.replace("m", "%M")

				//second
				.replace("ss", "%S")
				.replace("s", "%S")

				//fractional seconds
				.replace("SSSSSS", "%F50") //5 is the max
				.replace("SSSSS", "%F5")
				.replace("SSSS", "%F4")
				.replace("SSS", "%F3")
				.replace("SS", "%F2")
				.replace("S", "%F1");
	}

}
