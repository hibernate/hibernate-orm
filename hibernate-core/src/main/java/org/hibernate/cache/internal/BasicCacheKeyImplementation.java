/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.internal;

import java.io.Serializable;

import org.hibernate.Internal;
import org.hibernate.type.Type;

/**
 * Key produced by DefaultCacheKeysFactory; this is the specialized implementation
 * for the case in which the disassembled identifier is not an array and the tenantId
 * is not being defined.
 * The goal of this specialized representation is to be more efficient for this most common
 * scenario, and save memory by omitting some fields.
 * When making changes to this class, please be aware of its memory footprint.
 *
 * @author Sanne Grinovero
 * @since 6.2
 */
@Internal
public final class BasicCacheKeyImplementation implements Serializable {

	final Serializable id;
	private final String entityOrRoleName;
	private final int hashCode;

	/**
	 * Being an internal contract the arguments are not being checked.
	 */
	@Internal
	public BasicCacheKeyImplementation(
			final Object originalId,
			final Serializable disassembledKey,
			final Type type,
			final String entityOrRoleName) {
		this( disassembledKey, entityOrRoleName, calculateHashCode( originalId, type ) );
	}

	/**
	 * Being an internal contract the arguments are not being checked.
	 */
	@Internal
	public BasicCacheKeyImplementation(
			final Serializable id,
			final String entityOrRoleName,
			final int hashCode) {
		assert id != null;
		assert entityOrRoleName != null;
		this.id = id;
		this.entityOrRoleName = entityOrRoleName;
		this.hashCode = hashCode;
	}

	private static int calculateHashCode(Object disassembledKey, Type type) {
		return type.getHashCode( disassembledKey );
	}

	public Object getId() {
		return id;
	}

	@Internal
	public String getEntityOrRoleName() {
		return entityOrRoleName;
	}

	@Override
	public boolean equals(final Object other) {
		if ( other == null ) {
			return false;
		}
		else if ( this == other ) {
			return true;
		}
		else if ( other.getClass() != BasicCacheKeyImplementation.class ) {
			return false;
		}
		else {
			final BasicCacheKeyImplementation o = (BasicCacheKeyImplementation) other;
			return this.id.equals( o.id ) &&
					this.entityOrRoleName.equals( o.entityOrRoleName );
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
