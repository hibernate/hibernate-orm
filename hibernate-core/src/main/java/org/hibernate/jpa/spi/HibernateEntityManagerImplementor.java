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
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.jpa.HibernateEntityManager;
import org.hibernate.query.Query;
import org.hibernate.query.criteria.internal.ValueHandlerFactory;
import org.hibernate.type.Type;

/**
 * Additional internal contracts for the Hibernate {@link javax.persistence.EntityManager} implementation.
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 *
 * @deprecated (since 5.2) move these methods to SessionImplementor
 */
@Deprecated
public interface HibernateEntityManagerImplementor extends HibernateEntityManager, HibernateEntityManagerFactoryAware {
	@Override
	SessionImplementor getSession();

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
	void checkOpen(boolean markForRollbackIfClosed) throws IllegalStateException;

	/**
	 * Provides access to whether a transaction is currently in progress.
	 *
	 * @return True if a transaction is considered currently in progress; false otherwise.
	 */
	boolean isTransactionInProgress();

	/**
	 * Used to mark a transaction for rollback only (when that is the JPA spec defined behavior).
	 */
	void markForRollbackOnly();

	/**
	 * Convert from JPA 2 {@link javax.persistence.LockModeType} & properties into {@link org.hibernate.LockOptions}
	 *
	 * @param lockModeType is the requested lock type
	 * @param properties are the lock properties
	 *
	 * @return the LockOptions
	 *
	 * @deprecated (since 5.2) use {@link #buildLockOptions(LockModeType, Map)} instead
	 */
	@Deprecated
	LockOptions getLockRequest(LockModeType lockModeType, Map<String, Object> properties);

	/**
	 * Given a JPA {@link javax.persistence.LockModeType} and properties, build a Hibernate
	 * {@link org.hibernate.LockOptions}
	 *
	 * @param lockModeType the requested LockModeType
	 * @param properties the lock properties
	 *
	 * @return the LockOptions
	 */
	default LockOptions buildLockOptions(LockModeType lockModeType, Map<String, Object> properties) {
		return getLockRequest( lockModeType, properties );
	}

	interface QueryOptions {
		interface ResultMetadataValidator {
			void validate(Type[] returnTypes);
		}

		ResultMetadataValidator getResultMetadataValidator();

		/**
		 * Get the conversions for the individual tuples in the query results.
		 *
		 * @return Value conversions to be applied to the JPA QL results
		 */
		List<ValueHandlerFactory.ValueHandler> getValueHandlers();

		/**
		 * Get the explicit parameter types.  Generally speaking these would apply to implicit named
		 * parameters.
		 *
		 * @return The
		 */
		Map<String, Class> getNamedParameterExplicitTypes();
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
	 * @deprecated (since 5.2) this method form is used to construct a "compiled" representation of
	 * a JPA Criteria query.  However it assumes the old yucky implementation of "compilation" that
	 * converted the Criteria into a HQL/JPQL string.  In 6.0 that is re-written from scratch to
	 * compile to SQM, and so this method would not be needed in 6.0
	 *
	 * @return The typed query
	 */
	@Deprecated
	<T> Query<T> createQuery(
			String jpaqlString,
			Class<T> resultClass,
			Selection selection,
			QueryOptions queryOptions);
}
