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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Arrays;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.util.ValueHolder;
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
	private transient ValueHolder<String> toString;

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

		this.naturalIdValues = new Serializable[naturalIdValues.length];

		final SessionFactoryImplementor factory = session.getFactory();
		final int[] naturalIdPropertyIndexes = persister.getNaturalIdentifierProperties();
		final Type[] propertyTypes = persister.getPropertyTypes();

		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( this.entityName == null ) ? 0 : this.entityName.hashCode() );
		result = prime * result + ( ( this.tenantId == null ) ? 0 : this.tenantId.hashCode() );
		for ( int i = 0; i < naturalIdValues.length; i++ ) {
			final int naturalIdPropertyIndex = naturalIdPropertyIndexes[i];
            final Type type = propertyTypes[naturalIdPropertyIndex];
			final Object value = naturalIdValues[i];
			
			result = prime * result + (value != null ? type.getHashCode( value, factory ) : 0);
			
			this.naturalIdValues[i] = type.disassemble( value, session, null );
		}
		
		this.hashCode = result;
		initTransients();
	}
	
	private void initTransients() {
	    this.toString = new ValueHolder<String>(
                new ValueHolder.DeferredInitializer<String>() {
                    @Override
                    public String initialize() {
                        //Complex toString is needed as naturalIds for entities are not simply based on a single value like primary keys
                        //the only same way to differentiate the keys is to included the disassembled values in the string.
                        final StringBuilder toStringBuilder = new StringBuilder( entityName ).append( "##NaturalId[" );
                        for ( int i = 0; i < naturalIdValues.length; i++ ) {
                            toStringBuilder.append( naturalIdValues[i] );
                            if ( i + 1 < naturalIdValues.length ) {
                                toStringBuilder.append( ", " );
                            }
                        }
                        toStringBuilder.append( "]" );

                        return toStringBuilder.toString();
                    }
                }
        );
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
		return toString.getValue();
	}
	
	@Override
	public int hashCode() {
		return this.hashCode;
	}

	@Override
	public boolean equals(Object o) {
		if ( o == null ) {
			return false;
		}
		if ( this == o ) {
			return true;
		}

		if ( hashCode != o.hashCode() || !( o instanceof NaturalIdCacheKey ) ) {
			//hashCode is part of this check since it is pre-calculated and hash must match for equals to be true
			return false;
		}

		final NaturalIdCacheKey other = (NaturalIdCacheKey) o;
		return EqualsHelper.equals( entityName, other.entityName )
				&& EqualsHelper.equals( tenantId, other.tenantId )
				&& Arrays.deepEquals( this.naturalIdValues, other.naturalIdValues );
	}
	
    private void readObject(ObjectInputStream ois)
            throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        initTransients();
    }
}
