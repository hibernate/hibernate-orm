/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQL81Dialect;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.junit4.CustomParameterized;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 * Tests for storage of Instant properties.
 *
 * @param <T> The time type being tested.
 * @param <E> The entity type used in tests.
 */
@RunWith(CustomParameterized.class)
abstract class AbstractJavaTimeTypeTest<T, E> extends BaseCoreFunctionalTestCase {

	private static Dialect determineDialect() {
		try {
			return Dialect.getDialect();
		}
		catch (Exception e) {
			return new Dialect() {
			};
		}
	}

	protected static final String ENTITY_NAME = "theentity";
	protected static final String ID_COLUMN_NAME = "theid";
	protected static final String PROPERTY_COLUMN_NAME = "thevalue";

	protected static final ZoneId ZONE_UTC_MINUS_8 = ZoneId.of( "UTC-8" );
	protected static final ZoneId ZONE_PARIS = ZoneId.of( "Europe/Paris" );
	protected static final ZoneId ZONE_GMT = ZoneId.of( "GMT" );
	protected static final ZoneId ZONE_OSLO = ZoneId.of( "Europe/Oslo" );
	protected static final ZoneId ZONE_AMSTERDAM = ZoneId.of( "Europe/Amsterdam" );

	private final EnvironmentParameters env;

	public AbstractJavaTimeTypeTest(EnvironmentParameters env) {
		this.env = env;
	}

	@Override
	protected final Class<?>[] getAnnotatedClasses() {
		return new Class[] { getEntityType() };
	}

	protected abstract Class<E> getEntityType();

	protected abstract E createEntity(int id);

	protected abstract T getExpectedPropertyValue();

	protected abstract T getActualPropertyValue(E entity);

	protected abstract Object getExpectedJdbcValue();

	protected abstract Object getActualJdbcValue(ResultSet resultSet, int columnIndex) throws SQLException;

	@Before
	public void cleanup() {
		inTransaction( session -> {
			session.createNativeQuery( "DELETE FROM " + ENTITY_NAME ).executeUpdate();
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13266")
	public void writeThenRead() {
		withDefaultTimeZone( () -> {
			inTransaction( session -> {
				session.persist( createEntity( 1 ) );
			} );
			inTransaction( session -> {
				T read = getActualPropertyValue( session.find( getEntityType(), 1 ) );
				assertEquals(
						"Writing then reading a value should return the original value",
						getExpectedPropertyValue(), read
				);
			} );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13266")
	public void writeThenNativeRead() {
		withDefaultTimeZone( () -> {
			inTransaction( session -> {
				session.persist( createEntity( 1 ) );
			} );
			inTransaction( session -> {
				session.doWork( connection -> {
					final PreparedStatement statement = connection.prepareStatement(
							"SELECT " + PROPERTY_COLUMN_NAME + " FROM " + ENTITY_NAME + " WHERE " + ID_COLUMN_NAME + " = ?"
					);
					statement.setInt( 1, 1 );
					statement.execute();
					final ResultSet resultSet = statement.getResultSet();
					resultSet.next();
					Object nativeRead = getActualJdbcValue( resultSet, 1 );
					assertEquals(
							"Values written by Hibernate ORM should match the original value (same day, hour, ...)",
							getExpectedJdbcValue(),
							nativeRead
					);
				} );
			} );
		} );
	}

	protected final void withDefaultTimeZone(Runnable runnable) {
		TimeZone timeZoneBefore = TimeZone.getDefault();
		TimeZone.setDefault( TimeZone.getTimeZone( env.defaultJvmTimeZone ) );
		/*
		 * Run the code in a new thread, because some libraries (looking at you, h2 JDBC driver)
		 * cache data dependent on the default timezone in thread local variables,
		 * and we want this data to be reinitialized with the new default time zone.
		 */
		try {
			ExecutorService executor = Executors.newSingleThreadExecutor();
			Future<?> future = executor.submit( runnable );
			executor.shutdown();
			future.get();
		}
		catch (InterruptedException e) {
			throw new IllegalStateException( "Interrupted while testing", e );
		}
		catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if ( cause instanceof RuntimeException ) {
				throw (RuntimeException) cause;
			}
			else if ( cause instanceof Error ) {
				throw (Error) cause;
			}
			else {
				throw new IllegalStateException( "Unexpected exception while testing", cause );
			}
		}
		finally {
			TimeZone.setDefault( timeZoneBefore );
		}
	}

	protected static abstract class AbstractParametersBuilder<S extends AbstractParametersBuilder<S>> {

		private final Dialect dialect;

		private final List<Object[]> result = new ArrayList<>();

		protected AbstractParametersBuilder() {
			dialect = determineDialect();
		}

		protected final boolean isNanosecondPrecisionSupported() {
			// PostgreSQL apparently doesn't support nanosecond precision correctly
			return !( dialect instanceof PostgreSQL81Dialect );
		}

		protected final S add(ZoneId defaultJvmTimeZone, Object ... subClassParameters) {
			List<Object> parameters = new ArrayList<>();
			parameters.add( new EnvironmentParameters( defaultJvmTimeZone ) );
			Collections.addAll( parameters, subClassParameters );
			result.add( parameters.toArray() );
			return thisAsS();
		}

		private S thisAsS() {
			return (S) this;
		}

		public List<Object[]> build() {
			return result;
		}

	}

	protected final static class EnvironmentParameters {

		/*
		 * The default timezone affects conversions done using java.util,
		 * which is why we take it into account even with timezone-independent types such as Instant.
		 */
		private final ZoneId defaultJvmTimeZone;

		private EnvironmentParameters(ZoneId defaultJvmTimeZone) {
			this.defaultJvmTimeZone = defaultJvmTimeZone;
		}

		@Override
		public String toString() {
			return String.format( "[JVM TZ: %s]", defaultJvmTimeZone );
		}
	}

}
