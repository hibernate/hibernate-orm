/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate;
import org.hibernate.type.Type;

/**
 * Represents a "native sql" query and allows the user to define certain aspects about its execution, such as:<ul>
 * <li>result-set value mapping (see below)</li>
 * <li>
 * 	Tables used via {@link #addSynchronizedQuerySpace}, {@link #addSynchronizedEntityName} and
 *  {@link #addSynchronizedEntityClass}.  This allows Hibernate to properly know how to deal with auto-flush checking
 *  as well as cached query results if the results of the query are being cached.
 * </li>
 * </ul>
 * <p/>
 * In terms of result-set mapping, there are 3 approaches to defining:<ul>
 * <li>If this represents a named sql query, the mapping could be associated with the query as part of its metadata</li>
 * <li>A pre-defined (defined in metadata and named) mapping can be associated with {@link #setResultSetMapping}</li>
 * <li>Defined locally per the various {@link #addEntity}, {@link #addRoot}, {@link #addJoin}, {@link #addFetch} and {@link #addScalar} methods</li>
 *
 * </ul>
 * 
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface SQLQuery extends Query {

	/**
	 * Adds a query space (table name) for (a) auto-flush checking and (b) query result cache invalidation checking
	 *
	 * @param querySpace The query space to be auto-flushed for this query.
	 *
	 * @return this, for method chaining
	 */
	public SQLQuery addSynchronizedQuerySpace(String querySpace);

	/**
	 * Adds an entity name for (a) auto-flush checking and (b) query result cache invalidation checking.  Same as
	 * {@link #addSynchronizedQuerySpace} for all tables associated with the given entity.
	 *
	 * @param entityName The name of the entity upon whose defined query spaces we should additionally synchronize.
	 *
	 * @return this, for method chaining
	 *
	 * @throws MappingException Indicates the given name could not be resolved as an entity
	 */
	public SQLQuery addSynchronizedEntityName(String entityName) throws MappingException;

	/**
	 * Adds an entity for (a) auto-flush checking and (b) query result cache invalidation checking.  Same as
	 * {@link #addSynchronizedQuerySpace} for all tables associated with the given entity.
	 *
	 * @param entityClass The class of the entity upon whose defined query spaces we should additionally synchronize.
	 *
	 * @return this, for method chaining
	 *
	 * @throws MappingException Indicates the given class could not be resolved as an entity
	 */
	public SQLQuery addSynchronizedEntityClass(Class entityClass) throws MappingException;

	/**
	 * Use a predefined named result-set mapping.  This might be defined by a {@code <result-set/>} element in a
	 * Hibernate <tt>hbm.xml</tt> file or through a {@link javax.persistence.SqlResultSetMapping} annotation.
	 *
	 * @param name The name of the mapping to use.
	 *
	 * @return this, for method chaining
	 */
	public SQLQuery setResultSetMapping(String name);

	/**
	 * Declare a scalar query result. Hibernate will attempt to automatically detect the underlying type.
	 * <p/>
	 * Functions like {@code <return-scalar/>} in {@code hbm.xml} or {@link javax.persistence.ColumnResult}
	 *
	 * @param columnAlias The column alias in the result-set to be processed as a scalar result
	 *
	 * @return {@code this}, for method chaining
	 */
	public SQLQuery addScalar(String columnAlias);

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
	public SQLQuery addScalar(String columnAlias, Type type);

	/**
	 * Add a new root return mapping, returning a {@link RootReturn} to allow further definition
	 *
	 * @param tableAlias The SQL table alias to map to this entity
	 * @param entityName The name of the entity.
	 *
	 * @return The return config object for further control.
	 *
	 * @since 3.6
	 */
	public RootReturn addRoot(String tableAlias, String entityName);

	/**
	 * Add a new root return mapping, returning a {@link RootReturn} to allow further definition
	 *
	 * @param tableAlias The SQL table alias to map to this entity
	 * @param entityType The java type of the entity.
	 *
	 * @return The return config object for further control.
	 *
	 * @since 3.6
	 */
	public RootReturn addRoot(String tableAlias, Class entityType);

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
	public SQLQuery addEntity(String entityName);

	/**
	 * Declare a "root" entity
	 *
	 * @param tableAlias The SQL table alias
	 * @param entityName The entity name
	 *
	 * @return {@code this}, for method chaining
	 */
	public SQLQuery addEntity(String tableAlias, String entityName);

	/**
	 * Declare a "root" entity, specifying a lock mode
	 *
	 * @param tableAlias The SQL table alias
	 * @param entityName The entity name
	 * @param lockMode The lock mode for this return.
	 *
	 * @return {@code this}, for method chaining
	 */
	public SQLQuery addEntity(String tableAlias, String entityName, LockMode lockMode);

	/**
	 * Declare a "root" entity, without specifying an alias.  The expectation here is that the table alias is the
	 * same as the unqualified entity name
	 *
	 * @param entityType The java type of the entity to add as a root
	 *
	 * @return {@code this}, for method chaining
	 */
	public SQLQuery addEntity(Class entityType);

	/**
	 * Declare a "root" entity
	 *
	 * @param tableAlias The SQL table alias
	 * @param entityType The java type of the entity to add as a root
	 *
	 * @return {@code this}, for method chaining
	 */
	public SQLQuery addEntity(String tableAlias, Class entityType);

	/**
	 * Declare a "root" entity, specifying a lock mode
	 *
	 * @param tableAlias The SQL table alias
	 * @param entityName The entity name
	 * @param lockMode The lock mode for this return.
	 *
	 * @return {@code this}, for method chaining
	 */
	public SQLQuery addEntity(String tableAlias, Class entityName, LockMode lockMode);

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
	public FetchReturn addFetch(String tableAlias, String ownerTableAlias, String joinPropertyName);

	/**
	 * Declare a join fetch result.
	 *
	 * @param tableAlias The SQL table alias for the data to be mapped to this fetch
	 * @param path The association path ([owner-alias].[property-name]).
	 *
	 * @return {@code this}, for method chaining
	 */
	public SQLQuery addJoin(String tableAlias, String path);

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
	public SQLQuery addJoin(String tableAlias, String ownerTableAlias, String joinPropertyName);

	/**
	 * Declare a join fetch result, specifying a lock mode
	 *
	 * @param tableAlias The SQL table alias for the data to be mapped to this fetch
	 * @param path The association path ([owner-alias].[property-name]).
	 * @param lockMode The lock mode for this return.
	 *
	 * @return {@code this}, for method chaining
	 */
	public SQLQuery addJoin(String tableAlias, String path, LockMode lockMode);

	/**
	 * Allows access to further control how properties within a root or join fetch are mapped back from the result set.
	 * Generally used in composite value scenarios.
	 */
	public static interface ReturnProperty {
		/**
		 * Add a column alias to this property mapping.
		 *
		 * @param columnAlias The column alias.
		 *
		 * @return {@code this}, for method chaining
		 */
		public ReturnProperty addColumnAlias(String columnAlias);
	}

	/**
	 * Allows access to further control how root returns are mapped back from result sets
	 */
	public static interface RootReturn {
		/**
		 * Set the lock mode for this return
		 *
		 * @param lockMode The new lock mode.
		 *
		 * @return {@code this}, for method chaining
		 */
		public RootReturn setLockMode(LockMode lockMode);

		/**
		 * Name the column alias that identifies the entity's discriminator
		 *
		 * @param columnAlias The discriminator column alias
		 *
		 * @return {@code this}, for method chaining
		 */
		public RootReturn setDiscriminatorAlias(String columnAlias);

		/**
		 * Add a simple property-to-one-column mapping
		 *
		 * @param propertyName The name of the property.
		 * @param columnAlias The name of the column
		 *
		 * @return {@code this}, for method chaining
		 */
		public RootReturn addProperty(String propertyName, String columnAlias);

		/**
		 * Add a property, presumably with more than one column.
		 *
		 * @param propertyName The name of the property.
		 *
		 * @return The config object for further control.
		 */
		public ReturnProperty addProperty(String propertyName);
	}

	/**
	 * Allows access to further control how join fetch returns are mapped back from result sets
	 */
	public static interface FetchReturn {
		/**
		 * Set the lock mode for this return
		 *
		 * @param lockMode The new lock mode.
		 *
		 * @return {@code this}, for method chaining
		 */
		public FetchReturn setLockMode(LockMode lockMode);

		/**
		 * Add a simple property-to-one-column mapping
		 *
		 * @param propertyName The name of the property.
		 * @param columnAlias The name of the column
		 *
		 * @return {@code this}, for method chaining
		 */
		public FetchReturn addProperty(String propertyName, String columnAlias);

		/**
		 * Add a property, presumably with more than one column.
		 *
		 * @param propertyName The name of the property.
		 *
		 * @return The config object for further control.
		 */
		public ReturnProperty addProperty(String propertyName);
	}
}
