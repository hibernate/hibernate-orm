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
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;
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
	private final Type[] naturalIdTypes;
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
	 * @param persister The persister for the entity
	 * @param session The session for which we are caching
	 */
	public NaturalIdCacheKey(
			final Object[] naturalId,
			final EntityPersister persister,
			final SessionImplementor session) {
		
		this.entityName = persister.getEntityName();
		this.tenantId = session.getTenantIdentifier();
		
		final Serializable[] disassembledNaturalId = new Serializable[naturalId.length];
		final Type[] naturalIdTypes = new Type[naturalId.length];
		final StringBuilder str = new StringBuilder(entityName).append( "##NaturalId[" );
		
		
		final SessionFactoryImplementor factory = session.getFactory();
		final int[] naturalIdPropertyIndexes = persister.getNaturalIdentifierProperties();
		final Type[] propertyTypes = persister.getPropertyTypes();

		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( this.entityName == null ) ? 0 : this.entityName.hashCode() );
		result = prime * result + ( ( this.tenantId == null ) ? 0 : this.tenantId.hashCode() );
		for ( int i = 0; i < naturalId.length; i++ ) {
			final Type type = propertyTypes[naturalIdPropertyIndexes[i]];
			final Object value = naturalId[i];
			
			result = prime * result + type.getHashCode( value, factory );
			
			disassembledNaturalId[i] = type.disassemble( value, session, null );
			
			naturalIdTypes[i] = type;
			
			str.append( type.toLoggableString( value, factory ) );
			if (i + 1 < naturalId.length) {
				str.append( ", " );
			}
		}
		str.append( "]" );
		
		this.naturalId = disassembledNaturalId;
		this.naturalIdTypes = naturalIdTypes;
		this.hashCode = result;
		this.toString = str.toString();
	}

	public String getEntityName() {
		return entityName;
	}
	
	public String getTenantId() {
		return tenantId;
	}
	
	public Serializable[] getNaturalId() {
		return naturalId;
	}

	@Override
	public String toString() {
		return this.toString;
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
		if ( entityName == null ) {
			if ( other.entityName != null )
				return false;
		}
		else if ( !entityName.equals( other.entityName ) )
			return false;
		if ( tenantId == null ) {
			if ( other.tenantId != null )
				return false;
		}
		else if ( !tenantId.equals( other.tenantId ) )
			return false;
		if ( naturalId == other.naturalId )
			return true;
		if ( naturalId == null || other.naturalId == null )
			return false;
		int length = naturalId.length;
		if ( other.naturalId.length != length )
			return false;
		for ( int i = 0; i < length; i++ ) {
			if ( !this.naturalIdTypes[i].isEqual( naturalId[i], other.naturalId[i] ) ) {
				return false;
			}
		}
		return true;
	}
}
