/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.Types;

import org.hibernate.LockMode;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.LtrimRtrimReplaceTrimEmulation;
import org.hibernate.dialect.function.RDMSExtractEmulation;
import org.hibernate.metamodel.model.domain.spi.Lockable;
import org.hibernate.query.TemporalUnit;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.lock.OptimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.OptimisticLockingStrategy;
import org.hibernate.dialect.lock.PessimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.PessimisticReadUpdateLockingStrategy;
import org.hibernate.dialect.lock.PessimisticWriteUpdateLockingStrategy;
import org.hibernate.dialect.lock.SelectLockingStrategy;
import org.hibernate.dialect.lock.UpdateLockingStrategy;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitHelper;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.sql.CaseFragment;
import org.hibernate.sql.DecodeCaseFragment;

import org.jboss.logging.Logger;

import static org.hibernate.query.TemporalUnit.NANOSECOND;

/**
 * This is the Hibernate dialect for the Unisys 2200 Relational Database (RDMS).
 * This dialect was developed for use with Hibernate 3.0.5. Other versions may
 * require modifications to the dialect.
 * <p/>
 * Version History:
 * Also change the version displayed below in the constructor
 * 1.1
 * 1.0  2005-10-24  CDH - First dated version for use with CP 11
 *
 * @author Ploski and Hanson
 */
public class RDMSOS2200Dialect extends Dialect {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			RDMSOS2200Dialect.class.getName()
	);

	private static final AbstractLimitHandler LIMIT_HANDLER = new AbstractLimitHandler() {
		@Override
		public String processSql(String sql, RowSelection selection) {
			final boolean hasOffset = LimitHelper.hasFirstRow( selection );
			if (hasOffset) {
				throw new UnsupportedOperationException( "query result offset is not supported" );
			}
			return sql + " fetch first " + getMaxOrLimit( selection ) + " rows only ";
		}

		@Override
		public boolean supportsLimit() {
			return true;
		}

		@Override
		public boolean supportsLimitOffset() {
			return false;
		}

		@Override
		public boolean supportsVariableLimit() {
			return false;
		}
	};

	private static final AbstractLimitHandler LEGACY_LIMIT_HANDLER = new AbstractLimitHandler() {
		@Override
		public String processSql(String sql, RowSelection selection) {
			return sql + " fetch first " + getMaxOrLimit( selection ) + " rows only ";
		}

		@Override
		public boolean supportsLimit() {
			return true;
		}

		@Override
		public boolean supportsLimitOffset() {
			return false;
		}

		@Override
		public boolean supportsVariableLimit() {
			return false;
		}
	};

	/**
	 * Constructs a RDMSOS2200Dialect
	 */
	public RDMSOS2200Dialect() {
		super();
		// Display the dialect version.
		LOG.rdmsOs2200Dialect();

		/*
		 * For a list of column types to register, see section A-1
		 * in 7862 7395, the Unisys JDBC manual.
		 *
		 * Here are column sizes as documented in Table A-1 of
		 * 7831 0760, "Enterprise Relational Database Server
		 * for ClearPath OS2200 Administration Guide"
		 * Numeric - 21
		 * Decimal - 22 (21 digits plus one for sign)
		 * Float   - 60 bits
		 * Char    - 28000
		 * NChar   - 14000
		 * BLOB+   - 4294967296 (4 Gb)
		 * + RDMS JDBC driver does not support BLOBs
		 *
		 * DATE, TIME and TIMESTAMP literal formats are
		 * are all described in section 2.3.4 DATE Literal Format
		 * in 7830 8160.
		 * The DATE literal format is: YYYY-MM-DD
		 * The TIME literal format is: HH:MM:SS[.[FFFFFF]]
		 * The TIMESTAMP literal format is: YYYY-MM-DD HH:MM:SS[.[FFFFFF]]
		 *
		 * Note that $l (dollar-L) will use the length value if provided.
		 * Also new for Hibernate3 is the $p percision and $s (scale) parameters
		 */
		registerColumnType( Types.BIT, 1, "smallint" );
		registerColumnType( Types.BIT, "smallint" );
		registerColumnType( Types.BOOLEAN, "smallint" );
		registerColumnType( Types.TINYINT, "smallint" );
		registerColumnType( Types.BIGINT, "numeric(19,0)" );
		registerColumnType( Types.BLOB, "blob($l)" );

		//no 'binary' nor 'varbinary' so use 'blob'
		registerColumnType( Types.BINARY, "blob($l)");
		registerColumnType( Types.VARBINARY, "blob($l)");
		registerColumnType( Types.LONGVARBINARY, "blob($l)");

		//'varchar' is not supported in RDMS for OS 2200
		//(but it is for other flavors of RDMS)
		//'character' means ASCII by default, 'unicode(n)'
		//means 'character(n) character set "UCS-2"'
		registerColumnType( Types.CHAR, "unicode($l)");
		registerColumnType( Types.VARCHAR, "unicode($l)");
		registerColumnType( Types.LONGVARCHAR, "unicode($l)");

		registerColumnType( Types.TIMESTAMP_WITH_TIMEZONE, "timestamp($p)");
	}

	@Override
	public int getDefaultDecimalPrecision() {
		//the (really low) maximum
		return 21;
	}

	@Override
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry( queryEngine );

		CommonFunctionFactory.cosh( queryEngine );
		CommonFunctionFactory.sinh( queryEngine );
		CommonFunctionFactory.tanh( queryEngine );
		CommonFunctionFactory.cot( queryEngine );
		CommonFunctionFactory.log( queryEngine );
		CommonFunctionFactory.log10( queryEngine );
		CommonFunctionFactory.pi( queryEngine );
		CommonFunctionFactory.rand( queryEngine );
		CommonFunctionFactory.trunc( queryEngine );
		CommonFunctionFactory.truncate( queryEngine );
		CommonFunctionFactory.soundex( queryEngine );
		CommonFunctionFactory.trim2( queryEngine );
		CommonFunctionFactory.space( queryEngine );
		CommonFunctionFactory.repeat( queryEngine );
//		CommonFunctionFactory.replicate( queryEngine ); //synonym for more common repeat()
		CommonFunctionFactory.initcap( queryEngine );
		CommonFunctionFactory.instr( queryEngine );
		CommonFunctionFactory.substr( queryEngine );
		CommonFunctionFactory.translate( queryEngine );
		CommonFunctionFactory.yearMonthDay( queryEngine );
		CommonFunctionFactory.hourMinuteSecond( queryEngine );
		CommonFunctionFactory.dayofweekmonthyear( queryEngine );
		CommonFunctionFactory.weekQuarter( queryEngine );
		CommonFunctionFactory.daynameMonthname( queryEngine );
		CommonFunctionFactory.lastDay( queryEngine );
		CommonFunctionFactory.ceiling_ceil( queryEngine );
		CommonFunctionFactory.concat_pipeOperator( queryEngine );
		CommonFunctionFactory.leftRight( queryEngine );
		CommonFunctionFactory.ascii( queryEngine );
		CommonFunctionFactory.chr_char( queryEngine );
		CommonFunctionFactory.insert( queryEngine );
		CommonFunctionFactory.addMonths( queryEngine );
		CommonFunctionFactory.monthsBetween( queryEngine );

		// RDMS does not directly support the trim() function, we use rtrim() and ltrim()
		queryEngine.getSqmFunctionRegistry().register( "trim", new LtrimRtrimReplaceTrimEmulation() );

		queryEngine.getSqmFunctionRegistry().register( "extract", new RDMSExtractEmulation() );

	}

	@Override
	public void timestampadd(TemporalUnit unit, Renderer magnitude, Renderer to, Appender sqlAppender, boolean timestamp) {
		if ( unit == NANOSECOND ) {
			sqlAppender.append("timestampadd('SQL_TSI_FRAC_SECOND'"); //micros
		}
		else {
			sqlAppender.append("dateadd('");
			sqlAppender.append( unit.toString() );
			sqlAppender.append("'");
		}
		sqlAppender.append(", ");
		if ( unit == NANOSECOND ) {
			sqlAppender.append("(");
		}
		magnitude.render();
		if ( unit == NANOSECOND ) {
			sqlAppender.append(")/1e3");
		}
		sqlAppender.append(", ");
		to.render();
		sqlAppender.append(")");
	}

	@Override
	public void timestampdiff(TemporalUnit unit, Renderer from, Renderer to, Appender sqlAppender, boolean fromTimestamp, boolean toTimestamp) {
		if ( unit == NANOSECOND ) {
			sqlAppender.append("timestampdiff('SQL_TSI_FRAC_SECOND'"); //micros
		}
		else {
			sqlAppender.append("datediff('");
			sqlAppender.append( unit.toString() );
			sqlAppender.append("'");
		}
		sqlAppender.append(", ");
		from.render();
		sqlAppender.append(", ");
		to.render();
		sqlAppender.append(")");
		if ( unit == NANOSECOND ) {
			sqlAppender.append("*1e3");
		}
	}

	// Dialect method overrides ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * RDMS does not support qualifing index names with the schema name.
	 * <p/>
	 * {@inheritDoc}
	 */
	@Override
	public boolean qualifyIndexName() {
		return false;
	}

	/**
	 * The RDMS DB supports the 'FOR UPDATE OF' clause. However, the RDMS-JDBC
	 * driver does not support this feature, so a false is return.
	 * The base dialect also returns a false, but we will leave this over-ride
	 * in to make sure it stays false.
	 * <p/>
	 * {@inheritDoc}
	 */
	@Override
	public boolean forUpdateOfColumns() {
		return false;
	}

	/**
	 * Since the RDMS-JDBC driver does not support for updates, this string is
	 * set to an empty string. Whenever, the driver does support this feature,
	 * the returned string should be " FOR UPDATE OF". Note that RDMS does not
	 * support the string 'FOR UPDATE' string.
	 * <p/>
	 * {@inheritDoc}
	 */
	@Override
	public String getForUpdateString() {
		// Original Dialect.java returns " for update";
		return "";
	}

	// Verify the state of this new method in Hibernate 3.0 Dialect.java

	/**
	 * RDMS does not support Cascade Deletes.
	 * Need to review this in the future when support is provided.
	 * <p/>
	 * {@inheritDoc}
	 */
	@Override
	public boolean supportsCascadeDelete() {
		return false;
	}

	/**
	 * Currently, RDMS-JDBC does not support ForUpdate.
	 * Need to review this in the future when support is provided.
	 * <p/>
	 * {@inheritDoc}
	 */
	@Override
	public boolean supportsOuterJoinForUpdate() {
		return false;
	}

	@Override
	public String getAddColumnString() {
		return "add";
	}

	@Override
	public String getNullColumnString() {
		// The keyword used to specify a nullable column.
		return " null";
	}

	@Override
	public boolean supportsSequences() {
		return true;
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {
		// The where clause was added to eliminate this statement from Brute Force Searches.
		return "select permuted_id('NEXT',31) from rdms.rdms_dummy where key_col = 1 ";
	}

	@Override
	public String getCreateSequenceString(String sequenceName) {
		// We must return a valid RDMS/RSA command from this method to
		// prevent RDMS/RSA from issuing *ERROR 400
		return "";
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		// We must return a valid RDMS/RSA command from this method to
		// prevent RDMS/RSA from issuing *ERROR 400
		return "";
	}

	@Override
	public String getCascadeConstraintsString() {
		// Used with DROP TABLE to delete all records in the table.
		return " including contents";
	}

	@Override
	@SuppressWarnings("deprecation")
	public CaseFragment createCaseFragment() {
		return new DecodeCaseFragment();
	}

	@Override
	public LimitHandler getLimitHandler() {
		if ( isLegacyLimitHandlerBehaviorEnabled() ) {
			return LEGACY_LIMIT_HANDLER;
		}
		return LIMIT_HANDLER;
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean supportsLimit() {
		return true;
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean supportsLimitOffset() {
		return false;
	}

	@Override
	@SuppressWarnings("deprecation")
	public String getLimitString(String sql, int offset, int limit) {
		if ( offset > 0 ) {
			throw new UnsupportedOperationException( "query result offset is not supported" );
		}
		return sql + " fetch first " + limit + " rows only ";
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean supportsVariableLimit() {
		return false;
	}

	@Override
	public boolean supportsUnionAll() {
		// RDMS supports the UNION ALL clause.
		return true;
	}

	@Override
	public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {
		// RDMS has no known variation of a "SELECT ... FOR UPDATE" syntax...
		if ( lockMode == LockMode.PESSIMISTIC_FORCE_INCREMENT ) {
			return new PessimisticForceIncrementLockingStrategy( lockable, lockMode );
		}
		else if ( lockMode == LockMode.PESSIMISTIC_WRITE ) {
			return new PessimisticWriteUpdateLockingStrategy( lockable, lockMode );
		}
		else if ( lockMode == LockMode.PESSIMISTIC_READ ) {
			return new PessimisticReadUpdateLockingStrategy( lockable, lockMode );
		}
		else if ( lockMode == LockMode.OPTIMISTIC ) {
			return new OptimisticLockingStrategy( lockable, lockMode );
		}
		else if ( lockMode == LockMode.OPTIMISTIC_FORCE_INCREMENT ) {
			return new OptimisticForceIncrementLockingStrategy( lockable, lockMode );
		}
		else if ( lockMode.greaterThan( LockMode.READ ) ) {
			return new UpdateLockingStrategy( lockable, lockMode );
		}
		else {
			return new SelectLockingStrategy( lockable, lockMode );
		}
	}

	@Override
	public String translateDatetimeFormat(String format) {
		return OracleDialect.datetimeFormat( format, true ) //Does it really support FM?
				.replace("SSSSSS", "MLS")
				.replace("SSSSS", "MLS")
				.replace("SSSS", "MLS")
				.replace("SSS", "MLS")
				.replace("SS", "MLS")
				.replace("S", "MLS")
				.result();
	}
}
