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
package org.hibernate.type;

import java.sql.Timestamp;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.CallableStatement;

import org.hibernate.engine.SessionImplementor;
import org.hibernate.dialect.Dialect;
import org.hibernate.exception.JDBCExceptionHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <tt>dbtimestamp</tt>: An extension of {@link TimestampType} which
 * maps to the database's current timestamp, rather than the jvm's
 * current timestamp.
 * <p/>
 * Note: May/may-not cause issues on dialects which do not properly support
 * a true notion of timestamp (Oracle < 8, for example, where only its DATE
 * datatype is supported).  Depends on the frequency of DML operations...
 *
 * @author Steve Ebersole
 */
public class DbTimestampType extends TimestampType implements VersionType {

	private static final Logger log = LoggerFactory.getLogger( DbTimestampType.class );
	
	public String getName() { return "dbtimestamp"; }

	public Object seed(SessionImplementor session) {
		if ( session == null ) {
			log.trace( "incoming session was null; using current jvm time" );
			return super.seed( session );
		}
		else if ( !session.getFactory().getDialect().supportsCurrentTimestampSelection() ) {
			log.debug( "falling back to vm-based timestamp, as dialect does not support current timestamp selection" );
			return super.seed( session );
		}
		else {
			return getCurrentTimestamp( session );
		}
	}

	private Timestamp getCurrentTimestamp(SessionImplementor session) {
		Dialect dialect = session.getFactory().getDialect();
		String timestampSelectString = dialect.getCurrentTimestampSelectString();
		if ( dialect.isCurrentTimestampSelectStringCallable() ) {
			return useCallableStatement( timestampSelectString, session );
		}
		else {
			return usePreparedStatement( timestampSelectString, session );
		}
	}

	private Timestamp usePreparedStatement(String timestampSelectString, SessionImplementor session) {
		PreparedStatement ps = null;
		try {
			ps = session.getBatcher().prepareStatement( timestampSelectString );
			ResultSet rs = session.getBatcher().getResultSet( ps );
			rs.next();
			Timestamp ts = rs.getTimestamp( 1 );
			if ( log.isTraceEnabled() ) {
				log.trace(
				        "current timestamp retreived from db : " + ts +
				        " (nanos=" + ts.getNanos() +
				        ", time=" + ts.getTime() + ")"
					);
			}
			return ts;
		}
		catch( SQLException sqle ) {
			throw JDBCExceptionHelper.convert(
			        session.getFactory().getSQLExceptionConverter(),
			        sqle,
			        "could not select current db timestamp",
			        timestampSelectString
				);
		}
		finally {
			if ( ps != null ) {
				try {
					session.getBatcher().closeStatement( ps );
				}
				catch( SQLException sqle ) {
					log.warn( "unable to clean up prepared statement", sqle );
				}
			}
		}
	}

	private Timestamp useCallableStatement(String callString, SessionImplementor session) {
		CallableStatement cs = null;
		try {
			cs = session.getBatcher().prepareCallableStatement( callString );
			cs.registerOutParameter( 1, java.sql.Types.TIMESTAMP );
			cs.execute();
			Timestamp ts = cs.getTimestamp( 1 );
			if ( log.isTraceEnabled() ) {
				log.trace(
				        "current timestamp retreived from db : " + ts +
				        " (nanos=" + ts.getNanos() +
				        ", time=" + ts.getTime() + ")"
					);
			}
			return ts;
		}
		catch( SQLException sqle ) {
			throw JDBCExceptionHelper.convert(
			        session.getFactory().getSQLExceptionConverter(),
			        sqle,
			        "could not call current db timestamp function",
			        callString
				);
		}
		finally {
			if ( cs != null ) {
				try {
					session.getBatcher().closeStatement( cs );
				}
				catch( SQLException sqle ) {
					log.warn( "unable to clean up callable statement", sqle );
				}
			}
		}
	}
}
