/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.exec;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.internal.ast.SqlGenerator;
import org.hibernate.hql.internal.ast.tree.DeleteStatement;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.param.ParameterSpecification;
import org.hibernate.persister.collection.AbstractCollectionPersister;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.Delete;
import org.hibernate.type.CollectionType;
import org.hibernate.type.Type;

import org.jboss.logging.Logger;

import antlr.RecognitionException;
import antlr.collections.AST;


/**
 * Provides deletions in addition to the basic SQL delete statement being executed.  Ex: cascading the delete into a
 * many-to-many join table.
 * 
 * @author Brett Meyer
 */
public class DeleteExecutor extends BasicExecutor {
	private static final Logger LOG = Logger.getLogger( DeleteExecutor.class );

	private final List<String> deletes = new ArrayList<>();
	private List<ParameterSpecification> parameterSpecifications;

	public DeleteExecutor(HqlSqlWalker walker, Queryable persister) {
		super( walker, persister );
		
		final SessionFactoryImplementor factory = walker.getSessionFactoryHelper().getFactory();
		final Dialect dialect = factory.getJdbcServices().getJdbcEnvironment().getDialect();
		
		try {
			final DeleteStatement deleteStatement = (DeleteStatement) walker.getAST();
			
			final String idSubselectWhere;
			if ( deleteStatement.hasWhereClause() ) {
				final AST whereClause = deleteStatement.getWhereClause();
				final SqlGenerator gen = new SqlGenerator( factory );
				gen.whereClause( whereClause );
				parameterSpecifications = gen.getCollectedParameters();
				idSubselectWhere = gen.getSQL().length() > 7 ? gen.getSQL() : "";
			}
			else {
				parameterSpecifications = new ArrayList<>();
				idSubselectWhere = "";
			}
			
			// If many-to-many, delete the FK row in the collection table.
			for ( Type type : persister.getPropertyTypes() ) {
				if ( type.isCollectionType() ) {
					final CollectionType cType = (CollectionType) type;
					final AbstractCollectionPersister cPersister = (AbstractCollectionPersister) factory.getMetamodel().collectionPersister( cType.getRole() );
					if ( cPersister.isManyToMany() ) {
						if ( persister.getIdentifierColumnNames().length > 1
								&& !dialect.supportsTuplesInSubqueries() ) {
							LOG.warn(
									"This dialect is unable to cascade the delete into the many-to-many join table" +
									" when the entity has multiple primary keys.  Either properly setup cascading on" +
									" the constraints or manually clear the associations prior to deleting the entities."
							);
						}
						else {
							final String idSubselect = "(select "
									+ StringHelper.join( ", ", persister.getIdentifierColumnNames() ) + " from "
									+ persister.getTableName() + idSubselectWhere + ")";
							final String where = "(" + StringHelper.join( ", ", cPersister.getKeyColumnNames() )
									+ ") in " + idSubselect;
							final Delete delete = new Delete().setTableName( cPersister.getTableName() ).setWhere( where );
							if ( factory.getSessionFactoryOptions().isCommentsEnabled() ) {
								delete.setComment( "delete FKs in join table" );
							}
							deletes.add( delete.toStatementString() );
						}
					}
				}
			}
		}
		catch (RecognitionException e) {
			throw new HibernateException( "Unable to delete the FKs in the join table!", e );
		}
	}
	
	@Override
	public int execute(QueryParameters parameters, SharedSessionContractImplementor session) throws HibernateException {
		for (String delete : deletes) {
			doExecute( parameters, session, delete, parameterSpecifications );
		}
		
		// finally, execute the original sql statement
		return super.execute( parameters, session );
	}
}
