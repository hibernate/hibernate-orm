/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.PreparedStatement;

import org.hibernate.dialect.identity.HANAIdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.internal.ast.tree.DeleteStatement;
import org.hibernate.hql.internal.ast.tree.FromElement;
import org.hibernate.hql.internal.ast.tree.UpdateStatement;
import org.hibernate.hql.spi.id.IdTableInfo;
import org.hibernate.hql.spi.id.IdTableSupportStandardImpl;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.hql.spi.id.TableBasedDeleteHandlerImpl;
import org.hibernate.hql.spi.id.TableBasedUpdateHandlerImpl;
import org.hibernate.hql.spi.id.global.GlobalTemporaryTableBulkIdStrategy;
import org.hibernate.hql.spi.id.local.AfterUseAction;
import org.hibernate.persister.entity.Queryable;

/**
 * An SQL dialect for HANA. <br/>
 * <a href="http://help.sap.com/hana/html/sqlmain.html">SAP HANA Reference</a> <br/>
 * Column tables are created by this dialect when using the auto-ddl feature.
 * 
 * @author Andrew Clemons <andrew.clemons@sap.com>
 */
public class HANAColumnStoreDialect extends AbstractHANADialect {

	public HANAColumnStoreDialect() {
		super();
	}

	@Override
	public String getCreateTableString() {
		return "create column table";
	}

	@Override
	public MultiTableBulkIdStrategy getDefaultMultiTableBulkIdStrategy() {
		return new GlobalTemporaryTableBulkIdStrategy( new IdTableSupportStandardImpl() {

			@Override
			public String getCreateIdTableCommand() {
				return "create global temporary column table";
			}

		}, AfterUseAction.CLEAN ) {

			@Override
			public DeleteHandler buildDeleteHandler(SessionFactoryImplementor factory, HqlSqlWalker walker) {
				final DeleteStatement updateStatement = (DeleteStatement) walker.getAST();

				final FromElement fromElement = updateStatement.getFromClause().getFromElement();
				final Queryable targetedPersister = fromElement.getQueryable();

				return new TableBasedDeleteHandlerImpl( factory, walker, getIdTableInfo( targetedPersister ) ) {

					@Override
					protected void releaseFromUse(Queryable persister, SharedSessionContractImplementor session) {
						cleanUpRows( ( (IdTableInfo) getIdTableInfo( persister ) ).getQualifiedIdTableName(), session );
					}
				};
			}

			@Override
			public UpdateHandler buildUpdateHandler(SessionFactoryImplementor factory, HqlSqlWalker walker) {
				final UpdateStatement updateStatement = (UpdateStatement) walker.getAST();

				final FromElement fromElement = updateStatement.getFromClause().getFromElement();
				final Queryable targetedPersister = fromElement.getQueryable();

				return new TableBasedUpdateHandlerImpl( factory, walker, getIdTableInfo( targetedPersister ) ) {

					@Override
					protected void releaseFromUse(Queryable persister, SharedSessionContractImplementor session) {
						// clean up our id-table rows
						cleanUpRows( ( (IdTableInfo) getIdTableInfo( persister ) ).getQualifiedIdTableName(), session );
					}
				};
			}

			private void cleanUpRows(String tableName, SharedSessionContractImplementor session) {
				// TODO: delegate to dialect
				final String sql = "truncate table " + tableName;
				PreparedStatement ps = null;
				try {
					ps = session.getJdbcCoordinator().getStatementPreparer().prepareStatement( sql, false );
					session.getJdbcCoordinator().getResultSetReturn().executeUpdate( ps );
				}
				finally {
					if ( ps != null ) {
						try {
							session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( ps );
						}
						catch (Throwable ignore) {
							// ignore
						}
					}
				}
			}
		};
	}
}
