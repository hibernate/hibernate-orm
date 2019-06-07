/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;

import org.hibernate.LockOptions;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.Teradata14IdentityColumnSupport;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtracter;
import org.hibernate.exception.spi.ViolatedConstraintNameExtracter;
import org.hibernate.metamodel.model.relational.spi.Index;
import org.hibernate.metamodel.model.relational.spi.PhysicalColumn;
import org.hibernate.naming.QualifiedNameImpl;
import org.hibernate.naming.QualifiedTableName;
import org.hibernate.query.TemporalUnit;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.mutation.spi.SqmMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.idtable.GlobalTempTableExporter;
import org.hibernate.query.sqm.mutation.spi.idtable.GlobalTemporaryTableStrategy;
import org.hibernate.query.sqm.mutation.spi.idtable.IdTable;
import org.hibernate.sql.ForUpdateFragment;
import org.hibernate.tool.schema.internal.StandardIndexExporter;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * A dialect for the Teradata database created by MCR as part of the
 * dialect certification process.
 *
 * @author Jay Nance
 */
public class TeradataDialect extends Dialect {

	int getVersion() {
		return 1200;
	}
	
	private static final int PARAM_LIST_SIZE_LIMIT = 1024;

	/**
	 * Constructor
	 */
	public TeradataDialect() {
		super();

		registerColumnType( Types.BOOLEAN, "byteint" );
		registerColumnType( Types.BIT, 1, "byteint" );
		registerColumnType( Types.BIT, "byteint" );

		registerColumnType( Types.TINYINT, "byteint" );

		registerColumnType( Types.BINARY, "byte($l)" );
		registerColumnType( Types.VARBINARY, "varbyte($l)" );
		registerColumnType( Types.LONGVARBINARY, "varbyte($l)" );

		if ( getVersion() < 1300 ) {
			registerColumnType( Types.BIGINT, "numeric(19,0)" );
		}
		else {
			//'bigint' has been there since at least version 13
			registerColumnType( Types.BIGINT, "bigint" );
		}

		registerKeyword( "password" );
		registerKeyword( "type" );
		registerKeyword( "title" );
		registerKeyword( "year" );
		registerKeyword( "month" );
		registerKeyword( "summary" );
		registerKeyword( "alias" );
		registerKeyword( "value" );
		registerKeyword( "first" );
		registerKeyword( "role" );
		registerKeyword( "account" );
		registerKeyword( "class" );

		if ( getVersion() < 1400 ) {
			// use getBytes instead of getBinaryStream
			getDefaultProperties().setProperty( Environment.USE_STREAMS_FOR_BINARY, "false" );
			// no batch statements
			getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, NO_BATCH );
		}
		else {
			getDefaultProperties().setProperty( Environment.USE_STREAMS_FOR_BINARY, "true" );
			getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE );
		}

	}

	@Override
	public int getDefaultDecimalPrecision() {
		return getVersion() < 1400 ? 18 : 38;
	}

	public void timestampdiff(TemporalUnit unit, Renderer from, Renderer to, Appender sqlAppender, boolean fromTimestamp, boolean toTimestamp) {
		//TODO: TOTALLY UNTESTED CODE!
		sqlAppender.append("cast((");
		to.render();
		sqlAppender.append(" - ");
		from.render();
		sqlAppender.append(") ");
		switch (unit) {
			case NANOSECOND:
				//default fractional precision is 6, the maximum
				sqlAppender.append("second");
				break;
			case WEEK:
				sqlAppender.append("day");
				break;
			case QUARTER:
				sqlAppender.append("month");
				break;
			default:
				sqlAppender.append( unit.toString() );
		}
		sqlAppender.append("(4)");
		sqlAppender.append(" as bigint)");
		switch (unit) {
			case WEEK:
				sqlAppender.append("/7");
				break;
			case QUARTER:
				sqlAppender.append("/3");
				break;
			case NANOSECOND:
				sqlAppender.append("*1e9");
				break;
		}
	}

	@Override
	public void timestampadd(TemporalUnit unit, Renderer magnitude, Renderer to, Appender sqlAppender, boolean timestamp) {
		//TODO: TOTALLY UNTESTED CODE!
		sqlAppender.append("(");
		to.render();
		boolean subtract = false;
//		if ( magnitude.startsWith("-") ) {
//			subtract = true;
//			magnitude = magnitude.substring(1);
//		}
		sqlAppender.append(subtract ? " - " : " + ");
		switch ( unit ) {
			case NANOSECOND:
				sqlAppender.append("(");
				magnitude.render();
				sqlAppender.append(")/1e9 * interval '1' second");
				break;
			case QUARTER:
				sqlAppender.append("(");
				magnitude.render();
				sqlAppender.append(") * interval '3' month");
				break;
			case WEEK:
				sqlAppender.append("(");
				magnitude.render();
				sqlAppender.append(") * interval '7' day");
				break;
			default:
//				if ( magnitude.matches("\\d+") ) {
//					sqlAppender.append("interval '");
//					sqlAppender.append( magnitude );
//					sqlAppender.append("'");
//				}
//				else {
					sqlAppender.append("(");
					magnitude.render();
					sqlAppender.append(") * interval '1'");
//				}
				sqlAppender.append(" ");
				sqlAppender.append( unit.toString() );
		}
		sqlAppender.append(")");
	}

	@Override
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry( queryEngine );

		CommonFunctionFactory.concat_pipeOperator( queryEngine );
		CommonFunctionFactory.octetLength( queryEngine );
		CommonFunctionFactory.moreHyperbolic( queryEngine );
		CommonFunctionFactory.instr( queryEngine );
		CommonFunctionFactory.substr( queryEngine );
		CommonFunctionFactory.substring_substr( queryEngine );
		//also natively supports ANSI-style substring()
		CommonFunctionFactory.position( queryEngine );

		queryEngine.getSqmFunctionRegistry().patternTemplateBuilder( "mod", "(?1 mod ?2)" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 2 )
				.register();

		if ( getVersion() >= 1400 ) {

			//list actually taken from Teradata 15 docs
			CommonFunctionFactory.lastDay( queryEngine );
			CommonFunctionFactory.initcap( queryEngine );
			CommonFunctionFactory.pad( queryEngine );
			CommonFunctionFactory.trim2( queryEngine );
			CommonFunctionFactory.soundex( queryEngine );
			CommonFunctionFactory.ascii( queryEngine );
			CommonFunctionFactory.char_chr( queryEngine );
			CommonFunctionFactory.trunc( queryEngine );
			CommonFunctionFactory.moreHyperbolic( queryEngine );
			CommonFunctionFactory.monthsBetween( queryEngine );
			CommonFunctionFactory.addMonths( queryEngine );
			CommonFunctionFactory.stddevPopSamp( queryEngine );
			CommonFunctionFactory.varPopSamp( queryEngine );
		}

	}

	/**
	 * Does this dialect support the <tt>FOR UPDATE</tt> syntax?
	 *
	 * @return empty string ... Teradata does not support <tt>FOR UPDATE<tt> syntax
	 */
	@Override
	public String getForUpdateString() {
		return "";
	}

	@Override
	public boolean supportsSequences() {
		return false;
	}

	@Override
	public String getAddColumnString() {
		return getVersion() < 1400 ? "Add Column" : "Add";
	}

	@Override
	public SqmMutationStrategy getDefaultIdTableStrategy() {
		return new GlobalTemporaryTableStrategy( generateIdTableExporter() );
	}

	private Exporter<IdTable> generateIdTableExporter() {
		return new GlobalTempTableExporter() {
			@Override
			public String getCreateCommand() {
				return "create global temporary table";
			}

			@Override
			public String getCreateOptions() {
				return " on commit preserve rows";
			}

			@Override
			protected String getTruncateIdTableCommand() {
				return "delete from";
			}
		};
	}

	@Override
	public boolean supportsCascadeDelete() {
		return false;
	}

	@Override
	public boolean supportsCircularCascadeDeleteConstraints() {
		return false;
	}

	@Override
	public boolean supportsTupleDistinctCounts() {
		return false;
	}

	@Override
	public boolean supportsExistsInSelect() {
		return false;
	}

	@Override
	public boolean areStringComparisonsCaseInsensitive() {
		return getVersion() < 1400;
	}

	@Override
	public boolean supportsEmptyInList() {
		return false;
	}

	@Override
	public String getSelectClauseNullString(int sqlType) {
		String v = "null";

		switch ( sqlType ) {
			case Types.BIT:
			case Types.TINYINT:
			case Types.SMALLINT:
			case Types.INTEGER:
			case Types.BIGINT:
			case Types.FLOAT:
			case Types.REAL:
			case Types.DOUBLE:
			case Types.NUMERIC:
			case Types.DECIMAL:
				v = "cast(null as decimal)";
				break;
			case Types.CHAR:
			case Types.VARCHAR:
			case Types.LONGVARCHAR:
				v = "cast(null as varchar(255))";
				break;
			case Types.DATE:
			case Types.TIME:
			case Types.TIMESTAMP:
			case Types.TIMESTAMP_WITH_TIMEZONE:
				v = "cast(null as timestamp)";
				break;
			case Types.BINARY:
			case Types.VARBINARY:
			case Types.LONGVARBINARY:
			case Types.NULL:
			case Types.OTHER:
			case Types.JAVA_OBJECT:
			case Types.DISTINCT:
			case Types.STRUCT:
			case Types.ARRAY:
			case Types.BLOB:
			case Types.CLOB:
			case Types.REF:
			case Types.DATALINK:
			case Types.BOOLEAN:
				break;
		}
		return v;
	}

	@Override
	public boolean supportsUnboundedLobLocatorMaterialization() {
		return false;
	}

	@Override
	public String getCreateMultisetTableString() {
		return "create multiset table ";
	}

	@Override
	public boolean supportsLobValueChangePropogation() {
		return false;
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
	public boolean supportsBindAsCallableArgument() {
		return false;
	}

	@Override
	public int getInExpressionCountLimit() {
		return PARAM_LIST_SIZE_LIMIT;
	}

	@Override
	public int registerResultSetOutParameter(CallableStatement statement, int col) throws SQLException {
		statement.registerOutParameter( col, Types.REF );
		col++;
		return col;
	}

	@Override
	public ResultSet getResultSet(CallableStatement cs) throws SQLException {
		boolean isResultSet = cs.execute();
		while ( !isResultSet && cs.getUpdateCount() != -1 ) {
			isResultSet = cs.getMoreResults();
		}
		return cs.getResultSet();
	}

	@Override
	public ViolatedConstraintNameExtracter getViolatedConstraintNameExtracter() {
		return getVersion() < 1400 ? super.getViolatedConstraintNameExtracter() : EXTRACTER;
	}

	private static ViolatedConstraintNameExtracter EXTRACTER = new TemplatedViolatedConstraintNameExtracter() {
		/**
		 * Extract the name of the violated constraint from the given SQLException.
		 *
		 * @param sqle The exception that was the result of the constraint violation.
		 * @return The extracted constraint name.
		 */
		@Override
		protected String doExtractConstraintName(SQLException sqle) throws NumberFormatException {
			String constraintName = null;

			int errorCode = sqle.getErrorCode();
			switch (errorCode) {
				case 27003:
					constraintName = extractUsingTemplate( "Unique constraint (", ") violated.", sqle.getMessage() );
					break;
				case 2700:
					constraintName = extractUsingTemplate( "Referential constraint", "violation:", sqle.getMessage() );
					break;
				case 5317:
					constraintName = extractUsingTemplate( "Check constraint (", ") violated.", sqle.getMessage() );
					break;
			}

			if ( constraintName != null ) {
				int i = constraintName.indexOf( '.' );
				if ( i != -1 ) {
					constraintName = constraintName.substring( i + 1 );
				}
			}
			return constraintName;
		}
	};

	@Override
	public boolean supportsLockTimeouts() {
		return false;
	}

	@Override
	public boolean useFollowOnLocking(String sql, QueryOptions queryOptions) {
		return getVersion() >= 1400;
	}

	@Override
	public String getWriteLockString(int timeout) {
		if ( getVersion() < 1400 ) {
			return super.getWriteLockString( timeout );
		}
		String sMsg = " Locking row for write ";
		if ( timeout == LockOptions.NO_WAIT ) {
			return sMsg + " nowait ";
		}
		return sMsg;
	}

	@Override
	public String getReadLockString(int timeout) {
		if ( getVersion() < 1400 ) {
			return super.getReadLockString( timeout );
		}
		String sMsg = " Locking row for read  ";
		if ( timeout == LockOptions.NO_WAIT ) {
			return sMsg + " nowait ";
		}
		return sMsg;
	}

	@Override
	public Exporter<Index> getIndexExporter() {
		return new TeradataIndexExporter(this);
	}

	private static class TeradataIndexExporter extends StandardIndexExporter implements Exporter<Index> {

		private TeradataIndexExporter(Dialect dialect) {
			super(dialect);
		}

		@Override
		public String[] getSqlCreateStrings(Index index, JdbcServices jdbcServices) {
			final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
			QualifiedTableName qualifiedTableName = index.getTable().getQualifiedTableName();
			final String tableName = jdbcEnvironment.getQualifiedObjectNameFormatter().format(
					qualifiedTableName,
					jdbcEnvironment.getDialect()
			);

			final String indexNameForCreation;
			if ( getDialect().qualifyIndexName() ) {
				indexNameForCreation = jdbcEnvironment.getQualifiedObjectNameFormatter().format(
						new QualifiedNameImpl(
								qualifiedTableName.getCatalogName(),
								qualifiedTableName.getSchemaName(),
								index.getName()
						),
						jdbcEnvironment.getDialect()
				);
			}
			else {
				indexNameForCreation = index.getName().render( jdbcEnvironment.getDialect() );
			}

			StringBuilder columnList = new StringBuilder();
			boolean first = true;
			for ( PhysicalColumn column : index.getColumns() ) {
				if ( first ) {
					first = false;
				}
				else {
					columnList.append( ", " );
				}
				columnList.append( column.getName().render( jdbcEnvironment.getDialect() ) );
			}

			return new String[] {
					"create index " + indexNameForCreation
							+ "(" + columnList + ") on " + tableName
			};
		}
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return getVersion() < 1400
				? super.getIdentityColumnSupport()
				: new Teradata14IdentityColumnSupport();
	}

	@Override
	@SuppressWarnings("deprecation")
	public String applyLocksToSql(String sql, LockOptions aliasedLockOptions, Map<String, String[]> keyColumnNames) {
		return getVersion() < 1400
				? super.applyLocksToSql( sql, aliasedLockOptions, keyColumnNames )
				: new ForUpdateFragment( this, aliasedLockOptions, keyColumnNames ).toFragmentString() + " " + sql;
	}

}
