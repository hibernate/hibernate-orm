/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.internal;

import java.io.Serializable;
import java.util.Objects;

import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
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
	private final int hashCode; // not a record because we cache this

	// The constructor needs to be public because it is used by WildFly
	// NaturalIdCacheKeyMarshaller#readFrom(ProtoStreamReader)
	public NaturalIdCacheKey(Object naturalIdValues, String entityName, String tenantId, int hashCode) {
		this.naturalIdValues = naturalIdValues;
		this.entityName = entityName;
		this.tenantId = tenantId;
		this.hashCode = hashCode;
	}

	public static NaturalIdCacheKey from(
			Object naturalIdValues,
			EntityPersister persister,
			String entityName,
			SharedSessionContractImplementor session) {
		final NaturalIdCacheKeyBuilder builder =
				new NaturalIdCacheKeyBuilder( entityName, session.getTenantIdentifier(), persister );
		addTenantIdToCacheKey( session, builder );
		persister.getNaturalIdMapping().addToCacheKey( builder, naturalIdValues, session );
		return builder.build();
	}

	private static void addTenantIdToCacheKey(SharedSessionContractImplementor session, NaturalIdCacheKeyBuilder builder) {
		final Object tenantId = session.getTenantIdentifierValue();
		// Add the tenant id to the hash code
		builder.addHashCode( tenantId == null ? 0
				: session.getFactory().getTenantIdentifierJavaType().extractHashCode( tenantId ) );
	}

	public static NaturalIdCacheKey from(
			Object naturalIdValues,
			EntityPersister persister,
			SharedSessionContractImplementor session) {
		return from( naturalIdValues, persister, persister.getRootEntityName(), session );
	}

	@SuppressWarnings("unused")
	public String getEntityName() {
		return entityName;
	}

	@SuppressWarnings("unused")
	public String getTenantId() {
		return tenantId;
	}

	@SuppressWarnings("unused")
	public Object getNaturalIdValues() {
		return naturalIdValues;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object o) {
		if ( o == null ) {
			return false;
		}
		if ( this == o ) {
			return true;
		}
		if ( hashCode != o.hashCode() || !(o instanceof NaturalIdCacheKey other) ) {
			//hashCode is part of this check since it is pre-calculated and hash must match for equals to be true
			return false;
		}
		return Objects.equals( entityName, other.entityName )
			&& Objects.equals( tenantId, other.tenantId )
			&& Objects.deepEquals( this.naturalIdValues, other.naturalIdValues );
	}

	@Override
	public String toString() {
		// Complex toString() is needed as natural ids for entities
		// are not simply based on a single value like primary keys.
		// The only sane way to differentiate the keys is to include
		// the disassembled values in the string.
		final StringBuilder string =
				new StringBuilder().append( entityName )
						.append( "##NaturalId[" );
		if ( naturalIdValues instanceof Object[] values ) {
			for ( int i = 0; i < values.length; i++ ) {
				string.append( values[ i ] );
				if ( i + 1 < values.length ) {
					string.append( ", " );
				}
			}
		}
		else {
			string.append( naturalIdValues );
		}
		return string.toString();
	}

	private static class NaturalIdCacheKeyBuilder implements MutableCacheKeyBuilder {

		private final String entityName;
		private final String tenantIdentifier;
		private final Object[] naturalIdValues;
		private int hashCode;
		private int naturalIdValueIndex;

		public NaturalIdCacheKeyBuilder(String entityName, String tenantIdentifier, EntityPersister persister) {
			this.entityName = entityName;
			this.tenantIdentifier = tenantIdentifier;
			this.naturalIdValues = new Object[ persister.getNaturalIdMapping().getJdbcTypeCount() ];
		}

		@Override
		public void addValue(Object value) {
			naturalIdValues[naturalIdValueIndex++] = value;
		}

		@Override
		public void addHashCode(int hashCode) {
			this.hashCode = 37 * this.hashCode + hashCode;
		}

		@Override
		public NaturalIdCacheKey build() {
			return new NaturalIdCacheKey( naturalIdValues, entityName, tenantIdentifier, hashCode );
		}
	}
}
