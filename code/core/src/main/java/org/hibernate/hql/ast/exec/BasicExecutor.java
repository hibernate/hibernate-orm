// $Id: BasicExecutor.java 9242 2006-02-09 12:37:36Z steveebersole $
package org.hibernate.hql.ast.exec;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Implementation of BasicExecutor.
 *
 * @author Steve Ebersole
 */
public class BasicExecutor extends AbstractStatementExecutor {
	private static final Log log = LogFactory.getLog( BasicExecutor.class );

	private final Queryable persister;
	private final String sql;

	public BasicExecutor(HqlSqlWalker walker, Queryable persister) {
		super( walker, log );
		this.persister = persister;
		try {
			SqlGenerator gen = new SqlGenerator( getFactory() );
			gen.statement( walker.getAST() );
			sql = gen.getSQL();
			gen.getParseErrorHandler().throwQueryException();
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
				Iterator paramSpecifications = getWalker().getParameters().iterator();
				int pos = 1;
				while ( paramSpecifications.hasNext() ) {
					final ParameterSpecification paramSpec = ( ParameterSpecification ) paramSpecifications.next();
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
