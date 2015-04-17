/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.hql.spi;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.internal.log.DeprecationLogger;

/**
 * @author Steve Ebersole
 *
 * @deprecated Use the more specific {@link org.hibernate.hql.spi.LocalTemporaryTableBulkIdStrategy} instead.
 */
@Deprecated
public class TemporaryTableBulkIdStrategy extends LocalTemporaryTableBulkIdStrategy {
	public static final TemporaryTableBulkIdStrategy INSTANCE = new TemporaryTableBulkIdStrategy();

	public static final String SHORT_NAME = "temporary";

	@Override
	public void prepare(JdbcServices jdbcServices, JdbcConnectionAccess connectionAccess, MetadataImplementor metadata) {
		DeprecationLogger.DEPRECATION_LOGGER.logDeprecationOfTemporaryTableBulkIdStrategy();
		super.prepare( jdbcServices, connectionAccess, metadata );
	}

	@Override
	public void release(JdbcServices jdbcServices, JdbcConnectionAccess connectionAccess) {
		super.release( jdbcServices, connectionAccess );
	}

	@Override
	public UpdateHandler buildUpdateHandler(SessionFactoryImplementor factory, HqlSqlWalker walker) {
		return super.buildUpdateHandler( factory, walker );
	}

	@Override
	public DeleteHandler buildDeleteHandler(SessionFactoryImplementor factory, HqlSqlWalker walker) {
		return super.buildDeleteHandler( factory, walker );
	}
}
