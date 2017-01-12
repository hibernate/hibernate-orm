/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.internal.descriptor.java;

import java.util.Comparator;

import org.hibernate.internal.util.compare.ComparableComparator;
import org.hibernate.type.descriptor.java.spi.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.spi.MutabilityPlan;

/**
 * @author Steve Ebersole
 */
public class RegistryKeyBasicImpl {
	public static RegistryKeyBasicImpl from(Class javaType) {
		return new RegistryKeyBasicImpl(
				javaType,
				ImmutableMutabilityPlan.INSTANCE,
				Comparable.class.isAssignableFrom( javaType ) ? ComparableComparator.INSTANCE : null

		);
	}

	public static RegistryKeyBasicImpl from(Class javaType, MutabilityPlan mutabilityPlan) {
		return new RegistryKeyBasicImpl(
				javaType,
				mutabilityPlan,
				Comparable.class.isAssignableFrom( javaType ) ? ComparableComparator.INSTANCE : null

		);
	}

	public static RegistryKeyBasicImpl from(Class javaType, MutabilityPlan mutabilityPlan, Comparator comparator) {
		return new RegistryKeyBasicImpl( javaType, mutabilityPlan, comparator );
	}

	private final Class javaType;
	private final MutabilityPlan mutabilityPlan;
	private final Comparator comparator;

	public RegistryKeyBasicImpl(Class javaType, MutabilityPlan mutabilityPlan, Comparator comparator) {
		this.javaType = javaType;
		this.mutabilityPlan = mutabilityPlan;
		this.comparator = comparator;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !( o instanceof RegistryKeyBasicImpl ) ) {
			return false;
		}

		RegistryKeyBasicImpl that = (RegistryKeyBasicImpl) o;

		return javaType.equals( that.javaType )
				&& mutabilityPlan.equals( that.mutabilityPlan )
				&& ( comparator != null ? comparator.equals( that.comparator ) : that.comparator == null );

	}

	@Override
	public int hashCode() {
		int result = javaType.hashCode();
		result = 31 * result + mutabilityPlan.hashCode();
		result = 31 * result + ( comparator != null ? comparator.hashCode() : 0 );
		return result;
	}
}
