/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.util.compare.EqualsHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;

/**
 * Defines a key for caching natural identifier resolutions into the second level cache.
 *
 * @author Eric Dalquist
 * @author Steve Ebersole
 */
public class NaturalIdCacheKey implements Serializable {
	private final Serializable[] naturalIdValues;
	private final String entityName;
	private final String tenantId;
	private final int hashCode;
	private final String toString;

	/**
	 * Construct a new key for a caching natural identifier resolutions into the second level cache.
	 * Note that an entity name should always be the root entity name, not a subclass entity name.
	 *
	 * @param naturalIdValues The naturalIdValues associated with the cached data
	 * @param persister The persister for the entity
	 * @param session The originating session
	 */
	public NaturalIdCacheKey(
			final Object[] naturalIdValues,
			final EntityPersister persister,
			final SessionImplementor session) {

		this.entityName = persister.getRootEntityName();
		this.tenantId = session.getTenantIdentifier();

		final Serializable[] disassembledNaturalId = new Serializable[naturalIdValues.length];
		final StringBuilder toStringBuilder = new StringBuilder( entityName ).append( "##NaturalId[" );

		final SessionFactoryImplementor factory = session.getFactory();
		final int[] naturalIdPropertyIndexes = persister.getNaturalIdentifierProperties();
		final Type[] propertyTypes = persister.getPropertyTypes();

		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( this.entityName == null ) ? 0 : this.entityName.hashCode() );
		result = prime * result + ( ( this.tenantId == null ) ? 0 : this.tenantId.hashCode() );
		for ( int i = 0; i < naturalIdValues.length; i++ ) {
			final Type type = propertyTypes[naturalIdPropertyIndexes[i]];
			final Object value = naturalIdValues[i];
			
			result = prime * result + (value != null ? type.getHashCode( value, factory ) : 0);
			
			disassembledNaturalId[i] = type.disassemble( value, session, null );
			
			toStringBuilder.append( type.toLoggableString( value, factory ) );
			if (i + 1 < naturalIdValues.length) {
				toStringBuilder.append( ", " );
			}
		}
		toStringBuilder.append( "]" );
		
		this.naturalIdValues = disassembledNaturalId;
		this.hashCode = result;
		this.toString = toStringBuilder.toString();
	}

	@SuppressWarnings( {"UnusedDeclaration"})
	public String getEntityName() {
		return entityName;
	}

	@SuppressWarnings( {"UnusedDeclaration"})
	public String getTenantId() {
		return tenantId;
	}

	@SuppressWarnings( {"UnusedDeclaration"})
	public Serializable[] getNaturalIdValues() {
		return naturalIdValues;
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
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final NaturalIdCacheKey other = (NaturalIdCacheKey) o;
		return entityName.equals( other.entityName )
				&& EqualsHelper.equals( tenantId, other.tenantId )
				&& Arrays.equals( naturalIdValues, other.naturalIdValues );

	}
}
