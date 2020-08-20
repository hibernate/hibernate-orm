/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.spi.id;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.internal.ast.tree.AssignmentSpecification;
import org.hibernate.hql.internal.ast.tree.FromElement;
import org.hibernate.hql.internal.ast.tree.UpdateStatement;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.param.ParameterSpecification;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.InsertSelect;
import org.hibernate.sql.Update;

import org.jboss.logging.Logger;

/**
* @author Steve Ebersole
* @author Nathan Xu
*/
public class TableBasedUpdateHandlerImpl
		extends AbstractTableBasedBulkIdHandler
		implements MultiTableBulkIdStrategy.UpdateHandler {

	private static final Logger log = Logger.getLogger( TableBasedUpdateHandlerImpl.class );

	private final Queryable targetedPersister;

	private final String idInsertSelectSqls;
	private final List<ParameterSpecification> idSelectParameterSpecifications;

	// TODO: replace different processing based on 'isSingleTableUpdate' with different child classes
	private final boolean isSingleTableUpdate;

	private final String[] updateSqls;
	private final ParameterSpecification[][] assignmentParameterSpecifications;

	@SuppressWarnings("unchecked")
	public TableBasedUpdateHandlerImpl(
			SessionFactoryImplementor factory,
			HqlSqlWalker walker,
			IdTableInfo idTableInfo) {
		super( factory, walker );

		final Dialect dialect = factory.getJdbcServices().getJdbcEnvironment().getDialect();
		final UpdateStatement updateStatement = (UpdateStatement) walker.getAST();
		final FromElement fromElement = updateStatement.getFromClause().getFromElement();
		this.targetedPersister = fromElement.getQueryable();
		final String bulkTargetAlias = fromElement.getTableAlias();
		final ProcessedWhereClause processedWhereClause = processWhereClause( updateStatement.getWhereClause() );

		boolean isSingleTableUpdate = false;
		final Set<Serializable> querySpaces = fromElement.getWalker().getQuerySpaces();

		// optimizaiton note: we can reuse the code for case 2 below for case 1, but the below code will produce more compact SQL
		if ( querySpaces.size() == 1 ) {
			// single-table update case 1: no other table is involved in either 'set' clause or 'where' clause
			isSingleTableUpdate = true;
			final Update update = new Update( dialect ).setTableName( (String) querySpaces.iterator().next() );
			final String whereClauseFragment = StringHelper.replace( processedWhereClause.getUserWhereClauseFragment(), bulkTargetAlias + ".", "" );
			if ( !whereClauseFragment.isEmpty() ) {
				update.setWhere( whereClauseFragment );
			}
			final List<AssignmentSpecification> assignmentSpecifications = walker.getAssignmentSpecifications();
			final List<ParameterSpecification> parameterList = new ArrayList<>();
			for ( AssignmentSpecification assignmentSpecification : assignmentSpecifications ) {
				update.appendAssignmentFragment( assignmentSpecification.getSqlAssignmentFragment() );
				if ( assignmentSpecification.getParameters() != null ) {
					Collections.addAll( parameterList, assignmentSpecification.getParameters() );
				}
			}

			final String updateSql = update.toStatementString();

			log.tracev( "Skipped ID Table usage (single-table update) : {0}", updateSql );

			this.updateSqls = new String[] { updateSql };
			parameterList.addAll( processedWhereClause.getIdSelectParameterSpecifications() );
			this.assignmentParameterSpecifications = new ParameterSpecification[][] { parameterList.toArray( new ParameterSpecification[0] ) };

			this.idSelectParameterSpecifications = null;
			this.idInsertSelectSqls = null;
		}
		else {
			this.idSelectParameterSpecifications = processedWhereClause.getIdSelectParameterSpecifications();

			final InsertSelect idInsertSelect = generateIdInsertSelect(
					bulkTargetAlias,
					idTableInfo,
					processedWhereClause
			);
			this.idInsertSelectSqls = idInsertSelect.toStatementString();

			log.tracev( "Generated ID-INSERT-SELECT SQL (multi-table update) : {0}", idInsertSelect );

			String[] tableNames = targetedPersister.getConstraintOrderedTableNameClosure();
			String[][] columnNames = targetedPersister.getContraintOrderedTableKeyColumnClosure();
			String idSubselect = generateIdSubselect( targetedPersister, idTableInfo );

			this.updateSqls = new String[tableNames.length];
			this.assignmentParameterSpecifications = new ParameterSpecification[tableNames.length][];

			int affectedCount = 0;
			int lastAffectingTableIndex = -1;
			Update lastAffectingUpdate = null;
			for ( int tableIndex = 0; tableIndex < tableNames.length; tableIndex++ ) {
				boolean affected = false;
				final List<ParameterSpecification> parameterList = new ArrayList<>();
				final Update update = new Update( dialect )
						.setTableName( tableNames[tableIndex] )
						.setWhere( "(" + String.join( ", ", columnNames[tableIndex] ) + ") IN (" + idSubselect + ")" );
				if ( factory().getSessionFactoryOptions().isCommentsEnabled() ) {
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
					affectedCount++;
					lastAffectingTableIndex = tableIndex;
					lastAffectingUpdate = update;
					updateSqls[tableIndex] = update.toStatementString();
					assignmentParameterSpecifications[tableIndex] = parameterList.toArray( new ParameterSpecification[0] );
				}
			}

			// single-table update case 2: other tables are involved in either 'set' clause or 'where' clause
			if ( affectedCount == 1 ) {
				isSingleTableUpdate = true;
				lastAffectingUpdate.setWhere( "(" + String.join( ", ", columnNames[lastAffectingTableIndex] ) + ") IN (" + idInsertSelect.getSelect().toStatementString() + ")" );
				final String updateSql = lastAffectingUpdate.toStatementString();

				log.tracev( "Skipped ID Table usage (single-table update) : {0}", updateSql );

				updateSqls[lastAffectingTableIndex] = updateSql;
				assignmentParameterSpecifications[lastAffectingTableIndex] = ArrayHelper.join(
						assignmentParameterSpecifications[lastAffectingTableIndex],
						idSelectParameterSpecifications.toArray( new ParameterSpecification[0] )
				);
			}
		}
		this.isSingleTableUpdate = isSingleTableUpdate;
	}

	@Override
	public Queryable getTargetedQueryable() {
		return targetedPersister;
	}

	@Override
	public String[] getSqlStatements() {
		return updateSqls;
	}

	@Override
	public int execute(SharedSessionContractImplementor session, QueryParameters queryParameters) {
		if ( !isSingleTableUpdate ) {
			prepareForUse( targetedPersister, session );
		}
		try {
			// First, save off the pertinent ids, as the return value
			PreparedStatement ps = null;
			int idTableCount = 0;

			if ( !isSingleTableUpdate ) {
				try {
					try {
						ps = session.getJdbcCoordinator().getStatementPreparer().prepareStatement(
								idInsertSelectSqls,
								false
						);
						int position = 1;
						position += handlePrependedParametersOnIdSelection( ps, session, position );
						for ( ParameterSpecification parameterSpecification : idSelectParameterSpecifications ) {
							position += parameterSpecification.bind( ps, queryParameters, session, position );
						}
						idTableCount = session.getJdbcCoordinator().getResultSetReturn().executeUpdate( ps );
					}
					finally {
						if ( ps != null ) {
							session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( ps );
							session.getJdbcCoordinator().afterStatementExecution();
						}
					}
				}
				catch (SQLException e) {
					throw session.getJdbcServices().getSqlExceptionHelper().convert(
							e,
							"could not insert/select ids for bulk update",
							idInsertSelectSqls
					);
				}
			}

			// Start performing the updates
			int updateResultCount = 0;
			for ( int i = 0; i < updateSqls.length; i++ ) {
				if ( updateSqls[i] == null ) {
					continue;
				}
				try {
					try {
						ps = session.getJdbcCoordinator().getStatementPreparer().prepareStatement( updateSqls[i], false );
						if ( assignmentParameterSpecifications[i] != null ) {
							int position = 1; // jdbc params are 1-based
							for ( ParameterSpecification assignmentParameterSpecification : assignmentParameterSpecifications[i] ) {
								position += assignmentParameterSpecification
										.bind( ps, queryParameters, session, position );
							}
							handleAddedParametersOnUpdate( ps, session, position );
						}
						updateResultCount += session.getJdbcCoordinator().getResultSetReturn().executeUpdate( ps );
					}
					finally {
						if ( ps != null ) {
							session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( ps );
							session.getJdbcCoordinator().afterStatementExecution();
						}
					}
				}
				catch( SQLException e ) {
					throw session.getJdbcServices().getSqlExceptionHelper().convert( e, "error performing bulk update", updateSqls[i] );
				}
			}

			return isSingleTableUpdate ? updateResultCount : idTableCount;
		}
		finally {
			if ( !isSingleTableUpdate) {
				releaseFromUse( targetedPersister, session );
			}
		}
	}

	protected int handlePrependedParametersOnIdSelection(PreparedStatement ps, SharedSessionContractImplementor session, int pos) throws SQLException {
		return 0;
	}

	protected void handleAddedParametersOnUpdate(PreparedStatement ps, SharedSessionContractImplementor session, int position) throws SQLException {
	}
}
