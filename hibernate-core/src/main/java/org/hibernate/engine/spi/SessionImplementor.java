/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import javax.persistence.LockModeType;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;

import org.hibernate.HibernateException;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.query.spi.NativeQueryImplementor;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.spi.WrapperOptions;

/**
 * Defines the "internal contract" for {@link Session} and other parts of Hibernate such as
 * {@link Type}, {@link EntityTypeDescriptor}
 * and {@link PersistentCollectionDescriptor} implementations.
 *
 * A Session, through this interface and SharedSessionContractImplementor, implements:<ul>
 *     <li>
 *         {@link org.hibernate.resource.jdbc.spi.JdbcSessionOwner} to drive the behavior of the
 *         {@link org.hibernate.resource.jdbc.spi.JdbcSessionContext} delegate
 *     </li>
 *     <li>
 *         {@link TransactionCoordinatorBuilder.Options}
 *         to drive the creation of the {@link TransactionCoordinator} delegate
 *     </li>
 *     <li>
 *         {@link org.hibernate.engine.jdbc.LobCreationContext} to act as the context for JDBC LOB instance creation
 *     </li>
 *     <li>
 *         {@link WrapperOptions} to fulfill the behavior needed while
 *         binding/extracting values to/from JDBC as part of the Type contracts
 *     </li>
 * </ul>
 *
 * See also {@link org.hibernate.event.spi.EventSource} which extends this interface providing
 * bridge to the event generation features of {@link org.hibernate.event}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface SessionImplementor extends Session, SharedSessionContractImplementor {

	@Override
	SessionFactoryImplementor getSessionFactory();

	@Override
	<T> RootGraphImplementor<T> createEntityGraph(Class<T> rootType);

	@Override
	RootGraphImplementor<?> createEntityGraph(String graphName);

	@Override
	RootGraphImplementor<?> getEntityGraph(String graphName);

	ActionQueue getActionQueue();

	Object instantiate(EntityTypeDescriptor entityDescriptor, Object id) throws HibernateException;

	/**
	 * @deprecated (since 6.0) Use {@link #instantiate(EntityTypeDescriptor, Object)}
	 */
	@Deprecated
	default Object instantiate(EntityTypeDescriptor entityDescriptor, Serializable id) throws HibernateException {
		return instantiate( entityDescriptor, (Object) id );
	}

	void forceFlush(EntityEntry e) throws HibernateException;

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

	@Override
	QueryImplementor createQuery(String queryString);

	@Override
	<T> QueryImplementor<T> createQuery(String queryString, Class<T> resultType);

	@Override
	<T> QueryImplementor<T> createNamedQuery(String name, Class<T> resultType);

	@Override
	QueryImplementor createNamedQuery(String name);

	@Override
	NativeQueryImplementor createNativeQuery(String sqlString);

	@Override
	NativeQueryImplementor createNativeQuery(String sqlString, Class resultClass);

	@Override
	NativeQueryImplementor createNativeQuery(String sqlString, String resultSetMapping);

	@Override
	NativeQueryImplementor getNamedNativeQuery(String name);

	@Override
	QueryImplementor getNamedQuery(String queryName);

	@Override
	<T> QueryImplementor<T> createQuery(CriteriaQuery<T> criteriaQuery);

	@Override
	QueryImplementor createQuery(CriteriaUpdate updateQuery);

	@Override
	QueryImplementor createQuery(CriteriaDelete deleteQuery);

	/**
	 * @deprecated  OperationalContext should cover this overload I believe; Gail?
	 */
	@Deprecated
	void merge(String entityName, Object object, Map copiedAlready) throws HibernateException;

	/**
	 * @deprecated  OperationalContext should cover this overload I believe; Gail?
	 */
	@Deprecated
	void persist(String entityName, Object object, Map createdAlready) throws HibernateException;

	/**
	 * @deprecated  OperationalContext should cover this overload I believe; Gail?
	 */
	@Deprecated
	void persistOnFlush(String entityName, Object object, Map copiedAlready);

	/**
	 * @deprecated  OperationalContext should cover this overload I believe; Gail?
	 */
	@Deprecated
	void refresh(String entityName, Object object, Map refreshedAlready) throws HibernateException;

	/**
	 * @deprecated  OperationalContext should cover this overload I believe; Gail?
	 */
	@Deprecated
	void delete(String entityName, Object child, boolean isCascadeDeleteEnabled, Set transientEntities);

	/**
	 * @deprecated  OperationalContext should cover this overload I believe; Gail?
	 */
	@Deprecated
	void removeOrphanBeforeUpdates(String entityName, Object child);

	SessionImplementor getSession();

	/**
	 * Given a JPA {@link javax.persistence.LockModeType} and properties, build a Hibernate
	 * {@link org.hibernate.LockOptions}
	 *
	 * @param lockModeType the requested LockModeType
	 * @param properties the lock properties
	 *
	 * @return the LockOptions
	 */
	LockOptions buildLockOptions(LockModeType lockModeType, Map<String, Object> properties);

	interface QueryOptions {
		interface ResultMetadataValidator {
			void validate(Type[] returnTypes);
		}

		ResultMetadataValidator getResultMetadataValidator();

		/**
		 * Get the explicit parameter types.  Generally speaking these would apply to implicit named
		 * parameters.
		 *
		 * @return The
		 */
		Map<String, Class> getNamedParameterExplicitTypes();
	}

}
