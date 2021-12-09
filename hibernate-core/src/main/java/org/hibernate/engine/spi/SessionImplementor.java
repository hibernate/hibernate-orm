/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import java.util.Map;
import java.util.Set;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.sql.spi.NativeQueryImplementor;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;

/**
 * Defines the "internal contract" for {@link Session} and other parts of Hibernate such as
 * {@link org.hibernate.type.Type}, {@link EntityPersister}
 * and {@link org.hibernate.persister.collection.CollectionPersister} implementations.
 *
 * A Session, through this interface and SharedSessionContractImplementor, implements:
 * <ul>
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
 *         {@link org.hibernate.type.descriptor.WrapperOptions} to fulfill the behavior needed while
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
	default SessionImplementor getSession() {
		return this;
	}

	@Override
	SessionFactoryImplementor getSessionFactory();

	@Override
	<T> RootGraphImplementor<T> createEntityGraph(Class<T> rootType);

	@Override
	RootGraphImplementor<?> createEntityGraph(String graphName);

	@Override
	RootGraphImplementor<?> getEntityGraph(String graphName);


	/**
	 * @deprecated (since 5.2) use {@link #getHibernateFlushMode()} instead.
	 */
	@Deprecated
	boolean isFlushBeforeCompletionEnabled();

	ActionQueue getActionQueue();

	Object instantiate(EntityPersister persister, Object id) throws HibernateException;

	void forceFlush(EntityEntry e) throws HibernateException;

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


	// The following declarations just override the JPA return type with our
	// Hibernate QueryImplementor type, as required by the declaration in
	// QueryProducerImplementor

	@Override @SuppressWarnings({"rawtypes", "unchecked"})
	QueryImplementor getNamedQuery(String queryString);

	@Override @SuppressWarnings({"rawtypes", "unchecked"})
	QueryImplementor createQuery(String queryString);

	@Override
	<R> QueryImplementor<R> createQuery(String queryString, Class<R> resultType);

	@Override @SuppressWarnings({"rawtypes", "unchecked"})
	QueryImplementor createNamedQuery(String queryString);

	@Override
	<R> QueryImplementor<R> createNamedQuery(String name, Class<R> resultType);

	@Override @SuppressWarnings({"rawtypes", "unchecked"})
	NativeQueryImplementor createNativeQuery(String queryString);

	@Override @SuppressWarnings({"rawtypes", "unchecked"})
	NativeQueryImplementor createNativeQuery(String queryString, String resultSetMappingName);

	@Override
	<R> QueryImplementor<R> createQuery(CriteriaQuery<R> criteriaQuery);

	@Override @SuppressWarnings({"rawtypes", "unchecked"})
	QueryImplementor createQuery(CriteriaUpdate updateQuery);

	@Override @SuppressWarnings({"rawtypes", "unchecked"})
	QueryImplementor createQuery(CriteriaDelete deleteQuery);
}
