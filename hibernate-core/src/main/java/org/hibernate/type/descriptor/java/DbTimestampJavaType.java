/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import java.lang.reflect.Type;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Comparator;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.spi.TypeConfiguration;

import org.jboss.logging.Logger;

import jakarta.persistence.TemporalType;

/**
 * Wrapper Java type descriptor for that uses the database timestamp as seed value for versions.
 *
 * @author Christian Beikov
 */
@Deprecated
public class DbTimestampJavaType<T> implements VersionJavaType<T>, TemporalJavaType<T> {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			DbTimestampJavaType.class.getName()
	);

	private final TemporalJavaType<T> delegate;

	public DbTimestampJavaType(TemporalJavaType<T> delegate) {
		this.delegate = delegate;
	}

	@Override
	public T next(T current, SharedSessionContractImplementor session) {
		return seed( session );
	}

	@Override
	@SuppressWarnings("unchecked")
	public T seed(SharedSessionContractImplementor session) {
		if ( session == null ) {
			LOG.trace( "Incoming session was null; using current jvm time" );
			return ((VersionJavaType<T>) delegate).seed( null );
		}
		else if ( !session.getJdbcServices().getJdbcEnvironment().getDialect().supportsCurrentTimestampSelection() ) {
			LOG.debug( "Falling back to vm-based timestamp, as dialect does not support current timestamp selection" );
			return ((VersionJavaType<T>) delegate).seed( session );
		}
		else {
			return getCurrentTimestamp( session );
		}
	}

	private T getCurrentTimestamp(SharedSessionContractImplementor session) {
		Dialect dialect = session.getJdbcServices().getJdbcEnvironment().getDialect();
		String timestampSelectString = dialect.getCurrentTimestampSelectString();
		if ( dialect.isCurrentTimestampSelectStringCallable() ) {
			return useCallableStatement( timestampSelectString, session );
		}
		return usePreparedStatement( timestampSelectString, session );
	}

	private T usePreparedStatement(String timestampSelectString, SharedSessionContractImplementor session) {
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
						"Current timestamp retrieved from db : {0} (nanos={1}, time={2})",
						ts,
						ts.getNanos(),
						ts.getTime()
				);
			}
			return delegate.wrap( ts, session );
		}
		catch (SQLException e) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					e,
					"could not select current db timestamp",
					timestampSelectString
			);
		}
		finally {
			if ( ps != null ) {
				session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( ps );
				session.getJdbcCoordinator().afterStatementExecution();
			}
		}
	}

	private T useCallableStatement(String callString, SharedSessionContractImplementor session) {
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
						"Current timestamp retrieved from db : {0} (nanos={1}, time={2})",
						ts,
						ts.getNanos(),
						ts.getTime()
				);
			}
			return delegate.wrap( ts, session );
		}
		catch (SQLException e) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					e,
					"could not call current db timestamp function",
					callString
			);
		}
		finally {
			if ( cs != null ) {
				session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( cs );
				session.getJdbcCoordinator().afterStatementExecution();
			}
		}
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators indicators) {
		return delegate.getRecommendedJdbcType( indicators );
	}

	@Override
	public T fromString(CharSequence string) {
		return delegate.fromString( string );
	}

	@Override
	public Type getJavaType() {
		return delegate.getJavaType();
	}

	@Override
	public MutabilityPlan<T> getMutabilityPlan() {
		return delegate.getMutabilityPlan();
	}

	@Override
	public long getDefaultSqlLength(Dialect dialect, JdbcType jdbcType) {
		return delegate.getDefaultSqlLength( dialect, jdbcType );
	}

	@Override
	public long getLongSqlLength() {
		return delegate.getLongSqlLength();
	}

	@Override
	public int getDefaultSqlPrecision(Dialect dialect, JdbcType jdbcType) {
		return delegate.getDefaultSqlPrecision( dialect, jdbcType );
	}

	@Override
	public int getDefaultSqlScale(Dialect dialect, JdbcType jdbcType) {
		return delegate.getDefaultSqlScale( dialect, jdbcType );
	}

	@Override
	public Comparator<T> getComparator() {
		return delegate.getComparator();
	}

	@Override
	public int extractHashCode(T value) {
		return delegate.extractHashCode( value );
	}

	@Override
	public boolean areEqual(T one, T another) {
		return delegate.areEqual( one, another );
	}

	@Override
	public String extractLoggableRepresentation(T value) {
		return delegate.extractLoggableRepresentation( value );
	}

	@Override
	public String toString(T value) {
		return delegate.toString( value );
	}

	@Override
	public <X> X unwrap(T value, Class<X> type, WrapperOptions options) {
		return delegate.unwrap( value, type, options );
	}

	@Override
	public <X> T wrap(X value, WrapperOptions options) {
		return delegate.wrap( value, options );
	}

	@Override
	public <X> T coerce(X value, CoercionContext coercionContext) {
		return delegate.coerce( value, coercionContext );
	}

	@Override
	public Class<T> getJavaTypeClass() {
		return delegate.getJavaTypeClass();
	}

	@Override
	public String getCheckCondition(String columnName, JdbcType sqlType, Dialect dialect) {
		return delegate.getCheckCondition( columnName, sqlType, dialect );
	}

	@Override
	public TemporalType getPrecision() {
		return delegate.getPrecision();
	}

	@Override
	public <X> TemporalJavaType<X> resolveTypeForPrecision(
			TemporalType precision,
			TypeConfiguration typeConfiguration) {
		return delegate.resolveTypeForPrecision( precision, typeConfiguration );
	}
}
