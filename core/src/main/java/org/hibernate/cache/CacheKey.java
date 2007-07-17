//$Id: CacheKey.java 11499 2007-05-09 17:35:55Z steve.ebersole@jboss.com $
package org.hibernate.cache;

import java.io.Serializable;

import org.hibernate.EntityMode;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.type.Type;

/**
 * Allows multiple entity classes / collection roles to be
 * stored in the same cache region. Also allows for composite
 * keys which do not properly implement equals()/hashCode().
 *
 * @author Gavin King
 */
public class CacheKey implements Serializable {
	private final Serializable key;
	private final Type type;
	private final String entityOrRoleName;
	private final EntityMode entityMode;
	private final int hashCode;

	/**
	 * Construct a new key for a collection or entity instance.
	 * Note that an entity name should always be the root entity
	 * name, not a subclass entity name.
	 *
	 * @param id The identifier associated with the cached data
	 * @param type The Hibernate type mapping
	 * @param entityOrRoleName The entity or collection-role name.
	 * @param entityMode The entiyt mode of the originating session
	 * @param factory The session factory for which we are caching
	 */
	public CacheKey(
			final Serializable id,
			final Type type,
			final String entityOrRoleName,
			final EntityMode entityMode,
			final SessionFactoryImplementor factory) {
		this.key = id;
		this.type = type;
		this.entityOrRoleName = entityOrRoleName;
		this.entityMode = entityMode;
		hashCode = type.getHashCode( key, entityMode, factory );
	}

	//Mainly for OSCache
	public String toString() {
		return entityOrRoleName + '#' + key.toString();//"CacheKey#" + type.toString(key, sf);
	}

	public boolean equals(Object other) {
		if ( !(other instanceof CacheKey) ) return false;
		CacheKey that = (CacheKey) other;
		return entityOrRoleName.equals( that.entityOrRoleName )
				&& type.isEqual( key, that.key, entityMode );
	}

	public int hashCode() {
		return hashCode;
	}

	public Serializable getKey() {
		return key;
	}

	public String getEntityOrRoleName() {
		return entityOrRoleName;
	}

}
