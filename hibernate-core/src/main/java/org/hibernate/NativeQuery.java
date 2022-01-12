/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate;

import java.time.Instant;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.TemporalType;
import jakarta.persistence.metamodel.SingularAttribute;

import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.query.BindableType;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.SynchronizeableQuery;
import org.hibernate.query.TupleTransformer;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.type.BasicTypeReference;

/**
 * Represents a native (SQL) query.
 *
 * Allows the user to define certain aspects about its execution, such as:<ul>
 *     <li>
 *         result-set value mapping (see below)
 *     </li>
 *     <li>
 *         Tables used via {@link #addSynchronizedQuerySpace}, {@link #addSynchronizedEntityName} and
 *         {@link org.hibernate.query.SynchronizeableQuery#addSynchronizedEntityClass}.  This allows Hibernate to know how to properly deal with
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
 *         A pre-defined (defined in metadata and named) mapping can be associated via {@link org.hibernate.Session#createNativeQuery(String, String)}
 *     </li>
 *     <li>
 *         Defined locally per the various {@link #addEntity}, {@link #addRoot}, {@link #addJoin},
 *         {@link #addFetch} and {@link #addScalar} methods
 *     </li>
 * </ul>
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface NativeQuery<T> extends Query<T>, SynchronizeableQuery {
	/**
	 * Declare a scalar query result. Hibernate will attempt to automatically
	 * detect the underlying type.
	 * <p/>
	 * Functions like {@code <return-scalar/>} in {@code hbm.xml} or
	 * {@link jakarta.persistence.ColumnResult} in annotations
	 *
	 * @param columnAlias The column alias in the result-set to be processed
	 * 		as a scalar result
	 *
	 * @return {@code this}, for method chaining
	 */
	NativeQuery<T> addScalar(String columnAlias);

	/**
	 * Declare a scalar query result.
	 * <p/>
	 * Functions like {@code <return-scalar/>} in {@code hbm.xml} or
	 * {@link jakarta.persistence.ColumnResult} in annotations
	 *
	 * @param columnAlias The column alias in the result-set to be processed
	 * 		as a scalar result
	 * @param type The Hibernate type as which to treat the value.
	 *
	 * @return {@code this}, for method chaining
	 */
	NativeQuery<T> addScalar(String columnAlias, @SuppressWarnings("rawtypes") BasicTypeReference type);

	/**
	 * Declare a scalar query result.
	 * <p/>
	 * Functions like {@code <return-scalar/>} in {@code hbm.xml} or
	 * {@link jakarta.persistence.ColumnResult} in annotations
	 *
	 * @param columnAlias The column alias in the result-set to be processed
	 * 		as a scalar result
	 * @param type The Hibernate type as which to treat the value.
	 *
	 * @return {@code this}, for method chaining
	 */
	NativeQuery<T> addScalar(String columnAlias, @SuppressWarnings("rawtypes") BasicDomainType type);

	/**
	 * Declare a scalar query result using the specified result type.
	 *
	 * Hibernate will implicitly determine an appropriate conversion, if
	 * it can.  Otherwise an exception will be thrown
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @since 6.0
	 */
	NativeQuery<T> addScalar(String columnAlias, @SuppressWarnings("rawtypes") Class javaType);

	/**
	 * Declare a scalar query result with an explicit conversion
	 *
	 * @param relationalJavaType The Java type expected by the converter as its
	 * "relational" type.
	 * @param converter The conversion to apply.  Consumes the JDBC value based
	 * on `relationalJavaType`.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @since 6.0
	 */
	<C> NativeQuery<T> addScalar(String columnAlias, Class<C> relationalJavaType, AttributeConverter<?,C> converter);

	/**
	 * Declare a scalar query result with an explicit conversion
	 *
	 * @param jdbcJavaType The Java type expected by the converter as its "relational model" type.
	 * @param domainJavaType The Java type expected by the converter as its "object model" type.
	 * @param converter The conversion to apply.  Consumes the JDBC value based on `relationalJavaType`.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @since 6.0
	 */
	<O,R> NativeQuery<T> addScalar(String columnAlias, Class<O> domainJavaType, Class<R> jdbcJavaType, AttributeConverter<O,R> converter);

	/**
	 * Declare a scalar query result with an explicit conversion
	 *
	 * @param relationalJavaType The Java type expected by the converter as its
	 * "relational" type.
	 * @param converter The conversion to apply.  Consumes the JDBC value based
	 * on `relationalJavaType`.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @since 6.0
	 */
	<C> NativeQuery<T> addScalar(String columnAlias, Class<C> relationalJavaType, Class<? extends AttributeConverter<?,C>> converter);

	/**
	 * Declare a scalar query result with an explicit conversion
	 *
	 * @param jdbcJavaType The Java type expected by the converter as its "relational model" type.
	 * @param domainJavaType The Java type expected by the converter as its "object model" type.
	 * @param converter The conversion to apply.  Consumes the JDBC value based on `jdbcJavaType`.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @since 6.0
	 */
	<O,R> NativeQuery<T> addScalar(
			String columnAlias,
			Class<O> domainJavaType,
			Class<R> jdbcJavaType,
			Class<? extends AttributeConverter<O,R>> converter);

	<J> InstantiationResultNode<J> addInstantiation(Class<J> targetJavaType);

	/**
	 * Defines a result based on a specified attribute.  Differs from adding a scalar in that
	 * any conversions or other semantics defined on the attribute are automatically applied
	 * to the mapping
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @since 6.0
	 */
	NativeQuery<T> addAttributeResult(String columnAlias, @SuppressWarnings("rawtypes") Class entityJavaType, String attributePath);

	/**
	 * Defines a result based on a specified attribute.  Differs from adding a scalar in that
	 * any conversions or other semantics defined on the attribute are automatically applied
	 * to the mapping
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @since 6.0
	 */
	NativeQuery<T> addAttributeResult(String columnAlias, String entityName, String attributePath);

	/**
	 * Defines a result based on a specified attribute. Differs from adding a scalar in that
	 * any conversions or other semantics defined on the attribute are automatically applied
	 * to the mapping.
	 *
	 * This form accepts the JPA Attribute mapping describing the attribute
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @since 6.0
	 */
	NativeQuery<T> addAttributeResult(String columnAlias, @SuppressWarnings("rawtypes") SingularAttribute attribute);

	/**
	 * Add a new root return mapping, returning a {@link RootReturn} to allow
	 * further definition.
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
	 * Add a new root return mapping, returning a {@link RootReturn} to allow further definition.
	 *
	 * @param tableAlias The SQL table alias to map to this entity
	 * @param entityType The java type of the entity.
	 *
	 * @return The return config object for further control.
	 *
	 * @since 3.6
	 */
	RootReturn addRoot(String tableAlias, @SuppressWarnings("rawtypes") Class entityType);

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
	NativeQuery<T> addEntity(String entityName);

	/**
	 * Declare a "root" entity.
	 *
	 * @param tableAlias The SQL table alias
	 * @param entityName The entity name
	 *
	 * @return {@code this}, for method chaining
	 */
	NativeQuery<T> addEntity(String tableAlias, String entityName);

	/**
	 * Declare a "root" entity, specifying a lock mode.
	 *
	 * @param tableAlias The SQL table alias
	 * @param entityName The entity name
	 * @param lockMode The lock mode for this return.
	 *
	 * @return {@code this}, for method chaining
	 */
	NativeQuery<T> addEntity(String tableAlias, String entityName, LockMode lockMode);

	/**
	 * Declare a "root" entity, without specifying an alias.  The expectation here is that the table alias is the
	 * same as the unqualified entity name
	 *
	 * @param entityType The java type of the entity to add as a root
	 *
	 * @return {@code this}, for method chaining
	 */
	NativeQuery<T> addEntity(@SuppressWarnings("rawtypes") Class entityType);

	/**
	 * Declare a "root" entity.
	 *
	 * @param tableAlias The SQL table alias
	 * @param entityType The java type of the entity to add as a root
	 *
	 * @return {@code this}, for method chaining
	 */
	NativeQuery<T> addEntity(String tableAlias, @SuppressWarnings("rawtypes") Class entityType);

	/**
	 * Declare a "root" entity, specifying a lock mode.
	 *
	 * @param tableAlias The SQL table alias
	 * @param entityClass The entity Class
	 * @param lockMode The lock mode for this return.
	 *
	 * @return {@code this}, for method chaining
	 */
	NativeQuery<T> addEntity(String tableAlias, @SuppressWarnings("rawtypes") Class entityClass, LockMode lockMode);

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
	NativeQuery<T> addJoin(String tableAlias, String path);

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
	NativeQuery<T> addJoin(String tableAlias, String ownerTableAlias, String joinPropertyName);

	/**
	 * Declare a join fetch result, specifying a lock mode.
	 *
	 * @param tableAlias The SQL table alias for the data to be mapped to this fetch
	 * @param path The association path ([owner-alias].[property-name]).
	 * @param lockMode The lock mode for this return.
	 *
	 * @return {@code this}, for method chaining
	 */
	NativeQuery<T> addJoin(String tableAlias, String path, LockMode lockMode);

	/**
	 * Simple unification interface for all returns from the various `#addXYZ` methods .
	 * Allows control over the "shape" of that particular part of the fetch graph.
	 *
	 * Some GraphNodes can be query results, while others simply describe a part
	 * of one of the results.
	 */
	interface ResultNode {
	}

	/**
	 * ResultNode which can be a query result
	 */
	interface ReturnableResultNode extends ResultNode {
	}

	interface InstantiationResultNode<J> extends ReturnableResultNode {
		default InstantiationResultNode<J> addBasicArgument(String columnAlias) {
			return addBasicArgument( columnAlias, null );
		}

		InstantiationResultNode<J> addBasicArgument(String columnAlias, String argumentAlias);
	}

	/**
	 * Allows access to further control how properties within a root or join
	 * fetch are mapped back from the result set.   Generally used in composite
	 * value scenarios.
	 */
	interface ReturnProperty extends ResultNode {
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
	 * Allows access to further control how root returns are mapped back from
	 * result sets.
	 */
	interface RootReturn extends ReturnableResultNode {

		String getTableAlias();

		String getDiscriminatorAlias();

		EntityMappingType getEntityMapping();

		NavigablePath getNavigablePath();

		/**
		 * Set the lock mode for this return.
		 *
		 * @param lockMode The new lock mode.
		 *
		 * @return {@code this}, for method chaining
		 */
		RootReturn setLockMode(LockMode lockMode);

		RootReturn addIdColumnAliases(String... aliases);

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

	interface CollectionReturn extends ReturnableResultNode {

		String getTableAlias();

		PluralAttributeMapping getPluralAttribute();

		NavigablePath getNavigablePath();
	}

	/**
	 * Allows access to further control how join fetch returns are mapped back
	 * from result sets.
	 */
	interface FetchReturn extends ResultNode {

		String getTableAlias();

		String getOwnerAlias();

		String getFetchableName();

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
	// covariant overrides - SynchronizeableQuery
	@Override
	NativeQuery<T> addSynchronizedQuerySpace(String querySpace);

	@Override
	NativeQuery<T> addSynchronizedEntityName(String entityName) throws MappingException;

	@Override
	NativeQuery<T> addSynchronizedEntityClass(@SuppressWarnings("rawtypes") Class entityClass) throws MappingException;


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// covariant overrides - Query

	@Override
	NativeQuery<T> setHibernateFlushMode(FlushMode flushMode);

	@Override
	NativeQuery<T> setFlushMode(FlushModeType flushMode);

	@Override
	NativeQuery<T> setCacheMode(CacheMode cacheMode);

	@Override
	NativeQuery<T> setCacheable(boolean cacheable);

	@Override
	NativeQuery<T> setCacheRegion(String cacheRegion);

	@Override
	NativeQuery<T> setTimeout(int timeout);

	@Override
	NativeQuery<T> setFetchSize(int fetchSize);

	@Override
	NativeQuery<T> setReadOnly(boolean readOnly);

	@Override
	NativeQuery<T> setLockOptions(LockOptions lockOptions);

	@Override
	NativeQuery<T> setLockMode(String alias, LockMode lockMode);

	@Override
	NativeQuery<T> setComment(String comment);

	@Override
	NativeQuery<T> addQueryHint(String hint);

	@Override
	NativeQuery<T> setMaxResults(int maxResult);

	@Override
	NativeQuery<T> setFirstResult(int startPosition);

	@Override
	NativeQuery<T> setHint(String hintName, Object value);

	@Override
	NativeQuery<T> setLockMode(LockModeType lockMode);

	@Override
	NativeQuery<T> setTupleTransformer(TupleTransformer<T> transformer);

	@Override
	NativeQuery<T> setResultListTransformer(ResultListTransformer transformer);

	@Override @SuppressWarnings("deprecation")
	NativeQuery<T> setResultTransformer(ResultTransformer transformer);

	@Override
	NativeQuery<T> setParameter(String name, Object value);

	@Override
	<P> NativeQuery<T> setParameter(String name, P val, Class<P> type);

	@Override
	<P> NativeQuery<T> setParameter(String name, P val, BindableType<P> type);

	@Override
	NativeQuery<T> setParameter(String name, Instant value, TemporalType temporalType);

	@Override
	NativeQuery<T> setParameter(String name, Calendar value, TemporalType temporalType);

	@Override
	NativeQuery<T> setParameter(String name, Date value, TemporalType temporalType);

	@Override
	NativeQuery<T> setParameter(int position, Object value);

	@Override
	<P> NativeQuery<T> setParameter(int position, P val, Class<P> type);

	@Override
	<P> NativeQuery<T> setParameter(int position, P val, BindableType<P> type);

	@Override
	NativeQuery<T> setParameter(int position, Instant value, TemporalType temporalType);

	@Override
	NativeQuery<T> setParameter(int position, Calendar value, TemporalType temporalType);

	@Override
	NativeQuery<T> setParameter(int position, Date value, TemporalType temporalType);

	@Override
	<P> NativeQuery<T> setParameter(QueryParameter<P> parameter, P val);

	@Override
	<P> NativeQuery<T> setParameter(QueryParameter<P> parameter, P val, Class<P> type);

	@Override
	<P> NativeQuery<T> setParameter(QueryParameter<P> parameter, P val, BindableType<P> type);

	@Override
	<P> NativeQuery<T> setParameter(Parameter<P> param, P value);

	@Override
	NativeQuery<T> setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType);

	@Override
	NativeQuery<T> setParameter(Parameter<Date> param, Date value, TemporalType temporalType);

	@Override
	NativeQuery<T> setParameterList(String name, @SuppressWarnings("rawtypes") Collection values);

	@Override
	<P> NativeQuery<T> setParameterList(String name, Collection<? extends P> values, Class<P> type);

	@Override
	<P> NativeQuery<T> setParameterList(String name, Collection<? extends P> values, BindableType<P> type);

	@Override
	NativeQuery<T> setParameterList(String name, Object[] values);

	@Override
	<P> NativeQuery<T> setParameterList(String name, P[] values, Class<P> type);

	@Override
	<P> NativeQuery<T> setParameterList(String name, P[] values, BindableType<P> type);

	@Override
	NativeQuery<T> setParameterList(int position, @SuppressWarnings("rawtypes") Collection values);

	@Override
	<P> NativeQuery<T> setParameterList(int position, Collection<? extends P> values, Class<P> type);

	@Override
	<P> NativeQuery<T> setParameterList(int position, Collection<? extends P> values, BindableType<P> javaType);

	@Override
	NativeQuery<T> setParameterList(int position, Object[] values);

	@Override
	<P> NativeQuery<T> setParameterList(int position, P[] values, Class<P> javaType);

	@Override
	<P> NativeQuery<T> setParameterList(int position, P[] values, BindableType<P> javaType);

	@Override
	<P> NativeQuery<T> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values);

	@Override
	<P> NativeQuery<T> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Class<P> javaType);

	@Override
	<P> NativeQuery<T> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, BindableType<P> type);

	@Override
	<P> NativeQuery<T> setParameterList(QueryParameter<P> parameter, P[] values);

	@Override
	<P> NativeQuery<T> setParameterList(QueryParameter<P> parameter, P[] values, Class<P> javaType);

	@Override
	<P> NativeQuery<T> setParameterList(QueryParameter<P> parameter, P[] values, BindableType<P> type);

	@Override
	NativeQuery<T> setProperties(Object bean);

	@Override
	NativeQuery<T> setProperties(@SuppressWarnings("rawtypes") Map bean);
}
