/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Locale;

import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.InformixExtractEmulation;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.InformixIdentityColumnSupport;
import org.hibernate.dialect.pagination.FirstLimitHandler;
import org.hibernate.dialect.pagination.Informix10LimitHandler;
import org.hibernate.dialect.pagination.LegacyFirstLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.unique.InformixUniqueDelegate;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtracter;
import org.hibernate.exception.spi.ViolatedConstraintNameExtracter;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.mutation.spi.idtable.StandardIdTableSupport;
import org.hibernate.query.sqm.mutation.spi.SqmMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.idtable.LocalTempTableExporter;
import org.hibernate.query.sqm.mutation.spi.idtable.LocalTemporaryTableStrategy;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorInformixDatabaseImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * Dialect for Informix 7.31.UD3 with Informix
 * JDBC driver 2.21JC3 and above.
 *
 * @author Steve Molitor
 */
public class InformixDialect extends Dialect {

	private final int version;

	int getVersion() {
		return version;
	}

	public InformixDialect(DialectResolutionInfo info) {
		this( info.getDatabaseMajorVersion() );
	}

	private final UniqueDelegate uniqueDelegate;

	public InformixDialect() {
		this(7);
	}

	/**
	 * Creates new <code>InformixDialect</code> instance. Sets up the JDBC /
	 * Informix type mappings.
	 */
	public InformixDialect(int version) {
		super();
		this.version = version;

		// Informix doesn't have a bit type
		registerColumnType( Types.BIT, 1, "boolean" );
		registerColumnType( Types.BIT, "smallint" );
		registerColumnType( Types.BOOLEAN, "smallint" );

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
		registerColumnType( Types.LONGVARBINARY, "blob" );

		registerColumnType( Types.VARCHAR, "varchar($l)" );
		registerColumnType( Types.VARCHAR, 255, "varchar($l)" );
		registerColumnType( Types.VARCHAR, 32739, "lvarchar($l)" );
		registerColumnType( Types.LONGVARCHAR, "clob" );

		uniqueDelegate = new InformixUniqueDelegate( this );
	}

	@Override
	public int getPreferredSqlTypeCodeForBoolean() {
		return Types.BOOLEAN;
	}

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
				StandardSpiBasicTypes.INTEGER,
				"instr(?2, ?1)",
				"instr(?2, ?1, ?3)"
		).setArgumentListSignature("(pattern, string[, start])");

		//coalesce() and nullif() both supported since Informix 12
		queryEngine.getSqmFunctionRegistry().register( "extract", new InformixExtractEmulation() );
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
	public String getSequenceNextValString(String sequenceName) {
		return "select " + getSelectSequenceNextValString( sequenceName ) + " from informix.systables where tabid=1";
	}

	@Override
	public String getSelectSequenceNextValString(String sequenceName) {
		return sequenceName + ".nextval";
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
	public String getQuerySequencesString() {
		return "select systables.tabname as sequence_name, syssequences.* from syssequences join systables on syssequences.tabid = systables.tabid where tabtype = 'Q'";
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SequenceInformationExtractorInformixDatabaseImpl.INSTANCE;
	}

	@Override
	public LimitHandler getLimitHandler() {
		if ( getVersion() >= 10 ) {
			// Since version 10.00.xC3 Informix has limit/offset
 			// support which was introduced in July 2005.
			return Informix10LimitHandler.INSTANCE;
		}
		else if ( isLegacyLimitHandlerBehaviorEnabled() ) {
			return LegacyFirstLimitHandler.INSTANCE;
		}
		else {
			return FirstLimitHandler.INSTANCE;
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean supportsLimit() {
		return true;
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean useMaxForLimit() {
		return true;
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean supportsLimitOffset() {
		return false;
	}

	@Override
	@SuppressWarnings("deprecation")
	public String getLimitString(String querySelect, int offset, int limit) {
		if ( offset > 0 ) {
			throw new UnsupportedOperationException( "query result offset is not supported" );
		}
		return new StringBuilder( querySelect.length() + 8 )
				.append( querySelect )
				.insert( querySelect.toLowerCase(Locale.ROOT).indexOf( "select" ) + 6, " first " + limit )
				.toString();
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean supportsVariableLimit() {
		return false;
	}

	@Override
	public String getFromDual() {
		return "from (select 0 from systables where tabid = 1) as dual";
	}

	@Override
	public ViolatedConstraintNameExtracter getViolatedConstraintNameExtracter() {
		return EXTRACTER;
	}

	private static final ViolatedConstraintNameExtracter EXTRACTER = new TemplatedViolatedConstraintNameExtracter() {
		@Override
		protected String doExtractConstraintName(SQLException sqle) throws NumberFormatException {
			String constraintName = null;
			final int errorCode = JdbcExceptionHelper.extractErrorCode( sqle );

			switch (errorCode) {
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
			}

			if ( constraintName != null ) {
				// strip table-owner because Informix always returns constraint names as "<table-owner>.<constraint-name>"
				final int i = constraintName.indexOf( '.' );
				if ( i != -1 ) {
					constraintName = constraintName.substring( i + 1 );
				}
			}

			return constraintName;
		}

	};

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
	public SqmMutationStrategy getDefaultIdTableStrategy() {
		return new LocalTemporaryTableStrategy(
				new StandardIdTableSupport(
						new LocalTempTableExporter() {
							@Override
							protected String getCreateCommand() {
								return "create temp table";
							}

							@Override
							protected String getCreateOptions() {
								return "with no log";
							}
						}
				)
		);
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
	public String toBooleanValueString(boolean bool) {
		return bool ? "'t'" : "'f'";
	}

	@Override
	public String translateDatetimeFormat(String format) {
		//Informix' own variation of MySQL
		return datetimeFormat( format ).result();
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
