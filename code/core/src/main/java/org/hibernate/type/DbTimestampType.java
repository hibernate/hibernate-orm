// $Id: DbTimestampType.java 7830 2005-08-11 00:10:26Z oneovthafew $
package org.hibernate.type;

import java.sql.Timestamp;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.CallableStatement;

import org.hibernate.engine.SessionImplementor;
import org.hibernate.dialect.Dialect;
import org.hibernate.exception.JDBCExceptionHelper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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

	private static final Log log = LogFactory.getLog( DbTimestampType.class );
	
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
