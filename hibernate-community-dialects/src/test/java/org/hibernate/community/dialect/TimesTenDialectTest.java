package org.hibernate.community.dialect;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies the SQL and bind values {@link TimesTenDialect} generates for pagination.
///
/// TimesTen paginates with `select first n ...` or `select rows m to n ...`, where `m` and `n` are
/// 1-based and inclusive. No TimesTen database is needed: statements are recorded by a
/// [RecordingConnectionProvider] and never executed.
@DomainModel(annotatedClasses = TimesTenDialectTest.Book.class)
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.DIALECT, value = "org.hibernate.community.dialect.TimesTenDialect"),
				@Setting(name = AvailableSettings.CONNECTION_PROVIDER,
						value = "org.hibernate.community.dialect.TimesTenDialectTest$RecordingConnectionProvider"),
				@Setting(name = AvailableSettings.ALLOW_METADATA_ON_BOOT, value = "false")
		}
)
@SessionFactory
public class TimesTenDialectTest {

	@BeforeEach
	void clearRecordedStatements() {
		RecordingConnectionProvider.EXECUTIONS.clear();
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-21179")
	void hqlFirstPage(SessionFactoryScope scope) {
		// Rows 1 to 5
		assertThat( hql( scope, 0, 5 ) ).satisfies( execution -> {
			assertThat( execution.sql ).startsWith( "select rows ? to ? " );
			assertThat( execution.parameters ).containsExactly( entry( 1, 1 ), entry( 2, 5 ) );
		} );
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-21179")
	void hqlLaterPage(SessionFactoryScope scope) {
		// Skipping 5 rows then taking 5 is rows 6 to 10
		assertThat( hql( scope, 5, 5 ) ).satisfies( execution -> {
			assertThat( execution.sql ).startsWith( "select rows ? to ? " );
			assertThat( execution.parameters ).containsExactly( entry( 1, 6 ), entry( 2, 10 ) );
		} );
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-21179")
	void hqlMaxResultsOnly(SessionFactoryScope scope) {
		assertThat( hql( scope, null, 5 ) ).satisfies( execution -> {
			assertThat( execution.sql ).startsWith( "select first ? " );
			assertThat( execution.parameters ).containsExactly( entry( 1, 5 ) );
		} );
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-21180")
	void nativeFirstPage(SessionFactoryScope scope) {
		// The limit clause must follow `select`; TimesTen rejects it at the end of the statement (TT1001)
		assertThat( nativeQuery( scope, 0, 5 ) ).satisfies( execution -> {
			assertThat( execution.sql ).startsWith( "select rows ? to ? * from " );
			assertThat( execution.parameters ).containsExactly( entry( 1, 1 ), entry( 2, 5 ) );
		} );
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-21180")
	void nativeMaxResultsOnly(SessionFactoryScope scope) {
		assertThat( nativeQuery( scope, null, 5 ) ).satisfies( execution -> {
			assertThat( execution.sql ).startsWith( "select first ? * from " );
			assertThat( execution.parameters ).containsExactly( entry( 1, 5 ) );
		} );
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-21180")
	void nativeLaterPage(SessionFactoryScope scope) {
		assertThat( nativeQuery( scope, 10, 5 ) ).satisfies( execution -> {
			assertThat( execution.sql ).startsWith( "select rows ? to ? * from " );
			assertThat( execution.parameters ).containsExactly( entry( 1, 11 ), entry( 2, 15 ) );
		} );
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-21180")
	void nativeOffsetOnly(SessionFactoryScope scope) {
		assertThat( nativeQuery( scope, 10, null ) ).satisfies( execution -> {
			assertThat( execution.sql ).startsWith( "select rows ? to " + Integer.MAX_VALUE + " * from " );
			assertThat( execution.parameters ).containsExactly( entry( 1, 11 ) );
		} );
	}

	private static RecordingConnectionProvider.Execution hql(
			SessionFactoryScope scope,
			Integer firstResult,
			Integer maxResults) {
		return hql( scope, "from Book b order by b.id", firstResult, maxResults );
	}

	private static RecordingConnectionProvider.Execution hql(
			SessionFactoryScope scope,
			String hql,
			Integer firstResult,
			Integer maxResults) {
		scope.inTransaction( session -> {
			final var query = session.createSelectionQuery( hql, Object.class );
			if ( firstResult != null ) {
				query.setFirstResult( firstResult );
			}
			query.setMaxResults( maxResults ).getResultList();
		} );
		return lastSelect();
	}

	private static RecordingConnectionProvider.Execution nativeQuery(
			SessionFactoryScope scope,
			Integer firstResult,
			Integer maxResults) {
		return nativeQuery( scope, "select * from Book order by id", firstResult, maxResults );
	}

	private static RecordingConnectionProvider.Execution nativeQuery(
			SessionFactoryScope scope,
			String sql,
			Integer firstResult,
			Integer maxResults) {
		scope.inTransaction( session -> {
			final var query = session.createNativeQuery( sql, Book.class );
			if ( firstResult != null ) {
				query.setFirstResult( firstResult );
			}
			if ( maxResults != null ) {
				query.setMaxResults( maxResults );
			}
			query.getResultList();
		} );
		return lastSelect();
	}

	private static RecordingConnectionProvider.Execution lastSelect() {
		final List<RecordingConnectionProvider.Execution> selects = RecordingConnectionProvider.EXECUTIONS.stream()
				.filter( execution -> execution.sql.toLowerCase().startsWith( "select" ) )
				.toList();
		assertThat( selects ).as( "recorded select statements: %s", RecordingConnectionProvider.EXECUTIONS ).hasSize( 1 );
		return selects.get( 0 );
	}

	private static Map.Entry<Integer, Object> entry(int index, int value) {
		return Map.entry( index, value );
	}

	@Entity(name = "Book")
	public static class Book {
		@Id
		private Integer id;
		private String title;
	}

	/// A [ConnectionProvider] that needs no database. Every prepared statement is recorded together
	/// with the values bound to it, and every query returns an empty result.
	public static class RecordingConnectionProvider implements ConnectionProvider {

		/// A prepared statement's SQL and the values bound to it, by parameter index.
		public static final class Execution {
			final String sql;
			final Map<Integer, Object> parameters = new TreeMap<>();

			Execution(String sql) {
				this.sql = sql;
			}

			@Override
			public String toString() {
				return sql + " " + parameters;
			}
		}

		static final List<Execution> EXECUTIONS = new CopyOnWriteArrayList<>();

		@Override
		public Connection getConnection() {
			return proxy( Connection.class, new ConnectionHandler() );
		}

		@Override
		public void closeConnection(Connection connection) {
		}

		@Override
		public boolean supportsAggressiveRelease() {
			return false;
		}

		@Override
		public boolean isUnwrappableAs(Class<?> unwrapType) {
			return false;
		}

		@Override
		public <T> T unwrap(Class<T> unwrapType) {
			throw new UnsupportedOperationException();
		}

		private static final class ConnectionHandler implements InvocationHandler {
			private boolean autoCommit = true;

			@Override
			public Object invoke(Object proxy, Method method, Object[] args) {
				switch ( method.getName() ) {
					case "prepareStatement":
						final Execution execution = new Execution( (String) args[0] );
						EXECUTIONS.add( execution );
						return proxy( PreparedStatement.class, new StatementHandler( execution, (Connection) proxy ) );
					case "createStatement":
						// Used by the schema export of the test framework; nothing is executed
						return proxy( Statement.class, (statementProxy, statementMethod, statementArgs) ->
								defaultValue( statementMethod ) );
					case "getAutoCommit":
						return autoCommit;
					case "setAutoCommit":
						autoCommit = (Boolean) args[0];
						return null;
					default:
						return defaultValue( method );
				}
			}
		}

		private static final class StatementHandler implements InvocationHandler {
			private final Execution execution;
			private final Connection connection;

			StatementHandler(Execution execution, Connection connection) {
				this.execution = execution;
				this.connection = connection;
			}

			@Override
			public Object invoke(Object proxy, Method method, Object[] args) {
				final String name = method.getName();
				if ( name.startsWith( "set" ) && args != null && args.length >= 2 && args[0] instanceof Integer ) {
					// setInt( 1, 5 ), setObject( 2, x ), setNull( 3, Types.INTEGER ), ...
					execution.parameters.put( (Integer) args[0], name.equals( "setNull" ) ? null : args[1] );
					return null;
				}
				switch ( name ) {
					case "executeQuery":
					case "getResultSet":
						return emptyResultSet( (PreparedStatement) proxy );
					case "execute":
						return true;
					case "getUpdateCount":
						return -1;
					case "getConnection":
						return connection;
					default:
						return defaultValue( method );
				}
			}
		}

		private static ResultSet emptyResultSet(PreparedStatement statement) {
			final ResultSetMetaData metaData = proxy( ResultSetMetaData.class, (proxy, method, args) -> {
				switch ( method.getName() ) {
					case "getColumnCount":
						return 2;
					case "getColumnLabel":
					case "getColumnName":
						return (Integer) args[0] == 1 ? "ID" : "TITLE";
					case "getColumnType":
						return (Integer) args[0] == 1 ? Types.INTEGER : Types.VARCHAR;
					default:
						return defaultValue( method );
				}
			} );
			return proxy( ResultSet.class, (proxy, method, args) -> {
				switch ( method.getName() ) {
					case "next":
						return false;
					case "getMetaData":
						return metaData;
					case "getStatement":
						return statement;
					case "findColumn":
						return "ID".equalsIgnoreCase( (String) args[0] ) ? 1 : 2;
					default:
						return defaultValue( method );
				}
			} );
		}

		private static Object defaultValue(Method method) {
			final Class<?> type = method.getReturnType();
			if ( type == boolean.class ) {
				return false;
			}
			if ( type == int.class ) {
				return 0;
			}
			if ( type == long.class ) {
				return 0L;
			}
			if ( type == short.class ) {
				return (short) 0;
			}
			if ( type == byte.class ) {
				return (byte) 0;
			}
			if ( type == float.class ) {
				return 0F;
			}
			if ( type == double.class ) {
				return 0D;
			}
			return null;
		}

		@SuppressWarnings("unchecked")
		private static <T> T proxy(Class<T> type, InvocationHandler handler) {
			return (T) Proxy.newProxyInstance(
					RecordingConnectionProvider.class.getClassLoader(),
					new Class<?>[] { type },
					handler
			);
		}
	}
}
