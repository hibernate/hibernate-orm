/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.type;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.TRACE;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.SessionImplementor;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

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
public class DbTimestampType extends TimestampType {
	public static final DbTimestampType INSTANCE = new DbTimestampType();

    private static final Logger LOG = org.jboss.logging.Logger.getMessageLogger(Logger.class,
                                                                                DbTimestampType.class.getPackage().getName());

	@Override
    public String getName() {
		return "dbtimestamp";
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[] { getName() };
	}

	@Override
    public Date seed(SessionImplementor session) {
		if ( session == null ) {
            LOG.incomingSessionWasNull();
			return super.seed( session );
		}
		else if ( !session.getFactory().getDialect().supportsCurrentTimestampSelection() ) {
            LOG.fallingBackToVmBasedTimestamp();
			return super.seed( session );
		}
		else {
			return getCurrentTimestamp( session );
		}
	}

	private Date getCurrentTimestamp(SessionImplementor session) {
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
			ps = session.getJDBCContext().getConnectionManager().prepareStatement( timestampSelectString, false );
			ResultSet rs = ps.executeQuery();
			rs.next();
			Timestamp ts = rs.getTimestamp( 1 );
            LOG.currentTimestampRetrievedFromDatabase(ts, ts.getNanos(), ts.getTime());
			return ts;
		}
		catch( SQLException sqle ) {
			throw session.getFactory().getSQLExceptionHelper().convert(
			        sqle,
			        "could not select current db timestamp",
			        timestampSelectString
				);
		}
		finally {
			if ( ps != null ) {
				try {
					ps.close();
				}
				catch( SQLException sqle ) {
                    LOG.warn(LOG.unableToCleanUpPreparedStatement(), sqle);
				}
			}
		}
	}

	private Timestamp useCallableStatement(String callString, SessionImplementor session) {
		CallableStatement cs = null;
		try {
			cs = session.getJDBCContext().getConnectionManager().prepareCallableStatement( callString );
			cs.registerOutParameter( 1, java.sql.Types.TIMESTAMP );
			cs.execute();
			Timestamp ts = cs.getTimestamp( 1 );
            LOG.currentTimestampRetrievedFromDatabase(ts, ts.getNanos(), ts.getTime());
			return ts;
		}
		catch( SQLException sqle ) {
			throw session.getFactory().getSQLExceptionHelper().convert(
			        sqle,
			        "could not call current db timestamp function",
			        callString
				);
		}
		finally {
			if ( cs != null ) {
				try {
					cs.close();
				}
				catch( SQLException sqle ) {
                    LOG.warn(LOG.unableToCleanUpCallableStatement(), sqle);
				}
			}
		}
	}

    /**
     * Interface defining messages that may be logged by the outer class
     */
    @MessageLogger
    interface Logger extends BasicLogger {

        @LogMessage( level = DEBUG )
        @Message( value = "Falling back to vm-based timestamp, as dialect does not support current timestamp selection" )
        void fallingBackToVmBasedTimestamp();

        @LogMessage( level = TRACE )
        @Message( value = "Current timestamp retreived from db : %s (nanos=%d, time=%ld)" )
        void currentTimestampRetrievedFromDatabase( Timestamp ts,
                                                    int nanos,
                                                    long time );

        @LogMessage( level = TRACE )
        @Message( value = "Incoming session was null; using current jvm time" )
        void incomingSessionWasNull();

        @Message( value = "Unable to clean up callable statement" )
        Object unableToCleanUpCallableStatement();

        @Message( value = "Unable to clean up prepared statement" )
        Object unableToCleanUpPreparedStatement();
    }
}
