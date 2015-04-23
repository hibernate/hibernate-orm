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
package org.hibernate.hql.spi.id.local;

import org.hibernate.boot.TempTableDdlTransactionHandling;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.internal.ast.tree.DeleteStatement;
import org.hibernate.hql.internal.ast.tree.FromElement;
import org.hibernate.hql.internal.ast.tree.UpdateStatement;
import org.hibernate.hql.spi.id.AbstractMultiTableBulkIdStrategyImpl;
import org.hibernate.hql.spi.id.AbstractMultiTableBulkIdStrategyImpl.PreparationContext;
import org.hibernate.hql.spi.id.IdTableSupport;
import org.hibernate.hql.spi.id.IdTableSupportStandardImpl;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.hql.spi.id.TableBasedDeleteHandlerImpl;
import org.hibernate.hql.spi.id.TableBasedUpdateHandlerImpl;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.persister.entity.Queryable;

/**
 * Strategy based on ANSI SQL's definition of a "local temporary table" (local to each db session).
 *
 * @author Steve Ebersole
 */
public class LocalTemporaryTableBulkIdStrategy
		extends AbstractMultiTableBulkIdStrategyImpl<IdTableInfoImpl, PreparationContext>
		implements MultiTableBulkIdStrategy {

	public static final String SHORT_NAME = "local_temporary";

	private final AfterUseAction afterUseAction;
	private TempTableDdlTransactionHandling ddlTransactionHandling;

	public LocalTemporaryTableBulkIdStrategy() {
		this(
				new IdTableSupportStandardImpl() {
					@Override
					public String getCreateIdTableCommand() {
						return "create local temporary table";
					}
				},
				AfterUseAction.DROP,
				null
		);
	}

	public LocalTemporaryTableBulkIdStrategy(
			IdTableSupport idTableSupport,
			AfterUseAction afterUseAction,
			TempTableDdlTransactionHandling ddlTransactionHandling) {
		super( idTableSupport );
		this.afterUseAction = afterUseAction;
		this.ddlTransactionHandling = ddlTransactionHandling;
	}

	@Override
	protected void initialize(MetadataBuildingOptions buildingOptions, SessionFactoryOptions sessionFactoryOptions) {
		if ( ddlTransactionHandling == null ) {
			ddlTransactionHandling = sessionFactoryOptions.getTempTableDdlTransactionHandling();
		}
	}

	@Override
	protected IdTableInfoImpl buildIdTableInfo(
			PersistentClass entityBinding,
			Table idTable,
			JdbcServices jdbcServices,
			MetadataImplementor metadata,
			PreparationContext context) {
		return new IdTableInfoImpl(
				jdbcServices.getJdbcEnvironment().getQualifiedObjectNameFormatter().format(
						idTable.getQualifiedTableName(),
						jdbcServices.getJdbcEnvironment().getDialect()
				),
				buildIdTableCreateStatement( idTable, jdbcServices, metadata ),
				buildIdTableDropStatement( idTable, jdbcServices )
		);
	}

	@Override
	public void release(JdbcServices jdbcServices, JdbcConnectionAccess connectionAccess) {
		// nothing to do
	}

	@Override
	public UpdateHandler buildUpdateHandler(SessionFactoryImplementor factory, HqlSqlWalker walker) {
		final UpdateStatement updateStatement = (UpdateStatement) walker.getAST();

		final FromElement fromElement = updateStatement.getFromClause().getFromElement();
		final Queryable targetedPersister = fromElement.getQueryable();

		final IdTableInfoImpl tableInfo = getIdTableInfo( targetedPersister );

		return new TableBasedUpdateHandlerImpl( factory, walker, tableInfo ) {
			@Override
			protected void prepareForUse(Queryable persister, SessionImplementor session) {
				Helper.INSTANCE.createTempTable(
						tableInfo,
						ddlTransactionHandling,
						session
				);
			}

			@Override
			protected void releaseFromUse(Queryable persister, SessionImplementor session) {
				Helper.INSTANCE.releaseTempTable(
						tableInfo,
						afterUseAction,
						ddlTransactionHandling,
						session
				);
			}
		};
	}

	@Override
	public DeleteHandler buildDeleteHandler(SessionFactoryImplementor factory, HqlSqlWalker walker) {
		final DeleteStatement updateStatement = (DeleteStatement) walker.getAST();

		final FromElement fromElement = updateStatement.getFromClause().getFromElement();
		final Queryable targetedPersister = fromElement.getQueryable();

		final IdTableInfoImpl tableInfo = getIdTableInfo( targetedPersister );

		return new TableBasedDeleteHandlerImpl( factory, walker, tableInfo ) {
			@Override
			protected void prepareForUse(Queryable persister, SessionImplementor session) {
				Helper.INSTANCE.createTempTable(
						tableInfo,
						ddlTransactionHandling,
						session
				);
			}

			@Override
			protected void releaseFromUse(Queryable persister, SessionImplementor session) {
				Helper.INSTANCE.releaseTempTable(
						tableInfo,
						afterUseAction,
						ddlTransactionHandling,
						session
				);
			}
		};
	}

}
