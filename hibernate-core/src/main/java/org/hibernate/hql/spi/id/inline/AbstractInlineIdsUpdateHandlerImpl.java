/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.spi.id.inline;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.internal.ast.tree.AssignmentSpecification;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.param.ParameterSpecification;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.Update;

/**
 * Inline bulk-id delete handler that selects the identifiers of the rows to be deleted.
 *
 * @author Vlad Mihalcea
 */
public abstract class AbstractInlineIdsUpdateHandlerImpl
		extends AbstractInlineIdsBulkIdHandler
		implements MultiTableBulkIdStrategy.UpdateHandler {

	private final Map<Integer, String> updates = new LinkedHashMap<>();

	private ParameterSpecification[][] assignmentParameterSpecifications;

	public AbstractInlineIdsUpdateHandlerImpl(
			SessionFactoryImplementor factory,
			HqlSqlWalker walker) {
		super( factory, walker );
	}

	@Override
	public String[] getSqlStatements() {
		return updates.values().toArray( new String[updates.values().size()] );
	}

	@Override
	public int execute(
			SharedSessionContractImplementor session,
			QueryParameters queryParameters) {

		IdsClauseBuilder values = prepareInlineStatement( session, queryParameters );

		if ( !values.getIds().isEmpty() ) {

			final Queryable targetedQueryable = getTargetedQueryable();
			String[] tableNames = targetedQueryable.getConstraintOrderedTableNameClosure();
			String[][] columnNames = targetedQueryable.getContraintOrderedTableKeyColumnClosure();

			String idSubselect = values.toStatement();

			assignmentParameterSpecifications = new ParameterSpecification[tableNames.length][];
			for ( int tableIndex = 0; tableIndex < tableNames.length; tableIndex++ ) {
				boolean affected = false;
				final List<ParameterSpecification> parameterList = new ArrayList<>();

				Update update = generateUpdate( tableNames[tableIndex], columnNames[tableIndex], idSubselect, "bulk update" );

				final List<AssignmentSpecification> assignmentSpecifications = walker().getAssignmentSpecifications();
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
					updates.put( tableIndex, update.toStatementString() );
					assignmentParameterSpecifications[tableIndex] = parameterList.toArray( new ParameterSpecification[parameterList.size()] );
				}
			}

			final JdbcCoordinator jdbcCoordinator = session
					.getJdbcCoordinator();
			// Start performing the updates
			for ( Map.Entry<Integer, String> updateEntry: updates.entrySet()) {
				int i = updateEntry.getKey();
				String update = updateEntry.getValue();

				if ( update == null) {
					continue;
				}
				try {
					try (PreparedStatement ps = jdbcCoordinator.getStatementPreparer()
							.prepareStatement( update, false )) {
						int position = 1; // jdbc params are 1-based
						if ( assignmentParameterSpecifications[i] != null ) {
							for ( ParameterSpecification assignmentParameterSpecification : assignmentParameterSpecifications[i] ) {
								position += assignmentParameterSpecification
										.bind( ps, queryParameters, session, position );
							}
						}
						jdbcCoordinator.getResultSetReturn()
								.executeUpdate( ps );
					}
				}
				catch ( SQLException e ) {
					throw convert(
							e,
							"error performing bulk update",
							update
					);
				}
			}
		}

		return values.getIds().size();
	}

	protected Update generateUpdate(
			String tableName,
			String[] columnNames,
			String idSubselect,
			String comment) {
		final Update update = new Update( factory().getServiceRegistry().getService( JdbcServices.class ).getDialect() )
				.setTableName( tableName )
				.setWhere( "(" + String.join( ", ", (CharSequence[]) columnNames ) + ") in (" + idSubselect + ")" );
		if ( factory().getSessionFactoryOptions().isCommentsEnabled() ) {
			update.setComment( comment );
		}
		return update;
	}
}
