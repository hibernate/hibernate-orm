/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.internal;

import java.io.Serializable;
import java.util.Objects;

import org.hibernate.Internal;
import org.hibernate.type.Type;

/**
 * Allows multiple entity classes / collection roles to be stored in the same cache region. Also allows for composite
 * keys which do not properly implement equals()/hashCode().
 *
 * This was named org.hibernate.cache.spi.CacheKey in Hibernate until version 5.
 * Temporarily maintained as a reference while all components catch up with the refactoring to the caching interfaces.
 *
 * @author Gavin King
 * @author Steve Ebersole
 * @author Sanne Grinovero
 */
@Internal
public final class CacheKeyImplementation implements Serializable {
	private final Object id;
	private final String entityOrRoleName;
	private final String tenantId;
	private final int hashCode;

	//because of object alignment, we had "free space" in this key:
	//this field isn't strictly necessary but convenient: watch for
	//class layout changes.
	private final boolean requiresDeepEquals;

	/**
	 * Construct a new key for a collection or entity instance.
	 * Note that an entity name should always be the root entity
	 * name, not a subclass entity name.
	 *
	 * @param id The identifier associated with the cached data
	 * @param disassembledKey
	 * @param type The Hibernate type mapping
	 * @param entityOrRoleName The entity or collection-role name.
	 * @param tenantId The tenant identifier associated with this data.
	 */
	@Internal
	public CacheKeyImplementation(
			final Object id,
			final Serializable disassembledKey,
			final Type type,
			final String entityOrRoleName,
			final String tenantId) {
		this( disassembledKey, entityOrRoleName, tenantId, calculateHashCode( id, type, tenantId ) );
	}

	/**
	 * Construct a new key for a collection or entity instance.
	 * Note that an entity name should always be the root entity
	 * name, not a subclass entity name.
	 *
	 * @param id The identifier associated with the cached data
	 * @param entityOrRoleName The entity or collection-role name.
	 * @param tenantId The tenant identifier associated with this data.
	 * @param hashCode the pre-calculated hash code
	 */
	@Internal
	public CacheKeyImplementation(
			final Object id,
			final String entityOrRoleName,
			final String tenantId,
			final int hashCode) {
		assert entityOrRoleName != null;
		this.id = id;
		this.entityOrRoleName = entityOrRoleName;
		this.tenantId = tenantId; //might actually be null
		this.hashCode = hashCode;
		this.requiresDeepEquals = id.getClass().isArray();
	}

	private static int calculateHashCode(Object id, Type type, String tenantId) {
		int result = type.getHashCode( id );
		result = 31 * result + ( tenantId != null ? tenantId.hashCode() : 0 );
		return result;
	}

	public Object getId() {
		return id;
	}

	@Internal
	public String getEntityOrRoleName() {
		return entityOrRoleName;
	}

	@Internal
	public String getTenantId() {
		return tenantId;
	}

	@Override
	public boolean equals(Object other) {
		if ( other == null ) {
			return false;
		}
		else if ( this == other ) {
			return true;
		}
		else if ( other.getClass() != CacheKeyImplementation.class ) {
			return false;
		}
		else {
			CacheKeyImplementation o = (CacheKeyImplementation) other;
			//check this first, so we can short-cut following checks in a different order
			if ( requiresDeepEquals ) {
				//only in this case, leverage the hashcode comparison check first;
				//this is typically unnecessary, still far cheaper than the other checks we need to perform
				//so it should be worth it.
				return this.hashCode == o.hashCode &&
						entityOrRoleName.equals( o.entityOrRoleName ) &&
						Objects.equals( this.tenantId, o.tenantId ) &&
						Objects.deepEquals( this.id, o.id );
			}
			else {
				return this.id.equals( o.id ) &&
						entityOrRoleName.equals( o.entityOrRoleName ) &&
						( this.tenantId != null ? this.tenantId.equals( o.tenantId ) : o.tenantId == null );
			}
		}
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public String toString() {
		// Used to be required for OSCache
		return entityOrRoleName + '#' + id.toString();
	}
}
