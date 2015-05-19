/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;
import java.sql.Types;

import org.hibernate.LockMode;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.lock.OptimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.OptimisticLockingStrategy;
import org.hibernate.dialect.lock.PessimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.PessimisticReadUpdateLockingStrategy;
import org.hibernate.dialect.lock.PessimisticWriteUpdateLockingStrategy;
import org.hibernate.dialect.lock.SelectLockingStrategy;
import org.hibernate.dialect.lock.UpdateLockingStrategy;
import org.hibernate.persister.entity.Lockable;

/**
 * An SQL Dialect for Frontbase.  Assumes you're using the latest version
 * of the FrontBase JDBC driver, available from <tt>http://frontbase.com/</tt>
 * <p>
 * <b>NOTE</b>: The latest JDBC driver is not always included with the
 * latest release of FrontBase.  Download the driver separately, and enjoy
 * the informative release notes.
 * <p>
 * This dialect was tested with JDBC driver version 2.3.1.  This driver
 * contains a bug that causes batches of updates to fail.  (The bug should be
 * fixed in the next release of the JDBC driver.)  If you are using JDBC driver
 * 2.3.1, you can work-around this problem by setting the following in your
 * <tt>hibernate.properties</tt> file: <tt>hibernate.jdbc.batch_size=15</tt>
 *
 * @author Ron Lussier <tt>rlussier@lenscraft.com</tt>
 */
public class FrontBaseDialect extends Dialect {

	/**
	 * Constructs a FrontBaseDialect
	 */
	public FrontBaseDialect() {
		super();

		registerColumnType( Types.BIT, "bit" );
		registerColumnType( Types.BIGINT, "longint" );
		registerColumnType( Types.SMALLINT, "smallint" );
		registerColumnType( Types.TINYINT, "tinyint" );
		registerColumnType( Types.INTEGER, "integer" );
		registerColumnType( Types.CHAR, "char(1)" );
		registerColumnType( Types.VARCHAR, "varchar($l)" );
		registerColumnType( Types.FLOAT, "float" );
		registerColumnType( Types.DOUBLE, "double precision" );
		registerColumnType( Types.DATE, "date" );
		registerColumnType( Types.TIME, "time" );
		registerColumnType( Types.TIMESTAMP, "timestamp" );
		registerColumnType( Types.VARBINARY, "bit varying($l)" );
		registerColumnType( Types.NUMERIC, "numeric($p,$s)" );
		registerColumnType( Types.BLOB, "blob" );
		registerColumnType( Types.CLOB, "clob" );
	}

	@Override
	public String getAddColumnString() {
		return "add column";
	}

	@Override
	public String getCascadeConstraintsString() {
		return " cascade";
	}

	@Override
	public boolean dropConstraints() {
		return false;
	}

	/**
	 * FrontBase doesn't support this syntax, which was dropped with SQL92.
	 * <p/>
	 * {@inheritDoc}
	 */
	@Override
	public String getForUpdateString() {
		return "";
	}

	@Override
	public String getCurrentTimestampSelectString() {
		// TODO : not sure this is correct, could not find docs on how to do this.
		return "{?= call current_timestamp}";
	}

	@Override
	public boolean isCurrentTimestampSelectStringCallable() {
		return true;
	}

	@Override
	public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {
		// Frontbase has no known variation of a "SELECT ... FOR UPDATE" syntax...
		if ( lockMode==LockMode.PESSIMISTIC_FORCE_INCREMENT) {
			return new PessimisticForceIncrementLockingStrategy( lockable, lockMode);
		}
		else if ( lockMode==LockMode.PESSIMISTIC_WRITE) {
			return new PessimisticWriteUpdateLockingStrategy( lockable, lockMode);
		}
		else if ( lockMode==LockMode.PESSIMISTIC_READ) {
			return new PessimisticReadUpdateLockingStrategy( lockable, lockMode);
		}
		else if ( lockMode==LockMode.OPTIMISTIC) {
			return new OptimisticLockingStrategy( lockable, lockMode);
		}
		else if ( lockMode==LockMode.OPTIMISTIC_FORCE_INCREMENT) {
			return new OptimisticForceIncrementLockingStrategy( lockable, lockMode);
		}
		else if ( lockMode.greaterThan( LockMode.READ ) ) {
			return new UpdateLockingStrategy( lockable, lockMode );
		}
		else {
			return new SelectLockingStrategy( lockable, lockMode );
		}
	}
}
