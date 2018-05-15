/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.internal;

import java.io.Serializable;
import java.util.Objects;

import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Allows multiple entity classes / collection roles to be stored in the same cache region. Also allows for composite
 * keys which do not properly implement equals()/hashCode().
 *
 * This was named org.hibernate.cache.spi.CacheKey in Hibernate until version 5.
 * Temporarily maintained as a reference while all components catch up with the refactoring to the caching interfaces.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
final class CacheKeyImplementation implements Serializable {
	private final Object id;
	private final JavaTypeDescriptor idJavaTypeDescriptor;
	private final NavigableRole navigableRole;
	private final String tenantId;
	private final int hashCode;

	/**
	 * Construct a new key for a collection or entity instance.
	 * Note that an entity name should always be the root entity
	 * name, not a subclass entity name.
	 *
	 * @param id The identifier associated with the cached data
	 * @param idJavaTypeDescriptor The JavaTypeDescriptor type mapping
	 * @param entityOrRoleName The entity or collection-role name.
	 * @param tenantId The tenant identifier associated this data.
	 */
	CacheKeyImplementation(
			final Object id,
			final JavaTypeDescriptor idJavaTypeDescriptor,
			final NavigableRole entityOrRoleName,
			final String tenantId) {
		this.id = id;
		this.idJavaTypeDescriptor = idJavaTypeDescriptor;
		this.navigableRole = entityOrRoleName;
		this.tenantId = tenantId;
		this.hashCode = calculateHashCode( idJavaTypeDescriptor, id, tenantId );
	}

	private static int calculateHashCode(JavaTypeDescriptor typeDescriptor, Object value, String tenantId) {
		int result = typeDescriptor.extractHashCode( value );
		result = 31 * result + ( tenantId != null ? tenantId.hashCode() : 0 );
		return result;
	}

	public Object getId() {
		return id;
	}

	@Override
	public boolean equals(Object other) {
		if ( other == null ) {
			return false;
		}
		if ( this == other ) {
			return true;
		}
		if ( hashCode != other.hashCode() || !( other instanceof CacheKeyImplementation) ) {
			//hashCode is part of this check since it is pre-calculated and hash must match for equals to be true
			return false;
		}
		final CacheKeyImplementation that = (CacheKeyImplementation) other;
		return Objects.equals( navigableRole, that.navigableRole )
				&& idJavaTypeDescriptor.areEqual( id, that.id )
				&& Objects.equals( tenantId, that.tenantId );
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public String toString() {
		// Used to be required for OSCache
		return navigableRole.getFullPath() + '#' + id.toString();
	}
}
