/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.internal;

import java.io.Serializable;
import java.util.Comparator;

import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Immutable;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.MutabilityPlanExposer;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.usertype.CompositeUserType;

/**
 *
 * @author Christian Beikov
 */
public class CompositeUserTypeJavaTypeWrapper<J> implements JavaType<J> {
	protected final CompositeUserType<J> userType;
	private final MutabilityPlan<J> mutabilityPlan;

	private final Comparator<J> comparator;

	public CompositeUserTypeJavaTypeWrapper(CompositeUserType<J> userType) {
		this.userType = userType;

		MutabilityPlan<J> resolvedMutabilityPlan = null;

		if ( userType instanceof MutabilityPlanExposer ) {
			//noinspection unchecked
			resolvedMutabilityPlan = ( (MutabilityPlanExposer<J>) userType ).getExposedMutabilityPlan();
		}

		if ( resolvedMutabilityPlan == null ) {
			final Class<J> jClass = userType.returnedClass();
			if ( jClass != null ) {
				if ( jClass.getAnnotation( Immutable.class ) != null ) {
					resolvedMutabilityPlan = ImmutableMutabilityPlan.instance();
				}
			}
		}

		if ( resolvedMutabilityPlan == null ) {
			resolvedMutabilityPlan = new MutabilityPlanWrapper<>( userType );
		}

		this.mutabilityPlan = resolvedMutabilityPlan;

		if ( userType instanceof Comparator ) {
			//noinspection unchecked
			this.comparator = ( (Comparator<J>) userType );
		}
		else {
			this.comparator = this::compare;
		}
	}

	private int compare(J first, J second) {
		if ( userType.equals( first, second ) ) {
			return 0;
		}
		return Comparator.<J, Integer>comparing( userType::hashCode ).compare( first, second );
	}

	@Override
	public MutabilityPlan<J> getMutabilityPlan() {
		return mutabilityPlan;
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators context) {
		return null;
	}

	@Override
	public Comparator<J> getComparator() {
		return comparator;
	}

	@Override
	public int extractHashCode(J value) {
		return userType.hashCode(value );
	}

	@Override
	public boolean areEqual(J one, J another) {
		return userType.equals( one, another );
	}

	@Override
	public J fromString(CharSequence string) {
		throw new UnsupportedOperationException( "No support for parsing UserType values from String: " + userType );
	}

	@Override
	public <X> X unwrap(J value, Class<X> type, WrapperOptions options) {
		assert value == null || userType.returnedClass().isInstance( value );
		return type.cast( value );
	}

	@Override
	public <X> J wrap(X value, WrapperOptions options) {
//		assert value == null || userType.returnedClass().isInstance( value );
		//noinspection unchecked
		return (J) value;
	}

	@Override
	public Class<J> getJavaTypeClass() {
		return userType.returnedClass();
	}

	public static class MutabilityPlanWrapper<J> implements MutabilityPlan<J> {
		private final CompositeUserType<J> userType;

		public MutabilityPlanWrapper(CompositeUserType<J> userType) {
			this.userType = userType;
		}

		@Override
		public boolean isMutable() {
			return userType.isMutable();
		}

		@Override
		public J deepCopy(J value) {
			return userType.deepCopy( value );
		}

		@Override
		public Serializable disassemble(J value, SharedSessionContract session) {
			return userType.disassemble( value );
		}

		@Override
		public J assemble(Serializable cached, SharedSessionContract session) {
			return userType.assemble( cached, null );
		}
	}
}
