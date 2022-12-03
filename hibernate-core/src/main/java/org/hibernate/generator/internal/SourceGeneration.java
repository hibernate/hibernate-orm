/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.generator.internal;

import org.hibernate.AssertionFailure;
import org.hibernate.Internal;
import org.hibernate.Session;
import org.hibernate.annotations.Source;
import org.hibernate.annotations.SourceType;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.generator.InMemoryGenerator;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.tuple.TimestampGenerators;
import org.hibernate.tuple.ValueGenerator;
import org.jboss.logging.Logger;

import java.lang.reflect.Member;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import static java.sql.Types.TIMESTAMP;

/**
 * Value generation strategy using the query {@link Dialect#getCurrentTimestampSelectString()}.
 * This is a {@code select} that occurs <em>before</em> the {@code insert} or {@code update},
 * whereas with {@link CurrentTimestampGeneration} the {@code select} happens afterward.
 * <p>
 * Underlies the {@link Source @Source} annotation, and {@code <timestamp source="db"/>} in
 * {@code hbm.xml} mapping documents.
 *
 * @see Source
 * @see CurrentTimestampGeneration
 *
 * @author Gavin King
 *
 * @deprecated because both {@link Source} and {@code hbm.xml} are deprecated, though this
 *             implementation is instructive
 */
@Deprecated(since = "6.2")
@Internal
public class SourceGeneration implements InMemoryGenerator {

	private static final CoreMessageLogger log = Logger.getMessageLogger(
			CoreMessageLogger.class,
			SourceGeneration.class.getName()
	);

	private final Class<?> propertyType;
	private final ValueGenerator<?> valueGenerator;

	public SourceGeneration(Source annotation, Member member, GeneratorCreationContext context) {
		this( annotation.value(), context.getProperty().getType().getReturnedClass() );
	}

	public SourceGeneration(SourceType sourceType, Class<?> propertyType) {
		this.propertyType = propertyType;
		switch ( sourceType ) {
			case DB:
				valueGenerator = this::generateValue;
				break;
			case VM:
				valueGenerator = TimestampGenerators.get( propertyType );
				break;
			default:
				throw new AssertionFailure( "unknown source type" );
		}
	}

	/**
	 * @return {@code true}
	 */
	@Override
	public boolean generatedOnInsert() {
		return true;
	}

	/**
	 * @return {@code false}
	 */
	@Override
	public boolean generatedOnUpdate() {
		return false;
	}

	@Override
	public Object generate(SharedSessionContractImplementor session, Object owner, Object currentValue) {
		return valueGenerator.generateValue( (Session) session, owner, currentValue );
	}

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
