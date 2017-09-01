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

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.ValueHolder;
import org.hibernate.internal.util.compare.EqualsHelper;
import org.hibernate.metamodel.model.domain.spi.EntityHierarchy;
import org.hibernate.metamodel.model.domain.spi.PersistentAttribute;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.spi.EntityJavaDescriptor;

/**
 * Defines a key for caching natural identifier resolutions into the second level cache.
 *
 * This was named org.hibernate.cache.spi.NaturalIdCacheKey in Hibernate until version 5.
 * Temporarily maintained as a reference while all components catch up with the refactoring to the caching interfaces.
 *
 * @author Eric Dalquist
 * @author Steve Ebersole
 *
 * @deprecated Cache implementation should provide optimized key.
 */
@Deprecated
public class OldNaturalIdCacheKey implements Serializable {
	private final String entityName;
	private final String tenantId;

	private final Serializable[] naturalIdValues;

	private final int hashCode;

	// transient is important here -- NaturalIdCacheKey needs to be Serializable
	private transient ValueHolder<String> toString;

	public OldNaturalIdCacheKey(
			Object[] naturalIdValues,
			EntityHierarchy entityHierarchy,
			SharedSessionContractImplementor session) {
		if ( naturalIdValues.length != entityHierarchy.getNaturalIdDescriptor().getPersistentAttributes().size() ) {
			throw new HibernateException(
					String.format(
							"Number of natural-id values [%s] did not match the number of mapped natural-id attributes [%s]",
							naturalIdValues.length,
							entityHierarchy.getNaturalIdDescriptor().getPersistentAttributes().size()
					)
			);
		}

		this.entityName = entityHierarchy.getRootEntityType().getEntityName();
		this.tenantId = session.getTenantIdentifier();

		this.naturalIdValues = new Serializable[naturalIdValues.length];

		final int prime = 31;
		int hashCodeCalculation = 1;
		hashCodeCalculation = prime * hashCodeCalculation + ( ( this.entityName == null ) ? 0 : this.entityName.hashCode() );
		hashCodeCalculation = prime * hashCodeCalculation + ( ( this.tenantId == null ) ? 0 : this.tenantId.hashCode() );

		int i = -1;
		for ( PersistentAttribute naturalIdAttribute : entityHierarchy.getNaturalIdDescriptor().getPersistentAttributes() ) {
			i++;

			final JavaTypeDescriptor javaTypeDescriptor = naturalIdAttribute.getJavaTypeDescriptor();

			final Object value = naturalIdValues[i];
			hashCodeCalculation = prime * hashCodeCalculation + (value != null ? javaTypeDescriptor.extractHashCode( value ) : 0);

			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// todo (6.0) : not sure how to best deal with this "semi-resolved" aspect in 6.0...
//
//			// The natural id may not be fully resolved in some situations.  See HHH-7513 for one of them
//			// (re-attaching a mutable natural id uses a database snapshot and hydration does not resolve associations).
//			// TODO: The snapshot should probably be revisited at some point.  Consider semi-resolving, hydrating, etc.
//			//
//			if (javaTypeDescriptor instanceof EntityJavaDescriptor && javaTypeDescriptor.getSemiResolvedType( factory ).getReturnedClass().isInstance( value )) {
//				this.naturalIdValues[i] = (Serializable) value;
//			}
//			else {
//				this.naturalIdValues[i] = javaTypeDescriptor.getMutabilityPlan().disassemble( value );
//			}
			// for now...
			if ( EntityJavaDescriptor.class.isInstance( javaTypeDescriptor ) && !javaTypeDescriptor.getJavaType().isInstance( value ) ) {
				this.naturalIdValues[i] = (Serializable) value;
			}
			else {
				this.naturalIdValues[i] = javaTypeDescriptor.getMutabilityPlan().disassemble( value );
			}
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		}

		this.hashCode = hashCodeCalculation;
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

		if ( hashCode != o.hashCode() || !( o instanceof OldNaturalIdCacheKey ) ) {
			//hashCode is part of this check since it is pre-calculated and hash must match for equals to be true
			return false;
		}

		final OldNaturalIdCacheKey other = (OldNaturalIdCacheKey) o;
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
