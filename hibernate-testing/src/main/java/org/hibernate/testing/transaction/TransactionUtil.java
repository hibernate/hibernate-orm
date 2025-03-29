/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.transaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

import org.hibernate.Session;
import org.hibernate.SessionBuilder;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.DialectDelegateWrapper;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.ServiceRegistry;

import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.Assert;

import org.jboss.logging.Logger;

/**
 * @author Vlad Mihalcea
 */
public class TransactionUtil {

	private static final Logger log = Logger.getLogger( TransactionUtil.class );

	public static void doInHibernate(Supplier<SessionFactory> factorySupplier, Consumer<Session> function) {
		final SessionFactory sessionFactory = factorySupplier.get();
		Assert.assertNotNull( "SessionFactory is null in test!", sessionFactory );
		//Make sure any error is propagated
		try ( Session session = sessionFactory.openSession() ) {
			final Transaction txn = session.getTransaction();
			txn.begin();
			try {
				function.accept( session );
			}
			catch (Throwable e) {
				try {
					txn.rollback();
				}
				finally {
					throw e;
				}
			}
			txn.commit();
		}
	}

	/**
	 * Hibernate transaction function
	 *
	 * @param <T> function result
	 */
	@FunctionalInterface
	public interface HibernateTransactionFunction<T>
			extends Function<Session, T> {
		/**
		 * Before transaction completion function
		 */
		default void beforeTransactionCompletion() {

		}

		/**
		 * After transaction completion function
		 */
		default void afterTransactionCompletion() {

		}
	}

	/**
	 * Hibernate transaction function without return value
	 */
	@FunctionalInterface
	public interface HibernateTransactionConsumer extends Consumer<Session> {
		/**
		 * Before transaction completion function
		 */
		default void beforeTransactionCompletion() {

		}

		/**
		 * After transaction completion function
		 */
		default void afterTransactionCompletion() {

		}
	}

	/**
	 * JPA transaction function
	 *
	 * @param <T> function result
	 */
	@FunctionalInterface
	public interface JPATransactionFunction<T>
			extends Function<EntityManager, T> {
		/**
		 * Before transaction completion function
		 */
		default void beforeTransactionCompletion() {

		}

		/**
		 * After transaction completion function
		 */
		default void afterTransactionCompletion() {

		}
	}

	/**
	 * JPA transaction function without return value
	 */
	@FunctionalInterface
	public interface JPATransactionVoidFunction
			extends Consumer<EntityManager> {
		/**
		 * Before transaction completion function
		 */
		default void beforeTransactionCompletion() {

		}

		/**
		 * After transaction completion function
		 */
		default void afterTransactionCompletion() {

		}
	}

	/**
	 * JDBC transaction function
	 *
	 * @param <T> function result
	 */
	@FunctionalInterface
	public interface JDBCTransactionFunction<T> {
		T accept(Connection connection) throws SQLException;
	}

	/**
	 * JDBC transaction function without return value
	 */
	@FunctionalInterface
	public interface JDBCTransactionVoidFunction {
		void accept(Connection connection) throws SQLException;
	}

	/**
	 * Execute function in a JPA transaction
	 *
	 * @param factorySupplier EntityManagerFactory supplier
	 * @param function function
	 * @param properties properties for entity manager bootstrapping
	 * @param <T> result type
	 *
	 * @return result
	 */
	public static <T> T doInJPA(
			Supplier<EntityManagerFactory> factorySupplier,
			JPATransactionFunction<T> function,
			Map properties) {
		T result = null;
		EntityManager entityManager = null;
		EntityTransaction txn = null;
		try {
			entityManager = properties == null ?
				factorySupplier.get().createEntityManager():
				factorySupplier.get().createEntityManager(properties);
			function.beforeTransactionCompletion();
			txn = entityManager.getTransaction();
			txn.begin();
			result = function.apply( entityManager );
			txn.commit();
		}
		catch ( Throwable e ) {
			if ( txn != null && txn.isActive() ) {
				txn.rollback();
			}
			throw e;
		}
		finally {
			function.afterTransactionCompletion();
			if ( entityManager != null ) {
				entityManager.close();
			}
		}
		return result;
	}

	/**
	 * Execute function in a JPA transaction
	 *
	 * @param factorySupplier EntityManagerFactory supplier
	 * @param function function
	 * @param <T> result type
	 *
	 * @return result
	 */
	public static <T> T doInJPA(
			Supplier<EntityManagerFactory> factorySupplier,
			JPATransactionFunction<T> function) {
		return doInJPA( factorySupplier, function, null );
	}

	/**
	 * Execute function in a JPA transaction without return value
	 *
	 * @param factorySupplier EntityManagerFactory supplier
	 * @param function function
	 * @param properties properties for entity manager bootstrapping
	 */
	public static void doInJPA(
			Supplier<EntityManagerFactory> factorySupplier,
			JPATransactionVoidFunction function,
			Map properties) {
		EntityManager entityManager = null;
		EntityTransaction txn = null;
		try {
			entityManager = properties == null ?
				factorySupplier.get().createEntityManager():
				factorySupplier.get().createEntityManager(properties);
			function.beforeTransactionCompletion();
			txn = entityManager.getTransaction();
			txn.begin();
			function.accept( entityManager );
			if ( !txn.getRollbackOnly() ) {
				txn.commit();
			}
			else {
				try {
					txn.rollback();
				}
				catch (Exception e) {
					log.error( "Rollback failure", e );
				}
			}
		}
		catch ( Throwable t ) {
			if ( txn != null && txn.isActive() ) {
				try {
					txn.rollback();
				}
				catch (Exception e) {
					log.error( "Rollback failure", e );
				}
			}
			throw t;
		}
		finally {
			function.afterTransactionCompletion();
			if ( entityManager != null ) {
				entityManager.close();
			}
		}
	}

	/**
	 * Execute function in a JPA transaction without return value
	 *
	 * @param factorySupplier EntityManagerFactory supplier
	 * @param function function
	 */
	public static void doInJPA(
			Supplier<EntityManagerFactory> factorySupplier,
			JPATransactionVoidFunction function) {
		doInJPA( factorySupplier, function, null );
	}

	/**
	 * Execute function in a Hibernate transaction
	 *
	 * @param factorySupplier SessionFactory supplier
	 * @param function function
	 * @param <T> result type
	 *
	 * @return result
	 */
	public static <T> T doInHibernate(
			Supplier<SessionFactory> factorySupplier,
			HibernateTransactionFunction<T> function) {
		T result = null;
		Session session = null;
		Transaction txn = null;
		try {
			session = factorySupplier.get().openSession();
			function.beforeTransactionCompletion();
			txn = session.beginTransaction();

			result = function.apply( session );
			if ( !txn.getRollbackOnly() ) {
				txn.commit();
			}
			else {
				try {
					txn.rollback();
				}
				catch (Exception e) {
					log.error( "Rollback failure", e );
				}
			}
		}
		catch ( Throwable t ) {
			if ( txn != null && txn.isActive() ) {
				try {
					txn.rollback();
				}
				catch (Exception e) {
					log.error( "Rollback failure", e );
				}
			}
			throw t;
		}
		finally {
			function.afterTransactionCompletion();
			if ( session != null ) {
				session.close();
			}
		}
		return result;
	}

	/**
	 * Execute function in a Hibernate transaction without return value
	 *
	 * @param factorySupplier SessionFactory supplier
	 * @param function function
	 */
	public static void doInHibernate(
			Supplier<SessionFactory> factorySupplier,
			HibernateTransactionConsumer function) {
		Session session = null;
		Transaction txn = null;
		try {
			session = factorySupplier.get().openSession();
			function.beforeTransactionCompletion();
			txn = session.beginTransaction();

			function.accept( session );
			if ( !txn.getRollbackOnly() ) {
				txn.commit();
			}
			else {
				try {
					txn.rollback();
				}
				catch (Exception e) {
					log.error( "Rollback failure", e );
				}
			}
		}
		catch ( Throwable t ) {
			if ( txn != null && txn.isActive() ) {
				try {
					txn.rollback();
				}
				catch (Exception e) {
					log.error( "Rollback failure", e );
				}
			}
			throw t;
		}
		finally {
			function.afterTransactionCompletion();
			if ( session != null ) {
				session.close();
			}
		}
	}

	/**
	 * Execute function in a Hibernate transaction without return value and for a given tenant
	 *
	 * @param factorySupplier SessionFactory supplier
	 * @param tenant tenant
	 * @param function function
	 */
	public static void doInHibernate(
			Supplier<SessionFactory> factorySupplier,
			String tenant,
			Consumer<Session> function) {
		Session session = null;
		Transaction txn = null;
		try {
			session = factorySupplier.get()
					.withOptions()
					.tenantIdentifier( tenant )
					.openSession();
			txn = session.getTransaction();
			txn.begin();
			function.accept( session );
			txn.commit();
		}
		catch (Throwable e) {
			if ( txn != null ) {
				txn.rollback();
			}
			throw e;
		}
		finally {
			if ( session != null ) {
				session.close();
			}
		}
	}

	/**
	 * Execute function in a Hibernate transaction for a given tenant and return a value
	 *
	 * @param factorySupplier SessionFactory supplier
	 * @param tenant tenant
	 * @param function function
	 *
	 * @return result
	 */
	public static <R> R doInHibernate(
			Supplier<SessionFactory> factorySupplier,
			String tenant,
			Function<Session, R> function) {
		Session session = null;
		Transaction txn = null;
		try {
			session = factorySupplier.get()
					.withOptions()
					.tenantIdentifier( tenant )
					.openSession();
			txn = session.getTransaction();
			txn.begin();
			R returnValue = function.apply( session );
			txn.commit();
			return returnValue;
		}
		catch (Throwable e) {
			if ( txn != null ) {
				txn.rollback();
			}
			throw e;
		}
		finally {
			if ( session != null ) {
				session.close();
			}
		}
	}

	/**
	 * Execute function in a Hibernate transaction
	 *
	 * @param sessionBuilderSupplier SessionFactory supplier
	 * @param function function
	 * @param <T> result type
	 *
	 * @return result
	 */
	public static <T> T doInHibernateSessionBuilder(
			Supplier<SessionBuilder> sessionBuilderSupplier,
			HibernateTransactionFunction<T> function) {
		T result = null;
		Session session = null;
		Transaction txn = null;
		try {
			session = sessionBuilderSupplier.get().openSession();
			function.beforeTransactionCompletion();
			txn = session.beginTransaction();

			result = function.apply( session );
			if ( !txn.getRollbackOnly() ) {
				txn.commit();
			}
			else {
				try {
					txn.rollback();
				}
				catch (Exception e) {
					log.error( "Rollback failure", e );
				}
			}
		}
		catch ( Throwable t ) {
			if ( txn != null && txn.isActive() ) {
				try {
					txn.rollback();
				}
				catch (Exception e) {
					log.error( "Rollback failure", e );
				}
			}
			throw t;
		}
		finally {
			function.afterTransactionCompletion();
			if ( session != null ) {
				session.close();
			}
		}
		return result;
	}

	/**
	 * Execute function in a Hibernate transaction without return value
	 *
	 * @param sessionBuilderSupplier SessionFactory supplier
	 * @param function function
	 */
	public static void doInHibernateSessionBuilder(
			Supplier<SessionBuilder> sessionBuilderSupplier,
			HibernateTransactionConsumer function) {
		Session session = null;
		Transaction txn = null;
		try {
			session = sessionBuilderSupplier.get().openSession();
			function.beforeTransactionCompletion();
			txn = session.beginTransaction();

			function.accept( session );
			if ( !txn.getRollbackOnly() ) {
				txn.commit();
			}
			else {
				try {
					txn.rollback();
				}
				catch (Exception e) {
					log.error( "Rollback failure", e );
				}
			}
		}
		catch ( Throwable t ) {
			if ( txn != null && txn.isActive() ) {
				try {
					txn.rollback();
				}
				catch (Exception e) {
					log.error( "Rollback failure", e );
				}
			}
			throw t;
		}
		finally {
			function.afterTransactionCompletion();
			if ( session != null ) {
				session.close();
			}
		}
	}
	/**
	 * Set Session or Statement timeout
	 * @param session Hibernate Session
	 * @deprecated Use {@link #withJdbcTimeout(Session, Runnable)} instead
	 */
	@Deprecated
	public static void setJdbcTimeout(Session session) {
		setJdbcTimeout( session, TimeUnit.SECONDS.toMillis( 1 ) );
	}

	/**
	 * Set Session or Statement timeout
	 * @param session Hibernate Session
	 * @deprecated Use {@link #withJdbcTimeout(Session, long, Runnable)} instead
	 */
	@Deprecated
	public static void setJdbcTimeout(Session session, long millis) {
		final Dialect dialect = session.getSessionFactory().unwrap( SessionFactoryImplementor.class ).getJdbcServices().getDialect();
		session.doWork( connection -> {
			setJdbcTimeout( dialect, connection, millis );
		} );
	}

	private static void setJdbcTimeout(Dialect dialect, Connection connection, long millis) throws SQLException {
		Dialect extractedDialect = DialectDelegateWrapper.extractRealDialect( dialect );
		if ( extractedDialect instanceof PostgreSQLDialect || extractedDialect instanceof CockroachDialect ) {
			try (Statement st = connection.createStatement()) {
				//Prepared Statements fail for SET commands
				st.execute( String.format( "SET statement_timeout TO %d", millis ) );
			}
			catch (SQLException ex) {
				// Ignore if resetting the statement timeout to 0 fails
				// Since PostgreSQL is transactional anyway,
				// the prior change of statement timeout will be undone on rollback
				if ( millis != 0 ) {
					throw ex;
				}
			}
		}
		else if ( extractedDialect instanceof MySQLDialect ) {
			try (PreparedStatement st = connection.prepareStatement( "SET SESSION innodb_lock_wait_timeout = ?" )) {
				// 50 seconds is the default
				st.setLong( 1, millis == 0L ? 50 : Math.max( 1, Math.round( millis / 1e3f ) ) );
				st.execute();
			}
		}
		else if ( extractedDialect instanceof H2Dialect ) {
			try (PreparedStatement st = connection.prepareStatement( "SET LOCK_TIMEOUT ?" )) {
				// 10 seconds is the default we set
				st.setLong( 1, millis == 0L ? 10_000 : millis );
				st.execute();
			}
		}
		else if ( extractedDialect instanceof SQLServerDialect ) {
			try (Statement st = connection.createStatement()) {
				//Prepared Statements fail for SET commands
				st.execute( String.format( "SET LOCK_TIMEOUT %d", millis == 0L ? -1L : millis ) );
			}
		}
		else if ( extractedDialect instanceof HANADialect ) {
			try (Statement st = connection.createStatement()) {
				//Prepared Statements fail for SET commands
				st.execute( String.format( "SET TRANSACTION LOCK WAIT TIMEOUT %d", millis ) );
			}
		}
		else if ( extractedDialect instanceof SybaseASEDialect ) {
			try (Statement st = connection.createStatement()) {
				//Prepared Statements fail for SET commands
				if ( millis == 0L ) {
					st.execute( "SET LOCK WAIT" );
				}
				else {
					st.execute( String.format( "SET LOCK WAIT %d", Math.max( 1, Math.round( millis / 1e3f ) ) ) );
				}
			}
		}
		else {
			try {
				connection.setNetworkTimeout( Executors.newSingleThreadExecutor(), (int) millis );
			}
			catch (Throwable ignore) {
			}
		}
	}

	/**
	 * Run the runnable with a session or statement timeout
	 *
	 * @param session Hibernate Session
	 */
	public static void withJdbcTimeout(Session session, Runnable r) {
		withJdbcTimeout( session, TimeUnit.SECONDS.toMillis( 1 ), r );
	}

	/**
	 * Run the runnable with a session or statement timeout
	 *
	 * @param session Hibernate Session
	 */
	public static void withJdbcTimeout(Session session, long millis, Runnable r) {
		final Dialect dialect = session.getSessionFactory().unwrap( SessionFactoryImplementor.class ).getJdbcServices().getDialect();
		session.doWork( connection -> {
			try {
				setJdbcTimeout( dialect, connection, millis );
				r.run();
			}
			finally {
				setJdbcTimeout( dialect, connection, 0 );
			}
		} );
	}

	/**
	 * Use the supplied settings for building a new {@link ServiceRegistry} and
	 * create a new JDBC {@link Connection} in auto-commit mode.
	 *
	 * A new JDBC {@link Statement} is created and passed to the supplied callback.
	 *
	 * @param consumer {@link Statement} callback to execute statements in auto-commit mode
	 * @param settings Settings to build a new {@link ServiceRegistry}
	 */
	public static void doInAutoCommit(Consumer<Statement> consumer, Map<String,Object> settings) {
		StandardServiceRegistryBuilder ssrb = ServiceRegistryUtil.serviceRegistryBuilder();
		if ( settings != null ) {
			// Reset the connection provider to avoid rebuilding the shared connection pool for a single test
			ssrb.applySetting( AvailableSettings.CONNECTION_PROVIDER, "" );
			ssrb.applySettings( settings );
		}
		StandardServiceRegistry ssr = ssrb.build();

		try {
			final JdbcConnectionAccess connectionAccess = ssr.getService( JdbcServices.class )
					.getBootstrapJdbcConnectionAccess();
			final Connection connection;
			try {
				connection = connectionAccess.obtainConnection();
			}
			catch (SQLException e) {
				throw new RuntimeException( e );
			}
			try (Statement statement = connection.createStatement()) {
				connection.setAutoCommit( true );
				consumer.accept( statement );
			}
			catch (SQLException e) {
				log.debug( e.getMessage() );
			}
			finally {
				try {
					connectionAccess.releaseConnection( connection );
				}
				catch (SQLException e) {
					// ignore
				}
			}
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	/**
	 * Use the default settings for building a new {@link ServiceRegistry} and
	 * create a new JDBC {@link Connection} in auto-commit mode.
	 *
	 * A new JDBC {@link Statement} is created and passed to the supplied callback.
	 *
	 * @param consumer {@link Statement} callback to execute statements in auto-commit mode
	 */
	public static void doInAutoCommit(Consumer<Statement> consumer) {
		doInAutoCommit( consumer, null );
	}

	/**
	 * Use the supplied settings for building a new {@link ServiceRegistry} and
	 * create a new JDBC {@link Connection} in auto-commit mode.
	 *
	 * The supplied statements will be executed using the previously created connection
	 *
	 * @param settings Settings to build a new {@link ServiceRegistry}
	 * @param statements statements to be executed in auto-commit mode
	 */
	public static void doInAutoCommit(Map<String,Object> settings, String... statements) {
		doInAutoCommit( s -> {
			for ( String statement : statements ) {
				try {
					s.executeUpdate( statement );
				}
				catch (SQLException e) {
					log.debugf( e, "Statement [%s] execution failed!", statement );
				}
			}
		}, settings );
	}

	/**
	 * Use the default settings for building a new {@link ServiceRegistry} and
	 * create a new JDBC {@link Connection} in auto-commit mode.
	 *
	 * The supplied statements will be executed using the previously created connection
	 *
	 * @param statements statements to be executed in auto-commit mode
	 */
	public static void doInAutoCommit(String... statements) {
		doInAutoCommit( null, statements );
	}

	public static void doWithJDBC(ServiceRegistry serviceRegistry, JDBCTransactionVoidFunction function) throws SQLException {
		final JdbcConnectionAccess connectionAccess = serviceRegistry.getService( JdbcServices.class )
				.getBootstrapJdbcConnectionAccess();
		final Connection connection;
		try {
			connection = connectionAccess.obtainConnection();
		}
		catch (SQLException e) {
			throw new RuntimeException( e );
		}
		try {
			function.accept( connection );
		}
		finally {
			if ( connection != null ) {
				try {
					connectionAccess.releaseConnection( connection );
				}
				catch (SQLException ignore) {
				}
			}
		}
	}

	public static <T> T doWithJDBC(ServiceRegistry serviceRegistry, JDBCTransactionFunction<T> function) throws SQLException {
		final JdbcConnectionAccess connectionAccess = serviceRegistry.getService( JdbcServices.class )
				.getBootstrapJdbcConnectionAccess();
		final Connection connection;
		try {
			connection = connectionAccess.obtainConnection();
		}
		catch (SQLException e) {
			throw new RuntimeException( e );
		}
		try {
			return function.accept( connection );
		}
		finally {
			if ( connection != null ) {
				try {
					connectionAccess.releaseConnection( connection );
				}
				catch (SQLException ignore) {
				}
			}
		}
	}
}
