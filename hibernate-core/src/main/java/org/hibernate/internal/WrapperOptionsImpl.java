/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

import org.hibernate.Hibernate;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class WrapperOptionsImpl implements WrapperOptions {
	private final SessionImplementor session;

	private final boolean useStreamForLobBinding;

	public WrapperOptionsImpl(SessionImplementor session) {
		this.session = session;

		this.useStreamForLobBinding = Environment.useStreamsForBinary()
				|| session.getFactory().getDialect().useInputStreamToInsertBlob();
	}

	@Override
	public boolean useStreamForLobBinding() {
		return useStreamForLobBinding;
	}

	@Override
	public LobCreator getLobCreator() {
		return Hibernate.getLobCreator( session );
	}

	@Override
	public SqlTypeDescriptor remapSqlTypeDescriptor(SqlTypeDescriptor sqlTypeDescriptor) {
		final SqlTypeDescriptor remapped = sqlTypeDescriptor.canBeRemapped()
				? session.getFactory().getDialect().remapSqlTypeDescriptor( sqlTypeDescriptor )
				: sqlTypeDescriptor;
		return remapped == null ? sqlTypeDescriptor : remapped;
	}
}
