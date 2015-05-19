/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.CoreMessageLogger;

import org.jboss.logging.Logger;

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

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			DbTimestampType.class.getName()
	);

	@Override
	public String getName() {
		return "dbtimestamp";
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[] {getName()};
	}

	@Override
	public Date seed(SessionImplementor session) {
		if ( session == null ) {
			LOG.trace( "Incoming session was null; using current jvm time" );
			return super.seed( session );
		}
		else if ( !session.getFactory().getDialect().supportsCurrentTimestampSelection() ) {
			LOG.debug( "Falling back to vm-based timestamp, as dialect does not support current timestamp selection" );
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
		return usePreparedStatement( timestampSelectString, session );
	}

	private Timestamp usePreparedStatement(String timestampSelectString, SessionImplementor session) {
		PreparedStatement ps = null;
		try {
			ps = session
					.getJdbcCoordinator()
					.getStatementPreparer()
					.prepareStatement( timestampSelectString, false );
			ResultSet rs = session.getJdbcCoordinator().getResultSetReturn().extract( ps );
			rs.next();
			Timestamp ts = rs.getTimestamp( 1 );
			if ( LOG.isTraceEnabled() ) {
				LOG.tracev(
						"Current timestamp retreived from db : {0} (nanos={1}, time={2})",
						ts,
						ts.getNanos(),
						ts.getTime()
				);
			}
			return ts;
		}
		catch (SQLException e) {
			throw session.getFactory().getSQLExceptionHelper().convert(
					e,
					"could not select current db timestamp",
					timestampSelectString
			);
		}
		finally {
			if ( ps != null ) {
				session.getJdbcCoordinator().getResourceRegistry().release( ps );
				session.getJdbcCoordinator().afterStatementExecution();
			}
		}
	}

	private Timestamp useCallableStatement(String callString, SessionImplementor session) {
		CallableStatement cs = null;
		try {
			cs = (CallableStatement) session
					.getJdbcCoordinator()
					.getStatementPreparer()
					.prepareStatement( callString, true );
			cs.registerOutParameter( 1, java.sql.Types.TIMESTAMP );
			session.getJdbcCoordinator().getResultSetReturn().execute( cs );
			Timestamp ts = cs.getTimestamp( 1 );
			if ( LOG.isTraceEnabled() ) {
				LOG.tracev(
						"Current timestamp retreived from db : {0} (nanos={1}, time={2})",
						ts,
						ts.getNanos(),
						ts.getTime()
				);
			}
			return ts;
		}
		catch (SQLException e) {
			throw session.getFactory().getSQLExceptionHelper().convert(
					e,
					"could not call current db timestamp function",
					callString
			);
		}
		finally {
			if ( cs != null ) {
				session.getJdbcCoordinator().getResourceRegistry().release( cs );
				session.getJdbcCoordinator().afterStatementExecution();
			}
		}
	}
}
