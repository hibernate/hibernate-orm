/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.internal;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Objects;

import org.hibernate.Internal;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.ValueHolder;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Defines a key for caching natural identifier resolutions into the second level cache.
 *
 * This was named org.hibernate.cache.spi.NaturalIdCacheKey in Hibernate until version 5.
 * Temporarily maintained as a reference while all components catch up with the refactoring to the caching interfaces.
 *
 * @author Eric Dalquist
 * @author Steve Ebersole
 */
public class NaturalIdCacheKey implements Serializable {
	private final Object naturalIdValues;
	private final String entityName;
	private final String tenantId;
	private final int hashCode;
	// "transient" is important here -- NaturalIdCacheKey needs to be Serializable
	private transient ValueHolder<String> toString;

	public NaturalIdCacheKey(Object naturalIdValues, EntityPersister persister, SharedSessionContractImplementor session) {
		this( naturalIdValues, persister, persister.getRootEntityName(), session );
	}

	public NaturalIdCacheKey(Object naturalIdValues, EntityPersister persister, String entityName, SharedSessionContractImplementor session) {
		this( persister.getNaturalIdMapping().disassemble( naturalIdValues, session ), entityName, session.getTenantIdentifier(), persister.getNaturalIdMapping().calculateHashCode( naturalIdValues, session ) );
	}

	@Internal
	public NaturalIdCacheKey(Object naturalIdValues, String entityName, String tenantId, int hashCode) {
		this.naturalIdValues = naturalIdValues;
		this.entityName = entityName;
		this.tenantId = tenantId;
		this.hashCode = hashCode;

		initTransients();
	}

	private void initTransients() {
		this.toString = new ValueHolder<>(
				() -> {
					//Complex toString is needed as naturalIds for entities are not simply based on a single value like primary keys
					//the only same way to differentiate the keys is to include the disassembled values in the string.
					final StringBuilder toStringBuilder = new StringBuilder().append( entityName ).append( "##NaturalId[" );
					if ( naturalIdValues instanceof Object[] ) {
						final Object[] values = (Object[]) naturalIdValues;
						for ( int i = 0; i < values.length; i++ ) {
							toStringBuilder.append( values[ i ] );
							if ( i + 1 < values.length ) {
								toStringBuilder.append( ", " );
							}
						}
					}
					else {
						toStringBuilder.append( naturalIdValues );
					}

					return toStringBuilder.toString();
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
	public Object getNaturalIdValues() {
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

		if ( hashCode != o.hashCode() || !( o instanceof NaturalIdCacheKey) ) {
			//hashCode is part of this check since it is pre-calculated and hash must match for equals to be true
			return false;
		}

		final NaturalIdCacheKey other = (NaturalIdCacheKey) o;
		return Objects.equals( entityName, other.entityName )
				&& Objects.equals( tenantId, other.tenantId )
				&& Objects.deepEquals( this.naturalIdValues, other.naturalIdValues );
	}

	private void readObject(ObjectInputStream ois)
			throws ClassNotFoundException, IOException {
		ois.defaultReadObject();
		initTransients();
	}
}
