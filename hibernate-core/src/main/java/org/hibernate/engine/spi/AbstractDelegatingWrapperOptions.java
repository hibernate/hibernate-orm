/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import java.util.TimeZone;

import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * @author Christian Beikov
 * @author Andrea Boriero
 */
public abstract class AbstractDelegatingWrapperOptions implements WrapperOptions {

	/**
	 * Returns the underlying session delegate.
	 */
	protected abstract SessionImplementor delegate();

	@Override
	public boolean useStreamForLobBinding() {
		return delegate().useStreamForLobBinding();
	}

	@Override
	public LobCreator getLobCreator() {
		return delegate().getLobCreator();
	}

	@Override
	public SqlTypeDescriptor remapSqlTypeDescriptor(SqlTypeDescriptor sqlTypeDescriptor) {
		return delegate().remapSqlTypeDescriptor( sqlTypeDescriptor );
	}

	@Override
	public TimeZone getJdbcTimeZone() {
		return delegate().getJdbcTimeZone();
	}
}
