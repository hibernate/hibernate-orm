/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.cache.spi;

import java.io.Serializable;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.compare.EqualsHelper;
import org.hibernate.type.Type;

/**
 * Allows multiple entity classes / collection roles to be stored in the same cache region. Also allows for composite
 * keys which do not properly implement equals()/hashCode().
 *
 * Note on performance: this class is allocated very heavily!
 * Make sure the allocation cost stays as low as possible:
 * if you need to add fields for not-so-common use cases,
 * it's probably better to create an ad-hoc implementation
 * extending this one.
 *
 * @author Gavin King
 * @author Steve Ebersole
 * @author Sanne Grinovero
 */
public class CacheKey implements Serializable {

	private final Serializable key;
	private final Type type;
	private final int hashCode;

	/**
	 * Construct a new key for a collection or entity instance.
	 * Note that an entity name should always be the root entity
	 * name, not a subclass entity name.
	 *
	 * @param id The identifier associated with the cached data
	 * @param type The Hibernate type mapping
	 * @param entityOrRoleName The entity or collection-role name.
	 * @param tenantId The tenant identifier associated this data.
	 * @param factory The session factory for which we are caching
	 */
	public CacheKey(
			final Serializable id,
			final Type type,
			final String entityOrRoleName,
			final SessionFactoryImplementor factory) {
		this( id, type, entityOrRoleName, factory, null );
	}

	/**
	 * Used by subclasses: need to specify a tenantId
	 */
	protected CacheKey(
			final Serializable id,
			final Type type,
			final String entityOrRoleName,
			final SessionFactoryImplementor factory,
			final String tenantId) {
		this.key = id;
		this.type = type;
		this.hashCode = calculateHashCode( type, factory, tenantId );
	}

	private int calculateHashCode(Type type, SessionFactoryImplementor factory, String tenantId) {
		int result = type.getHashCode( key, factory );
		result = 31 * result + (tenantId != null ? tenantId.hashCode() : 0);
		return result;
	}

	public Serializable getKey() {
		return key;
	}

	public String getTenantId() {
		return null;
	}

	@Override
	public boolean equals(Object other) {
		if ( other == null ) {
			return false;
		}
		if ( this == other ) {
			return true;
		}
		if ( hashCode != other.hashCode() || !( other instanceof CacheKey ) ) {
			//hashCode is part of this check since it is pre-calculated and hash must match for equals to be true
			return false;
		}
		//Warning: this equals implementation needs to work correctly also for the subclass
		final CacheKey that = (CacheKey) other;
		return EqualsHelper.equals( getTenantId(), that.getTenantId() )
				&& type.isEqual( key, that.key );
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

}
