/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;


import java.sql.Types;

import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.SybaseAnywhereIdentityColumnSupport;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.TopLimitHandler;
import org.hibernate.type.descriptor.sql.spi.BitSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * SQL Dialect for Sybase Anywhere
 * (Tested on ASA 8.x)
 */
public class SybaseAnywhereDialect extends SybaseDialect {

	public SybaseAnywhereDialect() {
		super();

		registerColumnType( Types.BIGINT, "bigint" );
		registerColumnType( Types.DATE, "date" );
		registerColumnType( Types.TIME, "time" );
		registerColumnType( Types.TIMESTAMP, "timestamp" );
		registerColumnType( Types.TIMESTAMP_WITH_TIMEZONE, "timestamp with time zone" );
	}

	@Override
	protected SqlTypeDescriptor getSqlTypeDescriptorOverride(int sqlCode) {
		return sqlCode == Types.BOOLEAN
				? BitSqlDescriptor.INSTANCE
				: super.getSqlTypeDescriptorOverride( sqlCode );
	}

	@Override
	public String currentDate() {
		return "current date";
	}

	@Override
	public String currentTime() {
		return "current time";
	}

	@Override
	public String currentTimestamp() {
		return "current timestamp";
	}

	/**
	 * Sybase Anywhere syntax would require a "DEFAULT" for each column specified,
	 * but I suppose Hibernate use this syntax only with tables with just 1 column
	 * <p/>
	 * {@inheritDoc}
	 */
	@Override
	public String getNoColumnsInsertString() {
		return "values (default)";
	}

	/**
	 * ASA does not require to drop constraint before dropping tables, so disable it.
	 * <p/>
	 * NOTE : Also, the DROP statement syntax used by Hibernate to drop constraints is
	 * not compatible with ASA.
	 * <p/>
	 * {@inheritDoc}
	 */
	@Override
	public boolean dropConstraints() {
		return false;
	}

	@Override
	public String getFromDual() {
		return "from sys.dummy";
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new SybaseAnywhereIdentityColumnSupport();
	}

	@Override
	public LimitHandler getLimitHandler() {
		//TODO: support 'TOP ? START AT ?'
		//Note: Sybase Anywhere also supports LIMIT OFFSET,
		//      but it looks like this syntax is not enabled
		//      by default
		return TopLimitHandler.INSTANCE;
	}
}
