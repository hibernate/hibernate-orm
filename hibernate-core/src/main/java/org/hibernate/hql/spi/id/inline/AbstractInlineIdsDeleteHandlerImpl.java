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
import java.util.Arrays;
import java.util.List;

import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.persister.collection.AbstractCollectionPersister;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.Delete;
import org.hibernate.type.CollectionType;
import org.hibernate.type.Type;

/**
 * Inline bulk-id delete handler that selects the identifiers of the rows to be updated.
 *
 * @author Vlad Mihalcea
 */
public abstract class AbstractInlineIdsDeleteHandlerImpl
		extends AbstractInlineIdsBulkIdHandler
		implements MultiTableBulkIdStrategy.DeleteHandler {

	private final List<String> deletes = new ArrayList<>();

	public AbstractInlineIdsDeleteHandlerImpl(
			SessionFactoryImplementor factory,
			HqlSqlWalker walker) {
		super( factory, walker );
	}

	@Override
	public String[] getSqlStatements() {
		return deletes.toArray( new String[deletes.size()] );
	}

	@Override
	public int execute(
			SharedSessionContractImplementor session,
			QueryParameters queryParameters) {

		IdsClauseBuilder values = prepareInlineStatement( session, queryParameters );

		if ( !values.getIds().isEmpty() ) {
			final String idSubselect = values.toStatement();

			for ( Type type : getTargetedQueryable().getPropertyTypes() ) {
				if ( type.isCollectionType() ) {
					CollectionType cType = (CollectionType) type;
					AbstractCollectionPersister cPersister = (AbstractCollectionPersister) factory().getMetamodel().collectionPersister( cType.getRole() );
					if ( cPersister.isManyToMany() ) {
						deletes.add( generateDelete(
								cPersister.getTableName(),
								cPersister.getKeyColumnNames(),
								generateIdSubselect( idSubselect, getTargetedQueryable(), cPersister ),
								"bulk delete - m2m join table cleanup"
						).toStatementString() );
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
				deletes.add( generateDelete( tableNames[i], columnNames[i], idSubselect, "bulk delete" ).toStatementString() );
			}

			// Start performing the deletes
			for ( String delete : deletes ) {
				if ( delete == null) {
					continue;
				}

				try {
					try ( PreparedStatement ps = session
							.getJdbcCoordinator().getStatementPreparer()
							.prepareStatement( delete, false ) ) {
						session
								.getJdbcCoordinator().getResultSetReturn()
								.executeUpdate( ps );
					}
				}
				catch ( SQLException e ) {
					throw convert( e, "error performing bulk delete", delete );
				}
			}
		}

		return values.getIds().size();
	}

	protected String generateIdSubselect(String idSubselect, Queryable persister, AbstractCollectionPersister cPersister) {
		String[] columnNames = getKeyColumnNames( persister, cPersister );
		// If the column names are equal to the identifier column names, just return the idSubselect
		if ( Arrays.equals(getTargetedQueryable().getIdentifierColumnNames(), columnNames ) ) {
			return idSubselect;
		}

		// Otherwise, we need to fetch the key column names from the original table
		// Unfortunately, this is a bit more work, as only the identifiers are fetched
		// It would be great if we could adapt #selectIds to fetch key columns as well
		StringBuilder selectBuilder = new StringBuilder();
		selectBuilder.append( "select " );
		appendJoined( ", ", columnNames, selectBuilder );
		selectBuilder.append( " from " ).append( getTargetedQueryable().getTableName() );
		selectBuilder.append( " tmp where (" );
		appendJoined( ", ", getTargetedQueryable().getIdentifierColumnNames(), selectBuilder );
		selectBuilder.append( ") in (" );
		selectBuilder.append( idSubselect );
		selectBuilder.append( ")" );
		return selectBuilder.toString();
	}

	protected Delete generateDelete(
			String tableName,
			String[] columnNames,
			String idSubselect,
			String comment) {
		final Delete delete = new Delete().setTableName( tableName ).setWhere(
				"(" + String.join( ", ", (CharSequence[]) columnNames ) + ") in ("
						+ idSubselect + ")" );
		if ( factory().getSessionFactoryOptions().isCommentsEnabled() ) {
			delete.setComment( comment );
		}
		return delete;
	}
}
