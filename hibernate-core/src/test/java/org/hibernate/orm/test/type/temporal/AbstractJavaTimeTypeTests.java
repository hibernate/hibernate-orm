package org.hibernate.orm.test.type.temporal;

import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistryFunctionalTesting;
import org.hibernate.testing.orm.junit.ServiceRegistryProducer;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.hibernate.cfg.JdbcSettings.DIALECT;
import static org.hibernate.cfg.JdbcSettings.JDBC_TIME_ZONE;
import static org.hibernate.cfg.MappingSettings.JAVA_TIME_USE_DIRECT_JDBC;

/**
 * Test support for handling of temporal values.
 *
 * @param <T> The time type being tested.
 * @param <E> The entity type used in tests.
 */
@ServiceRegistryFunctionalTesting
public abstract class AbstractJavaTimeTypeTests<T, E>
		implements ServiceRegistryProducer {

	protected static final String ENTITY_TBL_NAME = "entity_tbl";
	protected static final String ID_COLUMN_NAME = "id_col";
	protected static final String VALUE_COLUMN_NAME = "value_col";

	private final Environment env;

	public AbstractJavaTimeTypeTests(Environment env) {
		this.env = env;
	}

	@Override
	public StandardServiceRegistry produceServiceRegistry(StandardServiceRegistryBuilder builder) {
		builder.applySetting( JAVA_TIME_USE_DIRECT_JDBC, env.directJdbc() );
		configureServiceRegistry( builder );
		return builder.build();
	}

	protected void configureServiceRegistry(StandardServiceRegistryBuilder builder) {
		if ( env.hibernateJdbcTimeZone() != null ) {
			builder.applySetting( JDBC_TIME_ZONE, env.hibernateJdbcTimeZone().getId() );
		}
		if ( env.remappingDialectClass() != null ) {
			builder.applySetting( DIALECT, env.remappingDialectClass().getName() );
		}
	}

	protected abstract Class<T> getJavaTimeType();

	protected abstract Class<E> getEntityType();

	protected abstract E createEntityForHibernateWrite(int id);

	protected abstract T getExpectedPropertyValueAfterHibernateRead();

	protected abstract T getActualPropertyValue(E entity);

	protected abstract Object getExpectedJdbcValueAfterHibernateWrite();

	protected abstract void bindJdbcValue(
			PreparedStatement statement,
			int parameterIndex,
			SessionFactoryScope factoryScope) throws SQLException;

	protected abstract Object extractJdbcValue(
			ResultSet resultSet,
			int columnIndex,
			SessionFactoryScope factoryScope) throws SQLException;

	@AfterEach
	public void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	protected void assertPropertyValueEquals(T expected, T actual, String message) {
		Assertions.assertEquals( expected, actual, message );
	}

	protected void bindDirectJdbcValue(PreparedStatement statement, int index) throws SQLException {
		statement.setObject( index, getExpectedPropertyValueAfterHibernateRead() );
	}

	protected T extractDirectJdbcValue(ResultSet resultSet, int index) throws SQLException {
		return resultSet.getObject( index, getJavaTimeType() );
	}

	@Test
	@JiraKey(value = "HHH-13266")
	public void writeThenRead(SessionFactoryScope factoryScope) {
		Timezones.withDefaultTimeZone( env, () -> {
			factoryScope.inTransaction( (session) -> {
				session.persist( createEntityForHibernateWrite( 1 ) );
			} );
			factoryScope.inTransaction( (session) -> {
				T read = getActualPropertyValue( session.find( getEntityType(), 1 ) );
				T expected = getExpectedPropertyValueAfterHibernateRead();
				assertPropertyValueEquals( expected, read,
						"Writing then reading a value should return the original value" );
			} );
		} );
	}

	// Check the stored value independently of Hibernate's extraction, so symmetric binding/extraction
	// errors cannot hide drift. The JDBC representation follows the intended column contents.
	@Test
	@JiraKey(value = "HHH-13266")
	public void writeThenNativeRead(SessionFactoryScope factoryScope) {
		if ( !env.directJdbc() ) {
			assumeNoJdbcTimeZone();
		}

		Timezones.withDefaultTimeZone( env, () -> {
			factoryScope.inTransaction( session -> {
				session.persist( createEntityForHibernateWrite( 1 ) );
			} );
			factoryScope.inTransaction( session -> {
				session.doWork( connection -> {
					try (PreparedStatement statement = connection.prepareStatement(
							"select value_col from entity_tbl where id_col = ?"
					)) {
						statement.setInt( 1, 1 );
						statement.execute();
						try (ResultSet resultSet = statement.getResultSet()) {
							resultSet.next();
							if ( env.directJdbc() ) {
								T nativeRead = extractDirectJdbcValue( resultSet, 1 );
								T expected = getExpectedPropertyValueAfterHibernateRead();
								assertPropertyValueEquals( expected, nativeRead,
										"Values written by Hibernate ORM should match the original value (same day, hour, ...)" );
							}
							else {
								Object nativeRead = extractJdbcValue( resultSet, 1, factoryScope );
								Object expected = getExpectedJdbcValueAfterHibernateWrite();
								Assertions.assertEquals( expected, nativeRead,
										"Values written by Hibernate ORM should match the original value (same day, hour, ...)" );
							}
						}
					}
				} );
			} );
		} );
	}

	// JDBC initializes pre-existing data with known contents; it is not a second Hibernate binder.
	// In particular, do not copy Hibernate's typed setObject call here: an independent fixture
	// lets this test isolate extraction even if that binding overload introduces drift.
	@Test
	@JiraKey(value = "HHH-13266")
	public void nativeWriteThenRead(SessionFactoryScope factoryScope) {
		if ( !env.directJdbc() ) {
			assumeNoJdbcTimeZone();
		}

		Timezones.withDefaultTimeZone( env, () -> {
			factoryScope.inTransaction( session -> {
				session.doWork( connection -> {
					try (PreparedStatement statement = connection.prepareStatement(
							"insert into entity_tbl (id_col, value_col) values (?,?)"
					)) {
						statement.setInt( 1, 1 );
						if ( env.directJdbc() ) {
							bindDirectJdbcValue( statement, 2 );
						}
						else {
							bindJdbcValue( statement, 2, factoryScope );
						}
						statement.execute();
					}
				} );
			} );
			factoryScope.inTransaction( session -> {
				T read = getActualPropertyValue( session.find( getEntityType(), 1 ) );
				T expected = getExpectedPropertyValueAfterHibernateRead();
				assertPropertyValueEquals( expected, read,
						"Values written without Hibernate ORM should be read correctly by Hibernate ORM" );
			} );
		} );
	}

	@Test
	@JiraKey(value = "HHH-13515")
	public void legacyWriteThenDirectRead(SessionFactoryScope factoryScope) {
		Assumptions.assumeTrue( env.directJdbc(),
				"This test verifies backward compatibility: values written with Hibernate ORM"
				+ " using legacy JDBC should be readable with Hibernate ORM using direct JDBC." );

		Timezones.withDefaultTimeZone( env, () -> {
			ConnectionProvider connectionProvider = factoryScope.getSessionFactory()
					.getServiceRegistry().getService( ConnectionProvider.class );
			StandardServiceRegistryBuilder legacyBuilder = new StandardServiceRegistryBuilder();
			legacyBuilder.applySetting( JAVA_TIME_USE_DIRECT_JDBC, false );
			legacyBuilder.addService( ConnectionProvider.class, connectionProvider );
			configureServiceRegistry( legacyBuilder );

			StandardServiceRegistry legacyRegistry = legacyBuilder.build();
			try {
				SessionFactory legacySessionFactory = new MetadataSources( legacyRegistry )
						.addAnnotatedClass( getEntityType() )
						.buildMetadata()
						.buildSessionFactory();
				try {
					// Pre-existing data must use the legacy schema too: direct JDBC can select
					// a different SQL type for offset/zoned values.
					factoryScope.getSessionFactory().getSchemaManager().drop( false );
					legacySessionFactory.getSchemaManager().create( false );
					legacySessionFactory.inTransaction( session -> {
						session.persist( createEntityForHibernateWrite( 1 ) );
					} );

					// Compare two fresh reads of the same row. Legacy writes may already have lost
					// information (for example a LocalDateTime in a DST gap). Original-value
					// preservation is checked separately by writeThenRead.
					T expected = legacySessionFactory.fromTransaction( session ->
							getActualPropertyValue( session.find( getEntityType(), 1 ) ) );
					factoryScope.inTransaction( session -> {
						T read = getActualPropertyValue( session.find( getEntityType(), 1 ) );
						assertPropertyValueEquals( expected, read,
								"Switching from legacy to direct JDBC should preserve the value read from the same row" );
					} );
				}
				finally {
					try {
						legacySessionFactory.getSchemaManager().drop( false );
						factoryScope.getSessionFactory().getSchemaManager().create( false );
					}
					finally {
						legacySessionFactory.close();
					}
				}
			}
			finally {
				StandardServiceRegistryBuilder.destroy( legacyRegistry );
			}
		} );
	}

	protected void assumeNoJdbcTimeZone() {
		Assumptions.assumeTrue( env.hibernateJdbcTimeZone() == null,
				"Tests with native read/writes are only relevant when not using " + JDBC_TIME_ZONE
				+ ", because the expectations do not take that time zone into account."
				+ " When this property is set, we only test that a write by Hibernate followed by "
				+ " a read by Hibernate returns the same value." );
	}
}
