/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.internal;

import java.io.Serializable;
import java.util.Objects;

import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.NaturalIdMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.descriptor.java.JavaType;

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

	// The constructor needs to be public because it is used by WildFly NaturalIdCacheKeyMarshaller#readFrom(ProtoStreamReader)
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
		final NaturalIdMapping naturalIdMapping = persister.getNaturalIdMapping();
		final NaturalIdCacheKeyBuilder builder = new NaturalIdCacheKeyBuilder(
				entityName,
				session.getTenantIdentifier(),
				naturalIdMapping.getJdbcTypeCount()
		);
		final JavaType<Object> tenantIdentifierJavaType = session.getFactory().getTenantIdentifierJavaType();
		final Object tenantId = session.getTenantIdentifierValue();
		// Add the tenant id to the hash code
		builder.addHashCode( tenantId == null ? 0 : tenantIdentifierJavaType.extractHashCode( tenantId ) );
		naturalIdMapping.addToCacheKey( builder, naturalIdValues, session );
		return builder.build();
	}

	public static NaturalIdCacheKey from(
			Object naturalIdValues,
			EntityPersister persister,
			SharedSessionContractImplementor session) {
		return from( naturalIdValues, persister, persister.getRootEntityName(), session );
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

	@Override
	public String toString() {
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

	private static class NaturalIdCacheKeyBuilder implements MutableCacheKeyBuilder {

		private final String entityName;
		private final String tenantIdentifier;
		private final Object[] naturalIdValues;
		private int hashCode;
		private int naturalIdValueIndex;

		public NaturalIdCacheKeyBuilder(String entityName, String tenantIdentifier, int naturalIdValueCount) {
			this.entityName = entityName;
			this.tenantIdentifier = tenantIdentifier;
			this.naturalIdValues = new Object[naturalIdValueCount];
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
			return new NaturalIdCacheKey(
					naturalIdValues,
					entityName,
					tenantIdentifier,
					hashCode
			);
		}
	}
}
