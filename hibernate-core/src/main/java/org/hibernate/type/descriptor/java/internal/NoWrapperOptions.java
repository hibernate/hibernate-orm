/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.java.internal;

import java.util.TimeZone;

import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.spi.WrapperOptions;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class NoWrapperOptions implements WrapperOptions {
	/**
	 * Singleton access
	 */
	public static final NoWrapperOptions INSTANCE = new NoWrapperOptions();

	@Override
	public boolean useStreamForLobBinding() {
		return false;
	}

	@Override
	public LobCreator getLobCreator() {
		throw new UnsupportedOperationException( "LOB locator creation not supported here" );
	}

	@Override
	public SqlTypeDescriptor remapSqlTypeDescriptor(SqlTypeDescriptor sqlTypeDescriptor) {
		return sqlTypeDescriptor;
	}

	@Override
	public TimeZone getJdbcTimeZone() {
		return TimeZone.getDefault();
	}

	@Override
	public SharedSessionContractImplementor getSession() {
		throw new UnsupportedOperationException( "No Session" );
	}
}
