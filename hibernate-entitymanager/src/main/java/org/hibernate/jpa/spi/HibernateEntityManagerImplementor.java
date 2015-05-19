/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.spi;

import java.util.List;
import java.util.Map;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceException;
import javax.persistence.criteria.Selection;

import org.hibernate.HibernateException;
import org.hibernate.LockOptions;
import org.hibernate.StaleStateException;
import org.hibernate.jpa.HibernateEntityManager;
import org.hibernate.jpa.criteria.ValueHandlerFactory;
import org.hibernate.jpa.internal.QueryImpl;
import org.hibernate.type.Type;

/**
 * Additional internal contracts for the Hibernate {@link javax.persistence.EntityManager} implementation.
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public interface HibernateEntityManagerImplementor extends HibernateEntityManager, HibernateEntityManagerFactoryAware {


	/**
	 * Used to ensure the EntityManager is open, throwing IllegalStateException if it is closed.
	 *
	 * Depending on the value of {@code markForRollbackIfClosed}, may also rollback any enlisted-in transaction.  This
	 * distinction is made across various sections of the spec.  Most failed checks should rollback.  Section
	 * 3.10.7 (per 2.1 spec) lists cases related to calls on related query objects that should not rollback.
	 *
	 * @param markForRollbackIfClosed If the EM is closed, should the transaction (if one) be marked for rollback?
	 *
	 * @throws IllegalStateException Thrown if the EM is closed
	 */
	public void checkOpen(boolean markForRollbackIfClosed) throws IllegalStateException;

	/**
	 * Provides access to whether a transaction is currently in progress.
	 *
	 * @return True if a transaction is considered currently in progress; false otherwise.
	 */
	boolean isTransactionInProgress();

	/**
	 * Used to mark a transaction for rollback only (when that is the JPA spec defined behavior).
	 */
	public void markForRollbackOnly();

	/**
	 * Handles marking for rollback and other such operations that need to occur depending on the type of
	 * exception being handled.
	 *
	 * @param e The exception being handled.
	 */
	public void handlePersistenceException(PersistenceException e);

	/**
	 * Delegates to {@link #handlePersistenceException} and then throws the given exception.
	 *
	 * @param e The exception being handled and finally thrown.
	 */
	public void throwPersistenceException(PersistenceException e);

	/**
	 * Converts a Hibernate-specific exception into a JPA-specified exception; note that the JPA sepcification makes use
	 * of exceptions outside its exception hierarchy, though they are all runtime exceptions.
	 * <p/>
	 * Any appropriate/needed calls to {@link #handlePersistenceException} are also made.
	 *
	 * @param e The Hibernate excepton.
	 * @param lockOptions The lock options in effect at the time of exception (can be null)
	 *
	 * @return The JPA-specified exception
	 */
	public RuntimeException convert(HibernateException e, LockOptions lockOptions);

	/**
	 * Converts a Hibernate-specific exception into a JPA-specified exception; note that the JPA sepcification makes use
	 * of exceptions outside its exception hierarchy, though they are all runtime exceptions.
	 * <p/>
	 * Any appropriate/needed calls to {@link #handlePersistenceException} are also made.
	 *
	 * @param e The Hibernate excepton.
	 *
	 * @return The JPA-specified exception
	 */
	public RuntimeException convert(HibernateException e);

	/**
	 * Delegates to {@link #convert} and then throws the given exception.
	 *
	 * @param e The exception being handled and finally thrown.
	 */
	public void throwPersistenceException(HibernateException e);

	public PersistenceException wrapStaleStateException(StaleStateException e);

	/**
	 * Convert from JPA 2 {@link javax.persistence.LockModeType} & properties into {@link org.hibernate.LockOptions}
	 *
	 * @param lockModeType is the requested lock type
	 * @param properties are the lock properties
	 *
	 * @return the LockOptions
	 */
	public LockOptions getLockRequest(LockModeType lockModeType, Map<String, Object> properties);

	public static interface QueryOptions {
		public static interface ResultMetadataValidator {
			public void validate(Type[] returnTypes);
		}

		public ResultMetadataValidator getResultMetadataValidator();

		/**
		 * Get the conversions for the individual tuples in the query results.
		 *
		 * @return Value conversions to be applied to the JPA QL results
		 */
		public List<ValueHandlerFactory.ValueHandler> getValueHandlers();

		/**
		 * Get the explicit parameter types.  Generally speaking these would apply to implicit named
		 * parameters.
		 *
		 * @return The
		 */
		public Map<String, Class> getNamedParameterExplicitTypes();
	}

	/**
	 * Used during "compiling" a JPA criteria query.
	 *
	 * @param jpaqlString The criteria query rendered as a JPA QL string
	 * @param resultClass The result type (the type expected in the result list)
	 * @param selection The selection(s)
	 * @param queryOptions The options to use to build the query.
	 * @param <T> The query type
	 *
	 * @return The typed query
	 */
	public <T> QueryImpl<T> createQuery(String jpaqlString, Class<T> resultClass, Selection selection, QueryOptions queryOptions);
}
