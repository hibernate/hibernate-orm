/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.generator.internal;

import org.hibernate.Internal;
import org.hibernate.annotations.Source;
import org.hibernate.annotations.SourceType;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.StatementPreparer;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.type.descriptor.java.JavaType;

import org.jboss.logging.Logger;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Member;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.EnumSet;

import static java.sql.Types.TIMESTAMP;
import static org.hibernate.generator.EventTypeSets.INSERT_AND_UPDATE;

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
public class SourceGeneration implements BeforeExecutionGenerator {

	private static final CoreMessageLogger log = Logger.getMessageLogger(
			MethodHandles.lookup(),
			CoreMessageLogger.class,
			SourceGeneration.class.getName()
	);

	private final JavaType<?> propertyType;
	private final CurrentTimestampGeneration.CurrentTimestampGeneratorDelegate valueGenerator;

	public SourceGeneration(Source annotation, Member member, GeneratorCreationContext context) {
		this( annotation.value(), context.getProperty().getType().getReturnedClass(), context );
	}

	public SourceGeneration(SourceType sourceType, Class<?> propertyType, GeneratorCreationContext context) {
		this.propertyType = context.getDatabase().getTypeConfiguration().getJavaTypeRegistry().getDescriptor( propertyType );
		this.valueGenerator = CurrentTimestampGeneration.getGeneratorDelegate( sourceType, propertyType, context );
	}

	/**
	 * @return {@link EventTypeSets#INSERT_ONLY}
	 */
	@Override
	public EnumSet<EventType> getEventTypes() {
		return INSERT_AND_UPDATE;
	}

	@Override
	public Object generate(SharedSessionContractImplementor session, Object owner, Object currentValue, EventType eventType) {
		return valueGenerator == null
				? propertyType.wrap( getCurrentTimestamp( session ), session )
				: valueGenerator.generate();
	}

	private Timestamp getCurrentTimestamp(SharedSessionContractImplementor session) {
		final Dialect dialect = session.getJdbcServices().getJdbcEnvironment().getDialect();
		final boolean callable = dialect.isCurrentTimestampSelectStringCallable();
		final String timestampSelectString = dialect.getCurrentTimestampSelectString();
		final JdbcCoordinator coordinator = session.getJdbcCoordinator();
		final StatementPreparer statementPreparer = coordinator.getStatementPreparer();
		PreparedStatement statement = null;
		try {
			statement = statementPreparer.prepareStatement( timestampSelectString, callable );
			final Timestamp ts = callable
					? extractCalledResult( statement, coordinator, timestampSelectString )
					: extractResult( statement, coordinator, timestampSelectString );
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

	private static Timestamp extractResult(PreparedStatement statement, JdbcCoordinator coordinator, String sql)
			throws SQLException {
		final ResultSet resultSet = coordinator.getResultSetReturn().extract( statement, sql );
		resultSet.next();
		return resultSet.getTimestamp( 1 );
	}

	private static Timestamp extractCalledResult(PreparedStatement statement, JdbcCoordinator coordinator, String sql)
			throws SQLException {
		final CallableStatement callable = (CallableStatement) statement;
		callable.registerOutParameter( 1, TIMESTAMP );
		coordinator.getResultSetReturn().execute( callable, sql );
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
