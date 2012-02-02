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
import java.util.Arrays;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.Type;

/**
 * Allows multiple entity classes / collection roles to be
 * stored in the same cache region. Also allows for composite
 * keys which do not properly implement equals()/hashCode().
 *
 * @author Gavin King
 */
public class NaturalIdCacheKey implements Serializable {
	private final Serializable[] naturalId;
	private final Type[] types;
	private final String entityName;
	private final String tenantId;
	private final int hashCode;
	private final String toString;

	/**
	 * Construct a new key for a collection or entity instance.
	 * Note that an entity name should always be the root entity
	 * name, not a subclass entity name.
	 *
	 * @param naturalId The naturalId associated with the cached data
	 * @param types The Hibernate type mappings
	 * @param entityOrRoleName The entity name.
	 * @param tenantId The tenant identifier associated this data.
	 * @param factory The session factory for which we are caching
	 */
	public NaturalIdCacheKey(
			final Serializable[] naturalId,
			final Type[] types,
			final String entityOrRoleName,
			final String tenantId,
			final SessionFactoryImplementor factory) {
		
		this.naturalId = naturalId;
		this.types = types;
		this.entityName = entityOrRoleName;
		this.tenantId = tenantId;
		
		this.hashCode = this.generateHashCode( this.naturalId, this.types, factory );
		this.toString = entityOrRoleName + "##NaturalId" + Arrays.toString( this.naturalId );
	}
	
	private int generateHashCode(final Serializable[] naturalId, final Type[] types,
			final SessionFactoryImplementor factory) {
		
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( entityName == null ) ? 0 : entityName.hashCode() );
		result = prime * result + ( ( tenantId == null ) ? 0 : tenantId.hashCode() );
		for ( int i = 0; i < naturalId.length; i++ ) {
			result = prime * result + types[i].getHashCode( naturalId[i], factory );
		}
		return result;
	}

	@Override
	public String toString() {
		// Mainly for OSCache
		return this.toString;
	}
	
	public Serializable[] getNaturalId() {
		return naturalId;
	}

	public Type[] getTypes() {
		return types;
	}

	@Override
	public int hashCode() {
		return this.hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj )
			return true;
		if ( obj == null )
			return false;
		if ( getClass() != obj.getClass() )
			return false;
		NaturalIdCacheKey other = (NaturalIdCacheKey) obj;
		if (this.hashCode != other.hashCode) {
			//Short circuit on hashCode, if they aren't equal there is no chance the keys are equal
			return false;
		}
		if ( entityName == null ) {
			if ( other.entityName != null )
				return false;
		}
		else if ( !entityName.equals( other.entityName ) )
			return false;
		if ( !Arrays.equals( naturalId, other.naturalId ) )
			return false;
		if ( tenantId == null ) {
			if ( other.tenantId != null )
				return false;
		}
		else if ( !tenantId.equals( other.tenantId ) )
			return false;
		
		for ( int i = 0; i < naturalId.length; i++ ) {
			if ( !types[i].isEqual( naturalId[i], other.naturalId[i] ) ) {
				return false;
			}
		}
		
		return true;
	}

	public String getEntityOrRoleName() {
		return entityName;
	}

}
