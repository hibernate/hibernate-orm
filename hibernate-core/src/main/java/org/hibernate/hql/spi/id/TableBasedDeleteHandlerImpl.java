/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.spi.id;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.internal.ast.tree.DeleteStatement;
import org.hibernate.hql.internal.ast.tree.FromElement;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.param.ParameterSpecification;
import org.hibernate.persister.collection.AbstractCollectionPersister;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.Delete;
import org.hibernate.type.CollectionType;
import org.hibernate.type.Type;

import org.jboss.logging.Logger;

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
	private final List<String> deletes;

	public TableBasedDeleteHandlerImpl(
			SessionFactoryImplementor factory,
			HqlSqlWalker walker,
			IdTableInfo idTableInfo) {
		super( factory, walker );

		DeleteStatement deleteStatement = ( DeleteStatement ) walker.getAST();
		FromElement fromElement = deleteStatement.getFromClause().getFromElement();

		this.targetedPersister = fromElement.getQueryable();
		final String bulkTargetAlias = fromElement.getTableAlias();

		final ProcessedWhereClause processedWhereClause = processWhereClause( deleteStatement.getWhereClause() );
		this.idSelectParameterSpecifications = processedWhereClause.getIdSelectParameterSpecifications();
		this.idInsertSelect = generateIdInsertSelect( bulkTargetAlias, idTableInfo, processedWhereClause );
		log.tracev( "Generated ID-INSERT-SELECT SQL (multi-table delete) : {0}", idInsertSelect );
		
		final String idSubselect = generateIdSubselect( targetedPersister, idTableInfo );
		deletes = new ArrayList<>();
		
		// If many-to-many, delete the FK row in the collection table.
		// This partially overlaps with DeleteExecutor, but it instead uses the temp table in the idSubselect.
		for ( Type type : targetedPersister.getPropertyTypes() ) {
			if ( type.isCollectionType() ) {
				CollectionType cType = (CollectionType) type;
				AbstractCollectionPersister cPersister = (AbstractCollectionPersister) factory.getMetamodel().collectionPersister( cType.getRole() );
				if ( cPersister.isManyToMany() ) {
					deletes.add( generateDelete( cPersister.getTableName(),
							cPersister.getKeyColumnNames(), idSubselect, "bulk delete - m2m join table cleanup"));
				}
			}
		}

		String[] tableNames = targetedPersister.getConstraintOrderedTableNameClosure();
		String[][] columnNames = targetedPersister.getContraintOrderedTableKeyColumnClosure();
		for ( int i = 0; i < tableNames.length; i++ ) {
			// TODO : an optimization here would be to consider cascade deletes and not gen those delete statements;
			//      the difficulty is the ordering of the tables here vs the cascade attributes on the persisters ->
			//          the table info gotten here should really be self-contained (i.e., a class representation
			//          defining all the needed attributes), then we could then get an array of those
			deletes.add( generateDelete( tableNames[i], columnNames[i], idSubselect, "bulk delete"));
		}
	}
	
	private String generateDelete(String tableName, String[] columnNames, String idSubselect, String comment) {
		final Delete delete = new Delete()
				.setTableName( tableName )
				.setWhere( "(" + StringHelper.join( ", ", columnNames ) + ") IN (" + idSubselect + ")" );
		if ( factory().getSessionFactoryOptions().isCommentsEnabled() ) {
			delete.setComment( comment );
		}
		return delete.toStatementString();
	}

	@Override
	public Queryable getTargetedQueryable() {
		return targetedPersister;
	}

	@Override
	public String[] getSqlStatements() {
		return deletes.toArray( new String[deletes.size()] );
	}

	@Override
	public int execute(SharedSessionContractImplementor session, QueryParameters queryParameters) {
		prepareForUse( targetedPersister, session );
		try {
			PreparedStatement ps = null;
			int resultCount = 0;
			try {
				try {
					ps = session.getJdbcCoordinator().getStatementPreparer().prepareStatement( idInsertSelect, false );
					int pos = 1;
					pos += handlePrependedParametersOnIdSelection( ps, session, pos );
					for ( ParameterSpecification parameterSpecification : idSelectParameterSpecifications ) {
						pos += parameterSpecification.bind( ps, queryParameters, session, pos );
					}
					resultCount = session.getJdbcCoordinator().getResultSetReturn().executeUpdate( ps );
				}
				finally {
					if ( ps != null ) {
						session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( ps );
						session.getJdbcCoordinator().afterStatementExecution();
					}
				}
			}
			catch( SQLException e ) {
				throw session.getJdbcServices().getSqlExceptionHelper().convert( e, "could not insert/select ids for bulk delete", idInsertSelect );
			}

			// Start performing the deletes
			for ( String delete : deletes ) {
				try {
					try {
						ps = session
								.getJdbcCoordinator()
								.getStatementPreparer()
								.prepareStatement( delete, false );
						handleAddedParametersOnDelete( ps, session );
						session.getJdbcCoordinator().getResultSetReturn().executeUpdate( ps );
					}
					finally {
						if ( ps != null ) {
							session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( ps );
							session.getJdbcCoordinator().afterStatementExecution();
						}
					}
				}
				catch (SQLException e) {
					throw session.getJdbcServices().getSqlExceptionHelper().convert( e, "error performing bulk delete", delete );
				}
			}

			return resultCount;

		}
		finally {
			releaseFromUse( targetedPersister, session );
		}
	}

	protected int handlePrependedParametersOnIdSelection(PreparedStatement ps, SharedSessionContractImplementor session, int pos) throws SQLException {
		return 0;
	}

	protected void handleAddedParametersOnDelete(PreparedStatement ps, SharedSessionContractImplementor session) throws SQLException {
	}
}
