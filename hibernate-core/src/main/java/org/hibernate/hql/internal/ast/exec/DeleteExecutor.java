/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.hql.internal.ast.exec;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
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

	private final List<String> deletes = new ArrayList<String>();
	private List<ParameterSpecification> parameterSpecifications;

	public DeleteExecutor(HqlSqlWalker walker, Queryable persister) {
		super( walker, persister );
		
		final SessionFactoryImplementor factory = walker.getSessionFactoryHelper().getFactory();
		final Dialect dialect = factory.getDialect();
		
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
				parameterSpecifications = new ArrayList<ParameterSpecification>();
				idSubselectWhere = "";
			}

			// find plural attributes defined for the entity being deleted...
			for ( Type type : persister.getPropertyTypes() ) {
				if ( ! type.isCollectionType() ) {
					continue;
				}

				// if the plural attribute maps to a "collection table" we need
				// to remove the rows from that table corresponding to any
				// owners we are about to delete.  "collection table" is
				// (unfortunately) indicated in a number of ways, but here we
				// are mainly concerned with:
				//		1) many-to-many mappings
				//		2) basic collection mappings
				final CollectionType cType = (CollectionType) type;
				final AbstractCollectionPersister cPersister = (AbstractCollectionPersister) factory
							.getCollectionPersister( cType.getRole() );
				final boolean hasCollectionTable = cPersister.isManyToMany()
						|| !cPersister.getElementType().isAssociationType();
				if ( !hasCollectionTable ) {
					continue;
				}

				if ( persister.getIdentifierColumnNames().length > 1
						&& !dialect.supportsTuplesInSubqueries() ) {
					LOG.warn(
							"This dialect is unable to cascade the delete into the many-to-many join table" +
									" when the entity has multiple primary keys.  Either properly setup cascading on" +
									" the constraints or manually clear the associations prior to deleting the entities."
					);
					continue;
				}

				final String idSubselect = "(select "
						+ StringHelper.join( ", ", persister.getIdentifierColumnNames() ) + " from "
						+ persister.getTableName() + idSubselectWhere + ")";
				final String where = "(" + StringHelper.join( ", ", cPersister.getKeyColumnNames() )
						+ ") in " + idSubselect;
				final Delete delete = new Delete().setTableName( cPersister.getTableName() ).setWhere( where );
				if ( factory.getSettings().isCommentsEnabled() ) {
					delete.setComment( "bulk delete - collection table clean up (" + cPersister.getRole() + ")" );
				}
				deletes.add( delete.toStatementString() );
			}
		}
		catch (RecognitionException e) {
			throw new HibernateException( "Unable to delete the FKs in the join table!", e );
		}
	}
	
	@Override
	public int execute(QueryParameters parameters, SessionImplementor session) throws HibernateException {
		for (String delete : deletes) {
			doExecute( parameters, session, delete, parameterSpecifications );
		}
		
		// finally, execute the original sql statement
		return super.execute( parameters, session );
	}
}
