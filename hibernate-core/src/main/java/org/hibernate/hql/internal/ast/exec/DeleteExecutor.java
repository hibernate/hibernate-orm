/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.exec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.internal.ast.SqlGenerator;
import org.hibernate.hql.internal.ast.tree.DeleteStatement;
import org.hibernate.hql.internal.ast.tree.FromElement;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.param.ParameterSpecification;
import org.hibernate.persister.collection.AbstractCollectionPersister;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.Delete;
import org.hibernate.type.CollectionType;
import org.hibernate.type.ComponentType;
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
				String sql = gen.getSQL();
				idSubselectWhere = sql.length() > 7 ? sql : "";
			}
			else {
				parameterSpecifications = new ArrayList<>();
				idSubselectWhere = "";
			}

			final boolean commentsEnabled = factory.getSessionFactoryOptions().isCommentsEnabled();
			final MetamodelImplementor metamodel = factory.getMetamodel();
			final boolean notSupportingTuplesInSubqueries = !dialect.supportsTuplesInSubqueries();
			// If many-to-many, delete the FK row in the collection table.
			for ( Type type : persister.getPropertyTypes() ) {
				if ( type.isCollectionType() ) {
					final CollectionType cType = (CollectionType) type;
					final AbstractCollectionPersister cPersister = (AbstractCollectionPersister) metamodel.collectionPersister( cType.getRole() );
					if ( cPersister.isManyToMany() ) {
						Type keyType = cPersister.getKeyType();
						String[] columnNames;
						if ( keyType.isComponentType() ) {
							ComponentType componentType = (ComponentType) keyType;
							List<String> columns = new ArrayList<>( componentType.getPropertyNames().length );
							try {
								for ( String propertyName : componentType.getPropertyNames() ) {
									Collections.addAll( columns, persister.toColumns( propertyName ) );
								}
								columnNames = columns.toArray( new String[0] );
							}
							catch (MappingException e) {
								// Property not found, due to IdClasses are not properly handled in metamodel HHH-12996
								columnNames = persister.getIdentifierColumnNames();
							}
						}
						else {
							columnNames = persister.getIdentifierColumnNames();
						}
						if ( columnNames.length > 1 && notSupportingTuplesInSubqueries ) {
							LOG.warn(
									"This dialect is unable to cascade the delete into the many-to-many join table" +
									" when the entity has multiple primary keys.  Either properly setup cascading on" +
									" the constraints or manually clear the associations prior to deleting the entities."
							);
						}
						else {
							StringBuilder whereBuilder = new StringBuilder();
							whereBuilder.append( '(' );
							append( ", ", cPersister.getKeyColumnNames(), whereBuilder );
							whereBuilder.append( ") in (select " );
							append( ", ", columnNames, whereBuilder );
							final String where = whereBuilder.append(" from ")
								.append( persister.getTableName() ).append( idSubselectWhere ).append( ")" ).toString();
							final Delete delete = new Delete().setTableName( cPersister.getTableName() ).setWhere( where );
							if ( commentsEnabled ) {
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

	private static void append(String delimiter, String[] parts, StringBuilder sb) {
		sb.append( parts[0] );
		for ( int i = 1; i < parts.length; i++ ) {
			sb.append( delimiter );
			sb.append( parts[i] );
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
