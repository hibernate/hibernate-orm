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
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.engine.QueryParameters;
import org.hibernate.engine.RowSelection;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.exception.JDBCExceptionHelper;
import org.hibernate.hql.ast.HqlSqlWalker;
import org.hibernate.hql.ast.QuerySyntaxException;
import org.hibernate.hql.ast.SqlGenerator;
import org.hibernate.param.ParameterSpecification;
import org.hibernate.persister.entity.Queryable;

import antlr.RecognitionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of BasicExecutor.
 *
 * @author Steve Ebersole
 */
public class BasicExecutor extends AbstractStatementExecutor {
	private static final Logger log = LoggerFactory.getLogger( BasicExecutor.class );

	private final Queryable persister;
	private final String sql;
	private final List parameterSpecifications;

	public BasicExecutor(HqlSqlWalker walker, Queryable persister) {
		super( walker, log );
		this.persister = persister;
		try {
			SqlGenerator gen = new SqlGenerator( getFactory() );
			gen.statement( walker.getAST() );
			sql = gen.getSQL();
			gen.getParseErrorHandler().throwQueryException();
			parameterSpecifications = gen.getCollectedParameters();
		}
		catch ( RecognitionException e ) {
			throw QuerySyntaxException.convert( e );
		}
	}

	public String[] getSqlStatements() {
		return new String[] { sql };
	}

	public int execute(QueryParameters parameters, SessionImplementor session) throws HibernateException {

		coordinateSharedCacheCleanup( session );

		PreparedStatement st = null;
		RowSelection selection = parameters.getRowSelection();

		try {
			try {
				st = session.getBatcher().prepareStatement( sql );
				Iterator parameterSpecifications = this.parameterSpecifications.iterator();
				int pos = 1;
				while ( parameterSpecifications.hasNext() ) {
					final ParameterSpecification paramSpec = ( ParameterSpecification ) parameterSpecifications.next();
					pos += paramSpec.bind( st, parameters, session, pos );
				}
				if ( selection != null ) {
					if ( selection.getTimeout() != null ) {
						st.setQueryTimeout( selection.getTimeout().intValue() );
					}
				}

				return st.executeUpdate();
			}
			finally {
				if ( st != null ) {
					session.getBatcher().closeStatement( st );
				}
			}
		}
		catch( SQLException sqle ) {
			throw JDBCExceptionHelper.convert(
					getFactory().getSQLExceptionConverter(),
			        sqle,
			        "could not execute update query",
			        sql
				);
		}
	}

	protected Queryable[] getAffectedQueryables() {
		return new Queryable[] { persister };
	}
}
