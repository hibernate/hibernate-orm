/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.spi.id.cte;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.internal.ast.tree.AssignmentSpecification;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.param.ParameterSpecification;
import org.hibernate.sql.Update;

/**
 * @author Evandro Pires da Silva
 * @author Vlad Mihalcea
 */
public class ValuesListCteBasedUpdateHandlerImpl
		extends AbstractValuesListCteBasedBulkIdHandler
		implements MultiTableBulkIdStrategy.UpdateHandler {

	private final String[] updates;
	private final ParameterSpecification[][] assignmentParameterSpecifications;

	public ValuesListCteBasedUpdateHandlerImpl(
			SessionFactoryImplementor factory,
			HqlSqlWalker walker) {
		this( factory, walker, null, null );
	}

	public ValuesListCteBasedUpdateHandlerImpl(
			SessionFactoryImplementor factory,
			HqlSqlWalker walker,
			String catalog,
			String schema) {
		super( factory, walker, catalog, schema );

		String[] tableNames = getTargetedQueryable().getConstraintOrderedTableNameClosure();
		String[][] columnNames = getTargetedQueryable().getContraintOrderedTableKeyColumnClosure();
		String idSubselect = generateIdSubselect( getTargetedQueryable() );

		updates = new String[tableNames.length];
		assignmentParameterSpecifications = new ParameterSpecification[tableNames.length][];
		for ( int tableIndex = 0; tableIndex < tableNames.length; tableIndex++ ) {
			boolean affected = false;
			final List<ParameterSpecification> parameterList = new ArrayList<>();
			final Update update = new Update( factory().getDialect() )
					.setTableName( tableNames[tableIndex] )
					.setWhere( "(" + StringHelper.join( ", ", columnNames[tableIndex] ) + ") in (" + idSubselect + ")" );
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
				updates[tableIndex] = update.toStatementString();
				assignmentParameterSpecifications[tableIndex] = parameterList.toArray( new ParameterSpecification[parameterList.size()] );
			}
		}
	}

	@Override
	public String[] getSqlStatements() {
		return updates;
	}

	@Override
	public int execute(
			SharedSessionContractImplementor session,
			QueryParameters queryParameters) {
		// First, save off the pertinent ids, as the return value
		int resultCount = 0;

		ValuesListCteBuilder values = prepareCteStatement( session, queryParameters );

		if ( values != null ) {

			// Start performing the updates
			for ( int i = 0; i < updates.length; i++ ) {
				String updateSuffix = updates[i];
				if ( updateSuffix == null) {
					continue;
				}
				String update = values.toStatement( updateSuffix );
				try {
					try (PreparedStatement ps = session
							.getJdbcCoordinator().getStatementPreparer()
							.prepareStatement( update, false )) {
						int position = 1; // jdbc params are 1-based
						for ( Object[] result : values.getIds() ) {
							for ( Object column : result ) {
								ps.setObject( position++, column );
							}
						}
						if ( assignmentParameterSpecifications[i] != null ) {
							for ( int x = 0; x < assignmentParameterSpecifications[i].length; x++ ) {
								position += assignmentParameterSpecifications[i][x]
										.bind( ps, queryParameters, session,
											   position
										);
							}
						}
						resultCount = session
								.getJdbcCoordinator().getResultSetReturn()
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

		return resultCount;
	}
}
