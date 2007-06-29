//$Id: SQLQuery.java 10845 2006-11-18 04:20:30Z steve.ebersole@jboss.com $
package org.hibernate;

import org.hibernate.type.Type;

/**
 * Allows the user to declare the types and select list injection
 * points of all entities returned by the query. Also allows
 * declaration of the type and column alias of any scalar results
 * of the query.
 * 
 * @author Gavin King
 */
public interface SQLQuery extends Query {
	/**
	 * Declare a "root" entity, without specifying an alias
	 */
	public SQLQuery addEntity(String entityName);
	/**
	 * Declare a "root" entity
	 */
	public SQLQuery addEntity(String alias, String entityName);
	/**
	 * Declare a "root" entity, specifying a lock mode
	 */
	public SQLQuery addEntity(String alias, String entityName, LockMode lockMode);
	/**
	 * Declare a "root" entity, without specifying an alias
	 */
	public SQLQuery addEntity(Class entityClass);
	/**
	 * Declare a "root" entity
	 */
	public SQLQuery addEntity(String alias, Class entityClass);
	/**
	 * Declare a "root" entity, specifying a lock mode
	 */
	public SQLQuery addEntity(String alias, Class entityClass, LockMode lockMode);

	/**
	 * Declare a "joined" entity
	 */
	public SQLQuery addJoin(String alias, String path);
	/**
	 * Declare a "joined" entity, specifying a lock mode
	 */
	public SQLQuery addJoin(String alias, String path, LockMode lockMode);
	
	/**
	 * Declare a scalar query result
	 */
	public SQLQuery addScalar(String columnAlias, Type type);

	/**
	 * Declare a scalar query. Hibernate will attempt to automatically detect the underlying type.
	 */
	public SQLQuery addScalar(String columnAlias);

	/**
	 * Use a predefined named ResultSetMapping
	 */
	public SQLQuery setResultSetMapping(String name);

	/**
	 * Adds a query space for auto-flush synchronization.
	 *
	 * @param querySpace The query space to be auto-flushed for this query.
	 * @return this, for method chaning
	 */
	public SQLQuery addSynchronizedQuerySpace(String querySpace);

	/**
	 * Adds an entity name or auto-flush synchronization.
	 *
	 * @param entityName The name of the entity upon whose defined
	 * query spaces we should additionally synchronize.
	 * @return this, for method chaning
	 * @throws MappingException Indicates the given entity name could not be
	 * resolved.
	 */
	public SQLQuery addSynchronizedEntityName(String entityName) throws MappingException;

	/**
	 * Adds an entity name or auto-flush synchronization.
	 *
	 * @param entityClass The class of the entity upon whose defined
	 * query spaces we should additionally synchronize.
	 * @return this, for method chaning
	 * @throws MappingException Indicates the given entity class could not be
	 * resolved.
	 */
	public SQLQuery addSynchronizedEntityClass(Class entityClass) throws MappingException;
}
