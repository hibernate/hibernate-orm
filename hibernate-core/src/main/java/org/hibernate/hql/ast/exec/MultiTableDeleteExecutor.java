/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.hql.ast.exec;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;

import org.hibernate.HibernateException;
import org.hibernate.engine.QueryParameters;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.exception.JDBCExceptionHelper;
import org.hibernate.hql.ast.HqlSqlWalker;
import org.hibernate.hql.ast.tree.DeleteStatement;
import org.hibernate.hql.ast.tree.FromElement;
import org.hibernate.param.ParameterSpecification;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.Delete;
import org.hibernate.util.StringHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of MultiTableDeleteExecutor.
 *
 * @author Steve Ebersole
 */
public class MultiTableDeleteExecutor extends AbstractStatementExecutor {
	private static final Logger log = LoggerFactory.getLogger( MultiTableDeleteExecutor.class );

	private final Queryable persister;
	private final String idInsertSelect;
	private final String[] deletes;

	public MultiTableDeleteExecutor(HqlSqlWalker walker) {
		super( walker, log );

		if ( !walker.getSessionFactoryHelper().getFactory().getDialect().supportsTemporaryTables() ) {
			throw new HibernateException( "cannot doAfterTransactionCompletion multi-table deletes using dialect not supporting temp tables" );
		}

		DeleteStatement deleteStatement = ( DeleteStatement ) walker.getAST();
		FromElement fromElement = deleteStatement.getFromClause().getFromElement();
		String bulkTargetAlias = fromElement.getTableAlias();
		this.persister = fromElement.getQueryable();

		this.idInsertSelect = generateIdInsertSelect( persister, bulkTargetAlias, deleteStatement.getWhereClause() );
		log.trace( "Generated ID-INSERT-SELECT SQL (multi-table delete) : " +  idInsertSelect );

		String[] tableNames = persister.getConstraintOrderedTableNameClosure();
		String[][] columnNames = persister.getContraintOrderedTableKeyColumnClosure();
		String idSubselect = generateIdSubselect( persister );

		deletes = new String[tableNames.length];
		for ( int i = tableNames.length - 1; i >= 0; i-- ) {
			// TODO : an optimization here would be to consider cascade deletes and not gen those delete statements;
			//      the difficulty is the ordering of the tables here vs the cascade attributes on the persisters ->
			//          the table info gotten here should really be self-contained (i.e., a class representation
			//          defining all the needed attributes), then we could then get an array of those
			final Delete delete = new Delete()
					.setTableName( tableNames[i] )
					.setWhere( "(" + StringHelper.join( ", ", columnNames[i] ) + ") IN (" + idSubselect + ")" );
			if ( getFactory().getSettings().isCommentsEnabled() ) {
				delete.setComment( "bulk delete" );
			}

			deletes[i] = delete.toStatementString();
		}
	}

	public String[] getSqlStatements() {
		return deletes;
	}

	public int execute(QueryParameters parameters, SessionImplementor session) throws HibernateException {
		coordinateSharedCacheCleanup( session );

		createTemporaryTableIfNecessary( persister, session );

		try {
			// First, save off the pertinent ids, saving the number of pertinent ids for return
			PreparedStatement ps = null;
			int resultCount = 0;
			try {
				try {
					ps = session.getBatcher().prepareStatement( idInsertSelect );
					Iterator paramSpecifications = getIdSelectParameterSpecifications().iterator();
					int pos = 1;
					while ( paramSpecifications.hasNext() ) {
						final ParameterSpecification paramSpec = ( ParameterSpecification ) paramSpecifications.next();
						pos += paramSpec.bind( ps, parameters, session, pos );
					}
					resultCount = ps.executeUpdate();
				}
				finally {
					if ( ps != null ) {
						session.getBatcher().closeStatement( ps );
					}
				}
			}
			catch( SQLException e ) {
				throw JDBCExceptionHelper.convert(
						getFactory().getSQLExceptionConverter(),
				        e,
				        "could not insert/select ids for bulk delete",
				        idInsertSelect
					);
			}

			// Start performing the deletes
			for ( int i = 0; i < deletes.length; i++ ) {
				try {
					try {
						ps = session.getBatcher().prepareStatement( deletes[i] );
						ps.executeUpdate();
					}
					finally {
						if ( ps != null ) {
							session.getBatcher().closeStatement( ps );
						}
					}
				}
				catch( SQLException e ) {
					throw JDBCExceptionHelper.convert(
							getFactory().getSQLExceptionConverter(),
					        e,
					        "error performing bulk delete",
					        deletes[i]
						);
				}
			}

			return resultCount;
		}
		finally {
			dropTemporaryTableIfNecessary( persister, session );
		}
	}

	protected Queryable[] getAffectedQueryables() {
		return new Queryable[] { persister };
	}
}
