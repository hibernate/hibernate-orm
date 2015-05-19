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
import java.util.Collections;
import java.util.List;

import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.internal.ast.tree.AssignmentSpecification;
import org.hibernate.hql.internal.ast.tree.FromElement;
import org.hibernate.hql.internal.ast.tree.UpdateStatement;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.param.ParameterSpecification;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.Update;

import org.jboss.logging.Logger;

/**
* @author Steve Ebersole
*/
public class TableBasedUpdateHandlerImpl
		extends AbstractTableBasedBulkIdHandler
		implements MultiTableBulkIdStrategy.UpdateHandler {

	private static final Logger log = Logger.getLogger( TableBasedUpdateHandlerImpl.class );

	private final Queryable targetedPersister;

	private final String idInsertSelect;
	private final List<ParameterSpecification> idSelectParameterSpecifications;

	private final String[] updates;
	private final ParameterSpecification[][] assignmentParameterSpecifications;

	@SuppressWarnings("unchecked")
	public TableBasedUpdateHandlerImpl(
			SessionFactoryImplementor factory,
			HqlSqlWalker walker,
			IdTableInfo idTableInfo) {
		super( factory, walker );

		UpdateStatement updateStatement = ( UpdateStatement ) walker.getAST();
		FromElement fromElement = updateStatement.getFromClause().getFromElement();

		this.targetedPersister = fromElement.getQueryable();

		final String bulkTargetAlias = fromElement.getTableAlias();

		final ProcessedWhereClause processedWhereClause = processWhereClause( updateStatement.getWhereClause() );
		this.idSelectParameterSpecifications = processedWhereClause.getIdSelectParameterSpecifications();
		this.idInsertSelect = generateIdInsertSelect( bulkTargetAlias, idTableInfo, processedWhereClause );
		log.tracev( "Generated ID-INSERT-SELECT SQL (multi-table update) : {0}", idInsertSelect );

		String[] tableNames = targetedPersister.getConstraintOrderedTableNameClosure();
		String[][] columnNames = targetedPersister.getContraintOrderedTableKeyColumnClosure();
		String idSubselect = generateIdSubselect( targetedPersister, idTableInfo );

		updates = new String[tableNames.length];
		assignmentParameterSpecifications = new ParameterSpecification[tableNames.length][];
		for ( int tableIndex = 0; tableIndex < tableNames.length; tableIndex++ ) {
			boolean affected = false;
			final List<ParameterSpecification> parameterList = new ArrayList<ParameterSpecification>();
			final Update update = new Update( factory().getDialect() )
					.setTableName( tableNames[tableIndex] )
					.setWhere( "(" + StringHelper.join( ", ", columnNames[tableIndex] ) + ") IN (" + idSubselect + ")" );
			if ( factory().getSettings().isCommentsEnabled() ) {
				update.setComment( "bulk update" );
			}
			final List<AssignmentSpecification> assignmentSpecifications = walker.getAssignmentSpecifications();
			for ( AssignmentSpecification assignmentSpecification : assignmentSpecifications ) {
				if ( assignmentSpecification.affectsTable( tableNames[tableIndex] ) ) {
					affected = true;
					update.appendAssignmentFragment( assignmentSpecification.getSqlAssignmentFragment() );
					if ( assignmentSpecification.getParameters() != null ) {
						Collections.addAll( parameterList, assignmentSpecification.getParameters() );
					}
				}
			}
			if ( affected ) {
				updates[tableIndex] = update.toStatementString();
				assignmentParameterSpecifications[tableIndex] = parameterList.toArray( new ParameterSpecification[parameterList.size()] );
			}
		}
	}

	@Override
	public Queryable getTargetedQueryable() {
		return targetedPersister;
	}

	@Override
	public String[] getSqlStatements() {
		return updates;
	}

	@Override
	public int execute(SessionImplementor session, QueryParameters queryParameters) {
		prepareForUse( targetedPersister, session );
		try {
			// First, save off the pertinent ids, as the return value
			PreparedStatement ps = null;
			int resultCount = 0;
			try {
				try {
					ps = session.getJdbcCoordinator().getStatementPreparer().prepareStatement( idInsertSelect, false );
					int sum = 1;
					sum += handlePrependedParametersOnIdSelection( ps, session, sum );
					for ( ParameterSpecification parameterSpecification : idSelectParameterSpecifications ) {
						sum += parameterSpecification.bind( ps, queryParameters, session, sum );
					}
					resultCount = session.getJdbcCoordinator().getResultSetReturn().executeUpdate( ps );
				}
				finally {
					if ( ps != null ) {
						session.getJdbcCoordinator().getResourceRegistry().release( ps );
						session.getJdbcCoordinator().afterStatementExecution();
					}
				}
			}
			catch( SQLException e ) {
				throw convert( e, "could not insert/select ids for bulk update", idInsertSelect );
			}

			// Start performing the updates
			for ( int i = 0; i < updates.length; i++ ) {
				if ( updates[i] == null ) {
					continue;
				}
				try {
					try {
						ps = session.getJdbcCoordinator().getStatementPreparer().prepareStatement( updates[i], false );
						if ( assignmentParameterSpecifications[i] != null ) {
							int position = 1; // jdbc params are 1-based
							for ( int x = 0; x < assignmentParameterSpecifications[i].length; x++ ) {
								position += assignmentParameterSpecifications[i][x].bind( ps, queryParameters, session, position );
							}
							handleAddedParametersOnUpdate( ps, session, position );
						}
						session.getJdbcCoordinator().getResultSetReturn().executeUpdate( ps );
					}
					finally {
						if ( ps != null ) {
							session.getJdbcCoordinator().getResourceRegistry().release( ps );
							session.getJdbcCoordinator().afterStatementExecution();
						}
					}
				}
				catch( SQLException e ) {
					throw convert( e, "error performing bulk update", updates[i] );
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

	protected void handleAddedParametersOnUpdate(PreparedStatement ps, SessionImplementor session, int position) throws SQLException {
	}
}
