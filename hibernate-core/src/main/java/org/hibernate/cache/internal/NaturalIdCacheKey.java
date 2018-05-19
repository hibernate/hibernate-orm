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
import java.util.Arrays;
import java.util.Objects;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.ValueHolder;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

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
	private final Serializable[] naturalIdValues;
	private final String entityName;
	private final String tenantId;
	private final int hashCode;
	// "transient" is important here -- NaturalIdCacheKey needs to be Serializable
	private transient ValueHolder<String> toString;

	/**
	 * Construct a new key for a caching natural identifier resolutions into the second level cache.
	 * @param naturalIdValues The naturalIdValues associated with the cached data
	 * @param propertyTypes
	 * @param naturalIdPropertyIndexes
	 * @param session The originating session
	 */
	public NaturalIdCacheKey(
			final Object[] naturalIdValues,
			Type[] propertyTypes, int[] naturalIdPropertyIndexes, final String entityName,
			final SharedSessionContractImplementor session) {

		this.entityName = entityName;
		this.tenantId = session.getTenantIdentifier();

		this.naturalIdValues = new Serializable[naturalIdValues.length];

		final SessionFactoryImplementor factory = session.getFactory();

		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( this.entityName == null ) ? 0 : this.entityName.hashCode() );
		result = prime * result + ( ( this.tenantId == null ) ? 0 : this.tenantId.hashCode() );
		for ( int i = 0; i < naturalIdValues.length; i++ ) {
			final int naturalIdPropertyIndex = naturalIdPropertyIndexes[i];
			final Type type = propertyTypes[naturalIdPropertyIndex];
			final Object value = naturalIdValues[i];

			result = prime * result + (value != null ? type.getHashCode( value, factory ) : 0);

			// The natural id may not be fully resolved in some situations.  See HHH-7513 for one of them
			// (re-attaching a mutable natural id uses a database snapshot and hydration does not resolve associations).
			// TODO: The snapshot should probably be revisited at some point.  Consider semi-resolving, hydrating, etc.
			if (type instanceof EntityType && type.getSemiResolvedType( factory ).getReturnedClass().isInstance( value )) {
				this.naturalIdValues[i] = (Serializable) value;
			}
			else {
				this.naturalIdValues[i] = type.disassemble( value, session, null );
			}
		}

		this.hashCode = result;
		initTransients();
	}

	private void initTransients() {
		this.toString = new ValueHolder<>(
				new ValueHolder.DeferredInitializer<String>() {
					@Override
					public String initialize() {
						//Complex toString is needed as naturalIds for entities are not simply based on a single value like primary keys
						//the only same way to differentiate the keys is to included the disassembled values in the string.
						final StringBuilder toStringBuilder = new StringBuilder().append( entityName ).append(
								"##NaturalId[" );
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

		if ( hashCode != o.hashCode() || !( o instanceof NaturalIdCacheKey) ) {
			//hashCode is part of this check since it is pre-calculated and hash must match for equals to be true
			return false;
		}

		final NaturalIdCacheKey other = (NaturalIdCacheKey) o;
		return Objects.equals( entityName, other.entityName )
				&& Objects.equals( tenantId, other.tenantId )
				&& Arrays.deepEquals( this.naturalIdValues, other.naturalIdValues );
	}

	private void readObject(ObjectInputStream ois)
			throws ClassNotFoundException, IOException {
		ois.defaultReadObject();
		initTransients();
	}
}
