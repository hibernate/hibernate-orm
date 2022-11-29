/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.annotations;

import org.hibernate.AssertionFailure;
import org.hibernate.Internal;
import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.tuple.AnnotationValueGenerationStrategy;
import org.hibernate.tuple.GenerationTiming;
import org.hibernate.tuple.InMemoryValueGenerationStrategy;
import org.hibernate.tuple.TimestampGenerators;
import org.hibernate.tuple.ValueGenerator;
import org.jboss.logging.Logger;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import static java.sql.Types.TIMESTAMP;
import static org.hibernate.tuple.GenerationTiming.ALWAYS;

/**
 * Value generation strategy using the query {@link Dialect#getCurrentTimestampSelectString()}.
 * This is a {@code select} that occurs <em>before</em> the {@code insert} or {@code update},
 * whereas with {@link CurrentTimestampGeneration} the {@code select} happens afterward.
 *
 * @see Source
 * @see CurrentTimestampGeneration
 *
 * @author Gavin King
 *
 * @deprecated because {@link Source} is generated, though this implementation is instructive
 */
@Deprecated(since = "6.2")
@Internal
public class SourceGeneration
		implements AnnotationValueGenerationStrategy<Source>, InMemoryValueGenerationStrategy, ValueGenerator<Object> {

	private static final CoreMessageLogger log = Logger.getMessageLogger(
			CoreMessageLogger.class,
			SourceGeneration.class.getName()
	);

	private Class<?> propertyType;
	private ValueGenerator<?> valueGenerator;

	@Override
	public void initialize(Source annotation, Class<?> propertyType, String entityName, String propertyName) {
		initialize( annotation, propertyType );
	}

	public void initialize(Source annotation, Class<?> propertyType) {
		this.propertyType = propertyType;
		switch ( annotation.value() ) {
			case DB:
				valueGenerator =  this;
				break;
			case VM:
				valueGenerator = TimestampGenerators.get( propertyType );
				break;
			default:
				throw new AssertionFailure( "unknown source type" );
		}
	}

	@Override
	public GenerationTiming getGenerationTiming() {
		return ALWAYS;
	}

	@Override
	public ValueGenerator<?> getValueGenerator() {
		return valueGenerator;
	}

	@Override
	public Object generateValue(Session session, Object owner) {
		SharedSessionContractImplementor implementor = (SharedSessionContractImplementor) session;
		return implementor.getTypeConfiguration().getBasicTypeForJavaType( propertyType )
				.getJavaTypeDescriptor().wrap( getCurrentTimestamp( implementor ), implementor );
	}

	private Timestamp getCurrentTimestamp(SharedSessionContractImplementor session) {
		Dialect dialect = session.getJdbcServices().getJdbcEnvironment().getDialect();
		boolean callable = dialect.isCurrentTimestampSelectStringCallable();
		String timestampSelectString = dialect.getCurrentTimestampSelectString();
		PreparedStatement statement = null;
		JdbcCoordinator coordinator = session.getJdbcCoordinator();
		try {
			statement = prepareStatement( coordinator, timestampSelectString, callable );
			Timestamp ts = callable
					? extractCalledResult( statement, coordinator )
					: extractResult( statement, coordinator );
			logResult( ts );
			return ts;
		}
		catch (SQLException e) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					e,
					"could not obtain current timestamp from database",
					timestampSelectString
			);
		}
		finally {
			if ( statement != null ) {
				coordinator.getLogicalConnection().getResourceRegistry().release( statement );
				coordinator.afterStatementExecution();
			}
		}
	}

	private static PreparedStatement prepareStatement(
			JdbcCoordinator coordinator,
			String timestampSelectString,
			boolean callable) {
		return coordinator.getStatementPreparer().prepareStatement( timestampSelectString, callable );
	}

	private static Timestamp extractResult(PreparedStatement statement, JdbcCoordinator coordinator) throws SQLException {
		ResultSet resultSet = coordinator.getResultSetReturn().extract( statement );
		resultSet.next();
		return resultSet.getTimestamp( 1 );
	}

	private static Timestamp extractCalledResult(PreparedStatement statement, JdbcCoordinator coordinator) throws SQLException {
		CallableStatement callable = (CallableStatement) statement;
		callable.registerOutParameter( 1, TIMESTAMP );
		coordinator.getResultSetReturn().execute( callable );
		return callable.getTimestamp( 1 );
	}

	private static void logResult(Timestamp ts) {
		if ( log.isTraceEnabled() ) {
			log.tracev(
					"Current timestamp retrieved from db : {0} (nanos={1}, time={2})",
					ts,
					ts.getNanos(),
					ts.getTime()
			);
		}
	}
}
