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

import org.hibernate.LockMode;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.lock.OptimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.OptimisticLockingStrategy;
import org.hibernate.dialect.lock.PessimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.PessimisticReadUpdateLockingStrategy;
import org.hibernate.dialect.lock.PessimisticWriteUpdateLockingStrategy;
import org.hibernate.dialect.lock.SelectLockingStrategy;
import org.hibernate.dialect.lock.UpdateLockingStrategy;
import org.hibernate.dialect.pagination.FirstLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.sql.JoinFragment;
import org.hibernate.sql.OracleJoinFragment;
import org.hibernate.type.StandardBasicTypes;

/**
 * A SQL dialect for TimesTen 5.1.
 * <p/>
 * Known limitations:
 * joined-subclass support because of no CASE support in TimesTen
 * No support for subqueries that includes aggregation
 * - size() in HQL not supported
 * - user queries that does subqueries with aggregation
 * No CLOB/BLOB support
 * No cascade delete support.
 * No Calendar support
 * No support for updating primary keys.
 *
 * @author Sherry Listgarten and Max Andersen
 */
@SuppressWarnings("deprecation")
public class TimesTenDialect extends Dialect {
	/**
	 * Constructs a TimesTenDialect
	 */
	public TimesTenDialect() {
		super();
		registerColumnType( Types.BIT, "TINYINT" );
		registerColumnType( Types.BIGINT, "BIGINT" );
		registerColumnType( Types.SMALLINT, "SMALLINT" );
		registerColumnType( Types.TINYINT, "TINYINT" );
		registerColumnType( Types.INTEGER, "INTEGER" );
		registerColumnType( Types.CHAR, "CHAR(1)" );
		registerColumnType( Types.VARCHAR, "VARCHAR($l)" );
		registerColumnType( Types.FLOAT, "FLOAT" );
		registerColumnType( Types.DOUBLE, "DOUBLE" );
		registerColumnType( Types.DATE, "DATE" );
		registerColumnType( Types.TIME, "TIME" );
		registerColumnType( Types.TIMESTAMP, "TIMESTAMP" );
		registerColumnType( Types.VARBINARY, "VARBINARY($l)" );
		registerColumnType( Types.NUMERIC, "DECIMAL($p, $s)" );
		// TimesTen has no BLOB/CLOB support, but these types may be suitable 
		// for some applications. The length is limited to 4 million bytes.
		registerColumnType( Types.BLOB, "VARBINARY(4000000)" );
		registerColumnType( Types.CLOB, "VARCHAR(4000000)" );

		getDefaultProperties().setProperty( Environment.USE_STREAMS_FOR_BINARY, "true" );
		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE );
		registerFunction( "lower", new StandardSQLFunction( "lower" ) );
		registerFunction( "upper", new StandardSQLFunction( "upper" ) );
		registerFunction( "rtrim", new StandardSQLFunction( "rtrim" ) );
		registerFunction( "concat", new StandardSQLFunction( "concat", StandardBasicTypes.STRING ) );
		registerFunction( "mod", new StandardSQLFunction( "mod" ) );
		registerFunction( "to_char", new StandardSQLFunction( "to_char", StandardBasicTypes.STRING ) );
		registerFunction( "to_date", new StandardSQLFunction( "to_date", StandardBasicTypes.TIMESTAMP ) );
		registerFunction( "sysdate", new NoArgSQLFunction( "sysdate", StandardBasicTypes.TIMESTAMP, false ) );
		registerFunction( "getdate", new NoArgSQLFunction( "getdate", StandardBasicTypes.TIMESTAMP, false ) );
		registerFunction( "nvl", new StandardSQLFunction( "nvl" ) );

	}

	@Override
	public boolean dropConstraints() {
		return true;
	}

	@Override
	public boolean qualifyIndexName() {
		return false;
	}

	@Override
	public String getAddColumnString() {
		return "add";
	}

	@Override
	public boolean supportsSequences() {
		return true;
	}

	@Override
	public String getSelectSequenceNextValString(String sequenceName) {
		return sequenceName + ".nextval";
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {
		return "select first 1 " + sequenceName + ".nextval from sys.tables";
	}

	@Override
	public String getCreateSequenceString(String sequenceName) {
		return "create sequence " + sequenceName;
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence " + sequenceName;
	}

	@Override
	public String getQuerySequencesString() {
		return "select NAME from sys.sequences";
	}

	@Override
	public JoinFragment createOuterJoinFragment() {
		return new OracleJoinFragment();
	}

	@Override
	public String getCrossJoinSeparator() {
		return ", ";
	}

	@Override
	public String getForUpdateString() {
		return "";
	}

	@Override
	public boolean supportsColumnCheck() {
		return false;
	}

	@Override
	public boolean supportsTableCheck() {
		return false;
	}

	@Override
    public LimitHandler buildLimitHandler(String sql, RowSelection selection) {
        return new FirstLimitHandler(sql, selection);
    }

	@Override
	public boolean supportsCurrentTimestampSelection() {
		return true;
	}

	@Override
	public String getCurrentTimestampSelectString() {
		return "select first 1 sysdate from sys.tables";
	}

	@Override
	public boolean isCurrentTimestampSelectStringCallable() {
		return false;
	}

	@Override
	public boolean supportsTemporaryTables() {
		return true;
	}

	@Override
	public String generateTemporaryTableName(String baseTableName) {
		final String name = super.generateTemporaryTableName( baseTableName );
		return name.length() > 30 ? name.substring( 1, 30 ) : name;
	}

	@Override
	public String getCreateTemporaryTableString() {
		return "create global temporary table";
	}

	@Override
	public String getCreateTemporaryTablePostfix() {
		return "on commit delete rows";
	}

	@Override
	public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {
		// TimesTen has no known variation of a "SELECT ... FOR UPDATE" syntax...
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

	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean supportsEmptyInList() {
		return false;
	}
}
