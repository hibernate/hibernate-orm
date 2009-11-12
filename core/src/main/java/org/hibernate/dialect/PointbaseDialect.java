/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.dialect;

import org.hibernate.dialect.lock.*;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.LockMode;

import java.sql.Types;

/**
 * A <tt>Dialect</tt> for Pointbase.
 * @author  Ed Mackenzie
 */
public class PointbaseDialect extends org.hibernate.dialect.Dialect {

	/**
	 * Creates new PointbaseDialect
	 */
	public PointbaseDialect() {
		super();
		registerColumnType( Types.BIT, "smallint" ); //no pointbase BIT
		registerColumnType( Types.BIGINT, "bigint" );
		registerColumnType( Types.SMALLINT, "smallint" );
		registerColumnType( Types.TINYINT, "smallint" ); //no pointbase TINYINT
		registerColumnType( Types.INTEGER, "integer" );
		registerColumnType( Types.CHAR, "char(1)" );
		registerColumnType( Types.VARCHAR, "varchar($l)" );
		registerColumnType( Types.FLOAT, "float" );
		registerColumnType( Types.DOUBLE, "double precision" );
		registerColumnType( Types.DATE, "date" );
		registerColumnType( Types.TIME, "time" );
		registerColumnType( Types.TIMESTAMP, "timestamp" );
		//the BLOB type requires a size arguement - this defaults to
		//bytes - no arg defaults to 1 whole byte!
		//other argument mods include K - kilobyte, M - megabyte, G - gigabyte.
		//refer to the PBdevelopers guide for more info.
		registerColumnType( Types.VARBINARY, "blob($l)" );
		registerColumnType( Types.NUMERIC, "numeric($p,$s)" );
	}

	public String getAddColumnString() {
		return "add";
	}

	public boolean dropConstraints() {
		return false;
	}

	public String getCascadeConstraintsString() {
		return " cascade";
	}

	public String getForUpdateString() {
		return "";
	}

	public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {
		// Pointbase has no known variation of a "SELECT ... FOR UPDATE" syntax...
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
