/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.LockMode;
import org.hibernate.dialect.lock.*;
import org.hibernate.persister.entity.Lockable;

import java.sql.Types;

/**
 * A Dialect for Pointbase.
 *
 * @author  Ed Mackenzie
 */
public class PointbaseDialect extends org.hibernate.dialect.Dialect {
	/**
	 * Creates new PointbaseDialect
	 */
	public PointbaseDialect() {
		super();
		//no pointbase BIT
		registerColumnType( Types.BIT, 1, "smallint" );
		registerColumnType( Types.BIT, "smallint" );
		//no pointbase TINYINT
		registerColumnType( Types.TINYINT, "smallint" );

		//no precision
		registerColumnType( Types.TIMESTAMP, "timestamp" );
		registerColumnType( Types.TIMESTAMP_WITH_TIMEZONE, "timestamp" );

		registerColumnType( Types.VARBINARY, "blob($l)" );
	}

	@Override
	public String getAddColumnString() {
		return "add";
	}

	@Override
	public boolean dropConstraints() {
		return false;
	}

	@Override
	public String getCascadeConstraintsString() {
		return " cascade";
	}

	@Override
	public String getForUpdateString() {
		return "";
	}

	@Override
	public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {
		// Pointbase has no known variation of a "SELECT ... FOR UPDATE" syntax...
		switch (lockMode) {
			case PESSIMISTIC_FORCE_INCREMENT:
				return new PessimisticForceIncrementLockingStrategy(lockable, lockMode);
			case PESSIMISTIC_WRITE:
				return new PessimisticWriteUpdateLockingStrategy(lockable, lockMode);
			case PESSIMISTIC_READ:
				return new PessimisticReadUpdateLockingStrategy(lockable, lockMode);
			case OPTIMISTIC:
				return new OptimisticLockingStrategy(lockable, lockMode);
			case OPTIMISTIC_FORCE_INCREMENT:
				return new OptimisticForceIncrementLockingStrategy(lockable, lockMode);
		}
		if ( lockMode.greaterThan( LockMode.READ ) ) {
			return new UpdateLockingStrategy( lockable, lockMode );
		}
		else {
			return new SelectLockingStrategy( lockable, lockMode );
		}
	}
}
