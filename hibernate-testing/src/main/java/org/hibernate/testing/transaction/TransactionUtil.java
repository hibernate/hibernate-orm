/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.transaction;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

import org.hibernate.Session;
import org.hibernate.SessionBuilder;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

/**
 * @author Vlad Mihalcea
 */
public class TransactionUtil {

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
		T result = null;
		EntityManager entityManager = null;
		EntityTransaction txn = null;
		try {
			entityManager = factorySupplier.get().createEntityManager();
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
	 * Execute function in a JPA transaction without return value
	 *
	 * @param factorySupplier EntityManagerFactory supplier
	 * @param function function
	 */
	public static void doInJPA(
			Supplier<EntityManagerFactory> factorySupplier,
			JPATransactionVoidFunction function) {
		EntityManager entityManager = null;
		EntityTransaction txn = null;
		try {
			entityManager = factorySupplier.get().createEntityManager();
			function.beforeTransactionCompletion();
			txn = entityManager.getTransaction();
			txn.begin();
			function.accept( entityManager );
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
			txn.commit();
		}
		catch ( Throwable e ) {
			if ( txn != null ) {
				txn.rollback();
			}
			throw e;
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
			txn.commit();
		}
		catch ( Throwable e ) {
			if ( txn != null ) {
				txn.rollback();
			}
			throw e;
		}
		finally {
			function.afterTransactionCompletion();
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
			txn.commit();
		}
		catch ( Throwable e ) {
			if ( txn != null ) {
				txn.rollback();
			}
			throw e;
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
			txn.commit();
		}
		catch ( Throwable e ) {
			if ( txn != null ) {
				txn.rollback();
			}
			throw e;
		}
		finally {
			function.afterTransactionCompletion();
			if ( session != null ) {
				session.close();
			}
		}
	}
}
