/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.persister.entity.Queryable;

/**
 * Strategy based on ANSI SQL's definition of a "global temporary table".
 *
 * @author Steve Ebersole
 */
public class GlobalTemporaryTableBulkIdStrategy implements MultiTableBulkIdStrategy {
	public static final String CLEAN_UP_ID_TABLES = "hibernate.hql.bulk_id_strategy.global_temporary.clean_up";

	public static final String SHORT_NAME = "global_temporary";

	private boolean cleanUpTables;
	private List<String> tableCleanUpDdl;

	@Override
	public void prepare(
			JdbcServices jdbcServices,
			JdbcConnectionAccess connectionAccess,
			MetadataImplementor metadata) {
		final ConfigurationService configService = metadata.getMetadataBuildingOptions()
				.getServiceRegistry()
				.getService( ConfigurationService.class );
		this.cleanUpTables = configService.getSetting(
				CLEAN_UP_ID_TABLES,
				StandardConverters.BOOLEAN,
				false
		);

		final List<Table> idTableDefinitions = new ArrayList<Table>();

		for ( PersistentClass entityBinding : metadata.getEntityBindings() ) {
			if ( !MultiTableBulkIdHelper.INSTANCE.needsIdTable( entityBinding ) ) {
				continue;
			}
			final Table idTableDefinition = generateIdTableDefinition( entityBinding, metadata );
			idTableDefinitions.add( idTableDefinition );

			if ( cleanUpTables ) {
				if ( tableCleanUpDdl == null ) {
					tableCleanUpDdl = new ArrayList<String>();
				}
				tableCleanUpDdl.add( idTableDefinition.sqlDropString( jdbcServices.getDialect(), null, null  ) );
			}
		}

		// we export them all at once to better reuse JDBC resources
		exportTableDefinitions( idTableDefinitions, jdbcServices, connectionAccess, metadata );
	}

	protected Table generateIdTableDefinition(PersistentClass entityMapping, MetadataImplementor metadata) {
		return MultiTableBulkIdHelper.INSTANCE.generateIdTableDefinition(
				entityMapping,
				null,
				null,
				false
		);
	}

	protected void exportTableDefinitions(
			List<Table> idTableDefinitions,
			JdbcServices jdbcServices,
			JdbcConnectionAccess connectionAccess,
			MetadataImplementor metadata) {
		MultiTableBulkIdHelper.INSTANCE.exportTableDefinitions(
				idTableDefinitions,
				jdbcServices,
				connectionAccess,
				metadata
		);
	}

	@Override
	public void release(
			JdbcServices jdbcServices,
			JdbcConnectionAccess connectionAccess) {
		if ( ! cleanUpTables ) {
			return;
		}

		MultiTableBulkIdHelper.INSTANCE.cleanupTableDefinitions( jdbcServices, connectionAccess, tableCleanUpDdl );
	}

	@Override
	public UpdateHandler buildUpdateHandler(
			SessionFactoryImplementor factory, HqlSqlWalker walker) {
		return new TableBasedUpdateHandlerImpl( factory, walker ) {
			@Override
			protected void releaseFromUse(Queryable persister, SessionImplementor session) {
				// clean up our id-table rows
				cleanUpRows( determineIdTableName( persister ), session );
			}
		};
	}

	private void cleanUpRows(String tableName, SessionImplementor session) {
		final String sql = "delete from " + tableName;
		PreparedStatement ps = null;
		try {
			ps = session.getTransactionCoordinator().getJdbcCoordinator().getStatementPreparer().prepareStatement( sql, false );
			session.getTransactionCoordinator().getJdbcCoordinator().getResultSetReturn().executeUpdate( ps );
		}
		finally {
			if ( ps != null ) {
				try {
					session.getTransactionCoordinator().getJdbcCoordinator().release( ps );
				}
				catch( Throwable ignore ) {
					// ignore
				}
			}
		}
	}

	@Override
	public DeleteHandler buildDeleteHandler(
			SessionFactoryImplementor factory, HqlSqlWalker walker) {
		return new TableBasedDeleteHandlerImpl( factory, walker ) {
			@Override
			protected void releaseFromUse(Queryable persister, SessionImplementor session) {
				// clean up our id-table rows
				cleanUpRows( determineIdTableName( persister ), session );
			}
		};
	}
}
