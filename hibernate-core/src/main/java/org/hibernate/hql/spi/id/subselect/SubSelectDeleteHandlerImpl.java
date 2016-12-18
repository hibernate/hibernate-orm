/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.spi.id.subselect;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.internal.ast.tree.AbstractRestrictableStatement;
import org.hibernate.hql.internal.ast.tree.FromElement;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.param.ParameterSpecification;
import org.hibernate.persister.collection.AbstractCollectionPersister;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.Delete;
import org.hibernate.type.CollectionType;
import org.hibernate.type.Type;

/**
 * @author Evandro Pires da Silva
 * @author Vlad Mihalcea
 */
public class SubSelectDeleteHandlerImpl
		extends AbstractSubSelectBasedBulkIdHandler
		implements MultiTableBulkIdStrategy.DeleteHandler {

	private final Queryable targetedPersister;

	private final String idSelect;
	private final List<ParameterSpecification> idSelectParameterSpecifications;

	private final List<String> deletes = new ArrayList<>();

	public SubSelectDeleteHandlerImpl(
			SessionFactoryImplementor factory,
			HqlSqlWalker walker) {
		super( factory, walker);

		final AbstractRestrictableStatement statement = (AbstractRestrictableStatement) walker.getAST();
		final FromElement fromElement = statement.getFromClause().getFromElement();

		this.targetedPersister = fromElement.getQueryable();

		final ProcessedWhereClause processedWhereClause = processWhereClause( statement.getWhereClause() );
		final String bulkTargetAlias = fromElement.getTableAlias();

		this.idSelect = generateIdSubSelect( bulkTargetAlias, processedWhereClause );
		this.idSelectParameterSpecifications = processedWhereClause.getIdSelectParameterSpecifications();

		// If many-to-many, delete the FK row in the collection table.
		// This partially overlaps with DeleteExecutor, but it instead uses the
		// temp table in the idSubselect.
		for ( Type type : getTargetedQueryable().getPropertyTypes() ) {
			if ( type.isCollectionType() ) {
				CollectionType cType = (CollectionType) type;
				AbstractCollectionPersister cPersister = (AbstractCollectionPersister) factory.getCollectionPersister( cType.getRole() );
				if ( cPersister.isManyToMany() ) {
					deletes.add( generateDelete(
							cPersister.getTableName(),
							cPersister.getKeyColumnNames(),
							idSelect,
							"bulk delete - m2m join table cleanup"
					) );
				}
			}
		}

		String[] tableNames = getTargetedQueryable().getConstraintOrderedTableNameClosure();
		String[][] columnNames = getTargetedQueryable().getContraintOrderedTableKeyColumnClosure();
		for ( int i = 0; i < tableNames.length; i++ ) {
			// TODO : an optimization here would be to consider cascade deletes and not gen those delete statements;
			//      the difficulty is the ordering of the tables here vs the cascade attributes on the persisters ->
			//          the table info gotten here should really be self-contained (i.e., a class representation
			//          defining all the needed attributes), then we could then get an array of those
			deletes.add( generateDelete( tableNames[i], columnNames[i], idSelect, "bulk delete" ) );
		}
	}

	@Override
	public Queryable getTargetedQueryable() {
		return targetedPersister;
	}

	@Override
	public int execute(
			SharedSessionContractImplementor session,
			QueryParameters queryParameters) {
		int resultCount = 0;

		for ( String delete : deletes ) {
			if ( delete == null) {
				continue;
			}
			try {
				try ( PreparedStatement ps = session
						.getJdbcCoordinator().getStatementPreparer().prepareStatement( delete, false ) ) {
					int position = 1; // jdbc params are 1-based
					for ( ParameterSpecification parameterSpecification : idSelectParameterSpecifications ) {
						position += parameterSpecification.bind( ps, queryParameters, session, position );
					}
					resultCount = session
							.getJdbcCoordinator().getResultSetReturn()
							.executeUpdate( ps );
				}
			}
			catch ( SQLException e ) {
				throw session.getJdbcServices().getSqlExceptionHelper().convert( e, "error performing bulk delete", delete );
			}
		}

		return resultCount;
	}

	private String generateDelete(
			String tableName,
			String[] columnNames,
			String idSubselect,
			String comment) {
		final Delete delete = new Delete().setTableName( tableName ).setWhere(
				"(" + StringHelper.join( ", ", columnNames ) + ") in ("
						+ idSubselect + ")" );
		if ( factory().getSessionFactoryOptions().isCommentsEnabled() ) {
			delete.setComment( comment );
		}
		return delete.toStatementString();
	}

	@Override
	public String[] getSqlStatements() {
		return deletes.toArray( new String[deletes.size()] );
	}
}
