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
import java.util.List;

import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.persister.collection.AbstractCollectionPersister;
import org.hibernate.sql.Delete;
import org.hibernate.type.CollectionType;
import org.hibernate.type.Type;

/**
 * Bulk-id delete handler that uses CTE and VALUES lists.
 *
 * @author Evandro Pires da Silva
 * @author Vlad Mihalcea
 */
public class CteValuesListDeleteHandlerImpl
		extends AbstractCteValuesListBulkIdHandler
		implements MultiTableBulkIdStrategy.DeleteHandler {

	private final List<String> deletes = new ArrayList<>();

	public CteValuesListDeleteHandlerImpl(
			SessionFactoryImplementor factory,
			HqlSqlWalker walker) {
		this( factory, walker, null, null );
	}

	public CteValuesListDeleteHandlerImpl(
			SessionFactoryImplementor factory,
			HqlSqlWalker walker,
			String catalog,
			String schema) {
		super( factory, walker, catalog, schema );

		final String idSubselect = generateIdSubselect( getTargetedQueryable() );

		for ( Type type : getTargetedQueryable().getPropertyTypes() ) {
			if ( type.isCollectionType() ) {
				CollectionType cType = (CollectionType) type;
				AbstractCollectionPersister cPersister = (AbstractCollectionPersister) factory.getMetamodel().collectionPersister( cType.getRole() );
				if ( cPersister.isManyToMany() ) {
					deletes.add( generateDelete(
							cPersister.getTableName(),
							cPersister.getKeyColumnNames(),
							generateIdSubselect( idSubselect, getTargetedQueryable(), cPersister ),
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
			deletes.add( generateDelete( tableNames[i], columnNames[i], idSubselect, "bulk delete" ) );
		}
	}

	@Override
	public int execute(
			SharedSessionContractImplementor session,
			QueryParameters queryParameters) {

		CteValuesListBuilder values = prepareCteStatement( session, queryParameters );

		if ( !values.getIds().isEmpty() ) {
			// Start performing the deletes
			for ( String deleteSuffix : deletes ) {
				if ( deleteSuffix == null) {
					continue;
				}

				String delete = values.toStatement( deleteSuffix );

				try {
					try ( PreparedStatement ps = session
							.getJdbcCoordinator().getStatementPreparer()
							.prepareStatement( delete, false ) ) {
						int pos = 1;
						for ( Object[] result : values.getIds() ) {
							for ( Object column : result ) {
								ps.setObject( pos++, column );
							}
						}
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

	private String generateDelete(
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
		return delete.toStatementString();
	}

	@Override
	public String[] getSqlStatements() {
		return deletes.toArray( new String[deletes.size()] );
	}
}
