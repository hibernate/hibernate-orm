/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.internal;

import java.io.Serializable;
import java.util.Comparator;

import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Immutable;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.MutabilityPlanExposer;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptorIndicators;
import org.hibernate.usertype.Sized;
import org.hibernate.usertype.UserType;

/**
 *
 * @author Steve Ebersole
 */
public class UserTypeJavaTypeWrapper<J> implements BasicJavaType<J> {
	protected final UserType<J> userType;
	private final MutabilityPlan<J> mutabilityPlan;

	private final Comparator<J> comparator;
	private final Sized sized;

	public UserTypeJavaTypeWrapper(UserType<J> userType) {
		this.userType = userType;

		MutabilityPlan<J> resolvedMutabilityPlan = null;

		if ( userType instanceof MutabilityPlanExposer ) {
			//noinspection unchecked
			resolvedMutabilityPlan = ( (MutabilityPlanExposer<J>) userType ).getExposedMutabilityPlan();
		}

		if ( resolvedMutabilityPlan == null ) {
			final Class<J> jClass = userType.returnedClass();
			if ( jClass.getAnnotation( Immutable.class ) != null ) {
				resolvedMutabilityPlan = ImmutableMutabilityPlan.instance();
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

		if ( userType instanceof Sized ) {
			this.sized = (Sized) userType;
		}
		else {
			this.sized = null;
		}
	}

	private int compare(J first, J second) {
		if ( userType.equals( first, second ) ) {
			return 0;
		}
		return Comparator.comparing( userType::hashCode ).compare( first, second );
	}

	@Override
	public MutabilityPlan<J> getMutabilityPlan() {
		return mutabilityPlan;
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeDescriptorIndicators context) {
		return context.getTypeConfiguration().getJdbcTypeDescriptorRegistry().getDescriptor( userType.sqlTypes()[0] );
	}

	@Override
	public long getDefaultSqlLength(Dialect dialect, JdbcType jdbcType) {
		if ( sized != null ) {
			return sized.defaultSizes()[0].getLength();
		}

		return Size.DEFAULT_LENGTH;
	}

	@Override
	public int getDefaultSqlPrecision(Dialect dialect) {
		if ( sized != null ) {
			return sized.defaultSizes()[0].getPrecision();
		}

		return Size.DEFAULT_PRECISION;
	}

	@Override
	public int getDefaultSqlScale() {
		if ( sized != null ) {
			return sized.defaultSizes()[0].getScale();
		}

		return Size.DEFAULT_SCALE;
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

		//noinspection unchecked
		return (X) value;
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
		private final UserType<J> userType;

		public MutabilityPlanWrapper(UserType<J> userType) {
			this.userType = userType;
		}

		@Override
		public boolean isMutable() {
			return userType.isMutable();
		}

		@Override
		public J deepCopy(J value) {
			//noinspection unchecked
			return (J) userType.deepCopy( value );
		}

		@Override
		public Serializable disassemble(J value, SharedSessionContract session) {
			return userType.disassemble( value );
		}

		@Override
		public J assemble(Serializable cached, SharedSessionContract session) {
			//noinspection unchecked
			return (J) userType.disassemble( cached );
		}
	}
}
