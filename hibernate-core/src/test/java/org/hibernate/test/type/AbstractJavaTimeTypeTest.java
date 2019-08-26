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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MariaDB10Dialect;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.junit4.CustomParameterized;
import org.junit.Assume;
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
	protected static final ZoneId ZONE_AUCKLAND = ZoneId.of( "Pacific/Auckland" );
	protected static final ZoneId ZONE_SANTIAGO = ZoneId.of( "America/Santiago" );

	private final EnvironmentParameters env;

	public AbstractJavaTimeTypeTest(EnvironmentParameters env) {
		this.env = env;
	}

	@Override
	protected final Class<?>[] getAnnotatedClasses() {
		return new Class[] { getEntityType() };
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		if ( env.hibernateJdbcTimeZone != null ) {
			configuration.setProperty( AvailableSettings.JDBC_TIME_ZONE, env.hibernateJdbcTimeZone.getId() );
		}
		if ( env.remappingDialectClass != null ) {
			configuration.setProperty( AvailableSettings.DIALECT, env.remappingDialectClass.getName() );
		}
	}

	protected abstract Class<E> getEntityType();

	protected abstract E createEntityForHibernateWrite(int id);

	protected abstract T getExpectedPropertyValueAfterHibernateRead();

	protected abstract T getActualPropertyValue(E entity);

	protected abstract void setJdbcValueForNonHibernateWrite(PreparedStatement statement, int parameterIndex) throws SQLException;

	protected abstract Object getExpectedJdbcValueAfterHibernateWrite();

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
				session.persist( createEntityForHibernateWrite( 1 ) );
			} );
			inTransaction( session -> {
				T read = getActualPropertyValue( session.find( getEntityType(), 1 ) );
				assertEquals(
						"Writing then reading a value should return the original value",
						getExpectedPropertyValueAfterHibernateRead(), read
				);
			} );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13266")
	public void writeThenNativeRead() {
		assumeNoJdbcTimeZone();

		withDefaultTimeZone( () -> {
			inTransaction( session -> {
				session.persist( createEntityForHibernateWrite( 1 ) );
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
							getExpectedJdbcValueAfterHibernateWrite(),
							nativeRead
					);
				} );
			} );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13266")
	public void nativeWriteThenRead() {
		assumeNoJdbcTimeZone();

		withDefaultTimeZone( () -> {
			inTransaction( session -> {
				session.doWork( connection -> {
					final PreparedStatement statement = connection.prepareStatement(
							"INSERT INTO " + ENTITY_NAME + " (" + ID_COLUMN_NAME + ", " + PROPERTY_COLUMN_NAME + ") "
							+ " VALUES ( ? , ? )"
					);
					statement.setInt( 1, 1 );
					setJdbcValueForNonHibernateWrite( statement, 2 );
					statement.execute();
				} );
			} );
			inTransaction( session -> {
				T read = getActualPropertyValue( session.find( getEntityType(), 1 ) );
				assertEquals(
						"Values written without Hibernate ORM should be read correctly by Hibernate ORM",
						getExpectedPropertyValueAfterHibernateRead(), read
				);
			} );
		} );
	}

	protected final void withDefaultTimeZone(Runnable runnable) {
		TimeZone timeZoneBefore = TimeZone.getDefault();
		TimeZone.setDefault( toTimeZone( env.defaultJvmTimeZone ) );
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

	private static TimeZone toTimeZone(ZoneId zoneId) {
		String idString = zoneId.getId();
		if ( idString.startsWith( "UTC+" ) || idString.startsWith( "UTC-" ) ) {
			// Apparently TimeZone doesn't understand UTC+XXX nor UTC-XXX
			// Using GMT+XXX or GMT-XXX as a fallback
			idString = "GMT" + idString.substring( "UTC".length() );
		}

		TimeZone result = TimeZone.getTimeZone( idString );
		if ( !idString.equals( result.getID() ) ) {
			// If the timezone is not understood, getTimeZone returns GMT and the condition above is true
			throw new IllegalStateException( "Attempting to test an unsupported timezone: " + zoneId );
		}

		return result;
	}

	protected final Class<? extends AbstractRemappingH2Dialect> getRemappingDialectClass() {
		return env.remappingDialectClass;
	}

	protected void assumeNoJdbcTimeZone() {
		Assume.assumeTrue(
				"Tests with native read/writes are only relevant when not using " + AvailableSettings.JDBC_TIME_ZONE
						+ ", because the expectations do not take that time zone into account."
						+ " When this property is set, we only test that a write by Hibernate followed by "
						+ " a read by Hibernate returns the same value.",
				env.hibernateJdbcTimeZone == null
		);
	}

	protected static abstract class AbstractParametersBuilder<S extends AbstractParametersBuilder<S>> {

		private final Dialect dialect;

		private final List<Object[]> result = new ArrayList<>();

		private final List<Class<? extends AbstractRemappingH2Dialect>> remappingDialectClasses = new ArrayList<>();

		private ZoneId forcedJdbcTimeZone = null;

		protected AbstractParametersBuilder() {
			dialect = determineDialect();
			remappingDialectClasses.add( null ); // Always test without remapping
		}

		public S skippedForDialects(List<Class<?>> dialectClasses, Consumer<S> skippedIfDialectMatchesClasses) {
			boolean skip = false;
			for ( Class<?> dialectClass : dialectClasses ) {
				if ( dialectClass.isInstance( dialect ) ) {
					skip = true;
				}
			}
			if ( !skip ) {
				skippedIfDialectMatchesClasses.accept( thisAsS() );
			}
			return thisAsS();
		}

		public S withForcedJdbcTimezone(String zoneIdString, Consumer<S> contributor) {
			ZoneId zoneId = ZoneId.of( zoneIdString );
			this.forcedJdbcTimeZone = zoneId;
			try {
				contributor.accept( thisAsS() );
			}
			finally {
				this.forcedJdbcTimeZone = null;
			}
			return thisAsS();
		}

		@SafeVarargs
		public final S alsoTestRemappingsWithH2(Class<? extends AbstractRemappingH2Dialect> ... dialectClasses) {
			if ( dialect instanceof H2Dialect ) {
				// Only test remappings with H2
				Collections.addAll( remappingDialectClasses, dialectClasses );
			}
			return thisAsS();
		}

		protected final boolean isNanosecondPrecisionSupported() {
			// Most databases apparently don't support nanosecond precision correctly
			return dialect instanceof H2Dialect;
		}

		protected final S add(ZoneId defaultJvmTimeZone, Object ... subClassParameters) {
			for ( Class<? extends AbstractRemappingH2Dialect> remappingDialectClass : remappingDialectClasses ) {
				List<Object> parameters = new ArrayList<>();
				parameters.add(
						new EnvironmentParameters(
								defaultJvmTimeZone,
								forcedJdbcTimeZone,
								remappingDialectClass
						)
				);
				Collections.addAll( parameters, subClassParameters );
				result.add( parameters.toArray() );
			}
			if ( forcedJdbcTimeZone == null ) {
				for ( ZoneId hibernateJdbcTimeZone : getHibernateJdbcTimeZonesToTest() ) {
					List<Object> parameters = new ArrayList<>();
					parameters.add(
							new EnvironmentParameters(
									defaultJvmTimeZone,
									hibernateJdbcTimeZone,
									null
							)
					);
					Collections.addAll( parameters, subClassParameters );
					result.add( parameters.toArray() );
				}
			}
			return thisAsS();
		}

		protected Iterable<? extends ZoneId> getHibernateJdbcTimeZonesToTest() {
			return Arrays.asList( ZONE_GMT, ZONE_OSLO );
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

		/**
		 * The Hibernate setting, {@link AvailableSettings#JDBC_TIME_ZONE},
		 * may affect a lot of time-related types,
		 * which is why we take it into account even with timezone-independent types such as Instant.
		 */
		private final ZoneId hibernateJdbcTimeZone;

		private final Class<? extends AbstractRemappingH2Dialect> remappingDialectClass;

		private EnvironmentParameters(ZoneId defaultJvmTimeZone, ZoneId hibernateJdbcTimeZone,
				Class<? extends AbstractRemappingH2Dialect> remappingDialectClass) {
			this.defaultJvmTimeZone = defaultJvmTimeZone;
			this.hibernateJdbcTimeZone = hibernateJdbcTimeZone;
			this.remappingDialectClass = remappingDialectClass;
		}

		@Override
		public String toString() {
			return String.format(
					"[JVM TZ: %s, JDBC TZ: %s, remapping dialect: %s]",
					defaultJvmTimeZone,
					hibernateJdbcTimeZone,
					remappingDialectClass == null ? null : remappingDialectClass.getSimpleName()
			);
		}
	}

	protected static class AbstractRemappingH2Dialect extends H2Dialect {
		private final int overriddenSqlTypeCode;
		private final SqlTypeDescriptor overriddenSqlTypeDescriptor;

		public AbstractRemappingH2Dialect(int overriddenSqlTypeCode, SqlTypeDescriptor overriddenSqlTypeDescriptor) {
			this.overriddenSqlTypeCode = overriddenSqlTypeCode;
			this.overriddenSqlTypeDescriptor = overriddenSqlTypeDescriptor;
		}

		@Override
		protected SqlTypeDescriptor getSqlTypeDescriptorOverride(int sqlCode) {
			if ( overriddenSqlTypeCode == sqlCode ) {
				return overriddenSqlTypeDescriptor;
			}
			else {
				return super.getSqlTypeDescriptorOverride( sqlCode );
			}
		}
	}

}
