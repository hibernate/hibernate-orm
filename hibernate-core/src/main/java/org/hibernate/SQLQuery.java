/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.persistence.FlushModeType;
import javax.persistence.Parameter;
import javax.persistence.TemporalType;

import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.QueryParameter;
import org.hibernate.type.Type;

/**
 * Represents a "native sql" query.
 *
 * Allows the user to define certain aspects about its execution, such as:<ul>
 *     <li>
 *         result-set value mapping (see below)
 *     </li>
 *     <li>
 *         Tables used via {@link #addSynchronizedQuerySpace}, {@link #addSynchronizedEntityName} and
 *         {@link #addSynchronizedEntityClass}.  This allows Hibernate to know how to properly deal with
 *         auto-flush checking as well as cached query results if the results of the query are being
 *         cached.
 *     </li>
 * </ul>
 *
 * In terms of result-set mapping, there are 3 approaches to defining:<ul>
 *     <li>
 *         If this represents a named sql query, the mapping could be associated with the query as part
 *         of its metadata
 *     </li>
 *     <li>
 *         A pre-defined (defined in metadata and named) mapping can be associated with
 *         {@link #setResultSetMapping}
 *     </li>
 *     <li>
 *         Defined locally per the various {@link #addEntity}, {@link #addRoot}, {@link #addJoin},
 *         {@link #addFetch} and {@link #addScalar} methods
 *     </li>
 * </ul>
 * 
 * @author Gavin King
 * @author Steve Ebersole
 *
 * @deprecated (since 5.2) use {@link NativeQuery} instead.
 */
@Deprecated
public interface SQLQuery extends Query<Object>, SynchronizeableQuery {
	/**
	 * Use a predefined named result-set mapping.  This might be defined by a {@code <result-set/>} element in a
	 * Hibernate <tt>hbm.xml</tt> file or through a {@link javax.persistence.SqlResultSetMapping} annotation.
	 *
	 * @param name The name of the mapping to use.
	 *
	 * @return this, for method chaining
	 */
	NativeQuery setResultSetMapping(String name);

	/**
	 * Is this native-SQL query known to be callable?
	 *
	 * @return {@code true} if the query is known to be callable; {@code false} otherwise.
	 */
	boolean isCallable();

	/**
	 * Retrieve the returns associated with this query.
	 *
	 * @return The return descriptors
	 */
	List<NativeSQLQueryReturn> getQueryReturns();

	/**
	 * Declare a scalar query result. Hibernate will attempt to automatically detect the underlying type.
	 * <p/>
	 * Functions like {@code <return-scalar/>} in {@code hbm.xml} or {@link javax.persistence.ColumnResult}
	 *
	 * @param columnAlias The column alias in the result-set to be processed as a scalar result
	 *
	 * @return {@code this}, for method chaining
	 */
	NativeQuery addScalar(String columnAlias);

	/**
	 * Declare a scalar query result.
	 * <p/>
	 * Functions like {@code <return-scalar/>} in {@code hbm.xml} or {@link javax.persistence.ColumnResult}
	 *
	 * @param columnAlias The column alias in the result-set to be processed as a scalar result
	 * @param type The Hibernate type as which to treat the value.
	 *
	 * @return {@code this}, for method chaining
	 */
	NativeQuery addScalar(String columnAlias, Type type);

	/**
	 * Add a new root return mapping, returning a {@link NativeQuery.RootReturn} to allow further definition.
	 *
	 * @param tableAlias The SQL table alias to map to this entity
	 * @param entityName The name of the entity.
	 *
	 * @return The return config object for further control.
	 *
	 * @since 3.6
	 */
	RootReturn addRoot(String tableAlias, String entityName);

	/**
	 * Add a new root return mapping, returning a {@link NativeQuery.RootReturn} to allow further definition.
	 *
	 * @param tableAlias The SQL table alias to map to this entity
	 * @param entityType The java type of the entity.
	 *
	 * @return The return config object for further control.
	 *
	 * @since 3.6
	 */
	RootReturn addRoot(String tableAlias, Class entityType);

	/**
	 * Declare a "root" entity, without specifying an alias.  The expectation here is that the table alias is the
	 * same as the unqualified entity name
	 * <p/>
	 * Use {@link #addRoot} if you need further control of the mapping
	 *
	 * @param entityName The entity name that is the root return of the query.
	 *
	 * @return {@code this}, for method chaining
	 */
	NativeQuery addEntity(String entityName);

	/**
	 * Declare a "root" entity.
	 *
	 * @param tableAlias The SQL table alias
	 * @param entityName The entity name
	 *
	 * @return {@code this}, for method chaining
	 */
	NativeQuery addEntity(String tableAlias, String entityName);

	/**
	 * Declare a "root" entity, specifying a lock mode.
	 *
	 * @param tableAlias The SQL table alias
	 * @param entityName The entity name
	 * @param lockMode The lock mode for this return.
	 *
	 * @return {@code this}, for method chaining
	 */
	NativeQuery addEntity(String tableAlias, String entityName, LockMode lockMode);

	/**
	 * Declare a "root" entity, without specifying an alias.  The expectation here is that the table alias is the
	 * same as the unqualified entity name
	 *
	 * @param entityType The java type of the entity to add as a root
	 *
	 * @return {@code this}, for method chaining
	 */
	NativeQuery addEntity(Class entityType);

	/**
	 * Declare a "root" entity.
	 *
	 * @param tableAlias The SQL table alias
	 * @param entityType The java type of the entity to add as a root
	 *
	 * @return {@code this}, for method chaining
	 */
	NativeQuery addEntity(String tableAlias, Class entityType);

	/**
	 * Declare a "root" entity, specifying a lock mode.
	 *
	 * @param tableAlias The SQL table alias
	 * @param entityClass The entity Class
	 * @param lockMode The lock mode for this return.
	 *
	 * @return {@code this}, for method chaining
	 */
	NativeQuery addEntity(String tableAlias, Class entityClass, LockMode lockMode);

	/**
	 * Declare a join fetch result.
	 *
	 * @param tableAlias The SQL table alias for the data to be mapped to this fetch
	 * @param ownerTableAlias Identify the table alias of the owner of this association.  Should match the alias of a
	 * previously added root or fetch
	 * @param joinPropertyName The name of the property being join fetched.
	 *
	 * @return The return config object for further control.
	 *
	 * @since 3.6
	 */
	FetchReturn addFetch(String tableAlias, String ownerTableAlias, String joinPropertyName);

	/**
	 * Declare a join fetch result.
	 *
	 * @param tableAlias The SQL table alias for the data to be mapped to this fetch
	 * @param path The association path ([owner-alias].[property-name]).
	 *
	 * @return {@code this}, for method chaining
	 */
	NativeQuery addJoin(String tableAlias, String path);

	/**
	 * Declare a join fetch result.
	 *
	 * @param tableAlias The SQL table alias for the data to be mapped to this fetch
	 * @param ownerTableAlias Identify the table alias of the owner of this association.  Should match the alias of a
	 * previously added root or fetch
	 * @param joinPropertyName The name of the property being join fetched.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @since 3.6
	 */
	NativeQuery addJoin(String tableAlias, String ownerTableAlias, String joinPropertyName);

	/**
	 * Declare a join fetch result, specifying a lock mode.
	 *
	 * @param tableAlias The SQL table alias for the data to be mapped to this fetch
	 * @param path The association path ([owner-alias].[property-name]).
	 * @param lockMode The lock mode for this return.
	 *
	 * @return {@code this}, for method chaining
	 */
	NativeQuery addJoin(String tableAlias, String path, LockMode lockMode);

	/**
	 * Allows access to further control how properties within a root or join fetch are mapped back from the result set.
	 * Generally used in composite value scenarios.
	 */
	interface ReturnProperty {
		/**
		 * Add a column alias to this property mapping.
		 *
		 * @param columnAlias The column alias.
		 *
		 * @return {@code this}, for method chaining
		 */
		ReturnProperty addColumnAlias(String columnAlias);
	}

	/**
	 * Allows access to further control how root returns are mapped back from result sets.
	 */
	interface RootReturn {
		/**
		 * Set the lock mode for this return.
		 *
		 * @param lockMode The new lock mode.
		 *
		 * @return {@code this}, for method chaining
		 */
		RootReturn setLockMode(LockMode lockMode);

		/**
		 * Name the column alias that identifies the entity's discriminator.
		 *
		 * @param columnAlias The discriminator column alias
		 *
		 * @return {@code this}, for method chaining
		 */
		RootReturn setDiscriminatorAlias(String columnAlias);

		/**
		 * Add a simple property-to-one-column mapping.
		 *
		 * @param propertyName The name of the property.
		 * @param columnAlias The name of the column
		 *
		 * @return {@code this}, for method chaining
		 */
		RootReturn addProperty(String propertyName, String columnAlias);

		/**
		 * Add a property, presumably with more than one column.
		 *
		 * @param propertyName The name of the property.
		 *
		 * @return The config object for further control.
		 */
		ReturnProperty addProperty(String propertyName);
	}

	/**
	 * Allows access to further control how join fetch returns are mapped back from result sets.
	 */
	interface FetchReturn {
		/**
		 * Set the lock mode for this return.
		 *
		 * @param lockMode The new lock mode.
		 *
		 * @return {@code this}, for method chaining
		 */
		FetchReturn setLockMode(LockMode lockMode);

		/**
		 * Add a simple property-to-one-column mapping.
		 *
		 * @param propertyName The name of the property.
		 * @param columnAlias The name of the column
		 *
		 * @return {@code this}, for method chaining
		 */
		FetchReturn addProperty(String propertyName, String columnAlias);

		/**
		 * Add a property, presumably with more than one column.
		 *
		 * @param propertyName The name of the property.
		 *
		 * @return The config object for further control.
		 */
		ReturnProperty addProperty(String propertyName);
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// overrides


	@Override
	NativeQuery setHibernateFlushMode(FlushMode flushMode);

	@Override
	NativeQuery setFlushMode(FlushModeType flushMode);

	@Override
	NativeQuery setCacheMode(CacheMode cacheMode);

	@Override
	NativeQuery setCacheable(boolean cacheable);

	@Override
	NativeQuery setCacheRegion(String cacheRegion);

	@Override
	NativeQuery setTimeout(int timeout);

	@Override
	NativeQuery setFetchSize(int fetchSize);

	@Override
	NativeQuery setReadOnly(boolean readOnly);

	@Override
	NativeQuery setLockOptions(LockOptions lockOptions);

	@Override
	NativeQuery setLockMode(String alias, LockMode lockMode);

	@Override
	NativeQuery setComment(String comment);

	@Override
	NativeQuery addQueryHint(String hint);

	@Override
	<T> NativeQuery setParameter(QueryParameter<T> parameter, T val);

	@Override
	<T> NativeQuery setParameter(Parameter<T> param, T value);

	@Override
	NativeQuery setParameter(String name, Object val);

	@Override
	NativeQuery setParameter(int position, Object val);

	@Override
	<P> NativeQuery setParameter(QueryParameter<P> parameter, P val, Type type);

	@Override
	NativeQuery setParameter(String name, Object val, Type type);

	@Override
	NativeQuery setParameter(int position, Object val, Type type);

	@Override
	<P> NativeQuery setParameter(QueryParameter<P> parameter, P val, TemporalType temporalType);

	@Override
	NativeQuery setParameter(String name, Object val, TemporalType temporalType);

	@Override
	NativeQuery setParameter(int position, Object val, TemporalType temporalType);

	@Override
	<P> NativeQuery setParameterList(QueryParameter<P> parameter, Collection<P> values);

	@Override
	NativeQuery setParameterList(String name, Collection values);

	@Override
	NativeQuery setParameterList(String name, Collection values, Type type);

	@Override
	NativeQuery setParameterList(String name, Object[] values, Type type);

	@Override
	NativeQuery setParameterList(String name, Object[] values);

	@Override
	NativeQuery setProperties(Object bean);

	@Override
	NativeQuery setProperties(Map bean);

	@Override
	NativeQuery setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType);

	@Override
	NativeQuery setParameter(Parameter<Date> param, Date value, TemporalType temporalType);

	@Override
	NativeQuery setParameter(String name, Calendar value, TemporalType temporalType);

	@Override
	NativeQuery setParameter(String name, Date value, TemporalType temporalType);

	@Override
	NativeQuery setParameter(int position, Calendar value, TemporalType temporalType);

	@Override
	NativeQuery setParameter(int position, Date value, TemporalType temporalType);

	@Override
	NativeQuery addSynchronizedQuerySpace(String querySpace);

	@Override
	NativeQuery addSynchronizedEntityName(String entityName) throws MappingException;

	@Override
	NativeQuery addSynchronizedEntityClass(Class entityClass) throws MappingException;

	@Override
	NativeQuery setFlushMode(FlushMode flushMode);

}
