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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.jboss.logging.Logger;

import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.internal.ast.tree.DeleteStatement;
import org.hibernate.hql.internal.ast.tree.FromElement;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.param.ParameterSpecification;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.Delete;

/**
* @author Steve Ebersole
*/
public class TableBasedDeleteHandlerImpl
		extends AbstractTableBasedBulkIdHandler
		implements MultiTableBulkIdStrategy.DeleteHandler {
	private static final Logger log = Logger.getLogger( TableBasedDeleteHandlerImpl.class );

	private final Queryable targetedPersister;

	private final String idInsertSelect;
	private final List<ParameterSpecification> idSelectParameterSpecifications;
	private final String[] deletes;

	public TableBasedDeleteHandlerImpl(SessionFactoryImplementor factory, HqlSqlWalker walker) {
		this( factory, walker, null, null );
	}

	public TableBasedDeleteHandlerImpl(
			SessionFactoryImplementor factory,
			HqlSqlWalker walker,
			String catalog,
			String schema) {
		super( factory, walker, catalog, schema );

		DeleteStatement deleteStatement = ( DeleteStatement ) walker.getAST();
		FromElement fromElement = deleteStatement.getFromClause().getFromElement();

		this.targetedPersister = fromElement.getQueryable();
		final String bulkTargetAlias = fromElement.getTableAlias();

		final ProcessedWhereClause processedWhereClause = processWhereClause( deleteStatement.getWhereClause() );
		this.idSelectParameterSpecifications = processedWhereClause.getIdSelectParameterSpecifications();
		this.idInsertSelect = generateIdInsertSelect( targetedPersister, bulkTargetAlias, processedWhereClause );
		log.tracev( "Generated ID-INSERT-SELECT SQL (multi-table delete) : {0}", idInsertSelect );

		String[] tableNames = targetedPersister.getConstraintOrderedTableNameClosure();
		String[][] columnNames = targetedPersister.getContraintOrderedTableKeyColumnClosure();
		String idSubselect = generateIdSubselect( targetedPersister );

		deletes = new String[tableNames.length];
		for ( int i = tableNames.length - 1; i >= 0; i-- ) {
			// TODO : an optimization here would be to consider cascade deletes and not gen those delete statements;
			//      the difficulty is the ordering of the tables here vs the cascade attributes on the persisters ->
			//          the table info gotten here should really be self-contained (i.e., a class representation
			//          defining all the needed attributes), then we could then get an array of those
			final Delete delete = new Delete()
					.setTableName( tableNames[i] )
					.setWhere( "(" + StringHelper.join( ", ", columnNames[i] ) + ") IN (" + idSubselect + ")" );
			if ( factory().getSettings().isCommentsEnabled() ) {
				delete.setComment( "bulk delete" );
			}

			deletes[i] = delete.toStatementString();
		}
	}

	@Override
	public Queryable getTargetedQueryable() {
		return targetedPersister;
	}

	@Override
	public String[] getSqlStatements() {
		return deletes;
	}

	@Override
	public int execute(SessionImplementor session, QueryParameters queryParameters) {
		prepareForUse( targetedPersister, session );
		try {
			PreparedStatement ps = null;
			int resultCount = 0;
			try {
				try {
					ps = session.getTransactionCoordinator().getJdbcCoordinator().getStatementPreparer().prepareStatement( idInsertSelect, false );
					int pos = 1;
					pos += handlePrependedParametersOnIdSelection( ps, session, pos );
					for ( ParameterSpecification parameterSpecification : idSelectParameterSpecifications ) {
						pos += parameterSpecification.bind( ps, queryParameters, session, pos );
					}
					resultCount = ps.executeUpdate();
				}
				finally {
					if ( ps != null ) {
						ps.close();
					}
				}
			}
			catch( SQLException e ) {
				throw convert( e, "could not insert/select ids for bulk delete", idInsertSelect );
			}

			// Start performing the deletes
			for ( String delete : deletes ) {
				try {
					try {
						ps = session.getTransactionCoordinator()
								.getJdbcCoordinator()
								.getStatementPreparer()
								.prepareStatement( delete, false );
						handleAddedParametersOnDelete( ps, session );
						ps.executeUpdate();
					}
					finally {
						if ( ps != null ) {
							ps.close();
						}
					}
				}
				catch (SQLException e) {
					throw convert( e, "error performing bulk delete", delete );
				}
			}

			return resultCount;

		}
		finally {
			releaseFromUse( targetedPersister, session );
		}
	}

	protected int handlePrependedParametersOnIdSelection(PreparedStatement ps, SessionImplementor session, int pos) throws SQLException {
		return 0;
	}

	protected void handleAddedParametersOnDelete(PreparedStatement ps, SessionImplementor session) throws SQLException {
	}
}
