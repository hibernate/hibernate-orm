/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.converted.converter.custom;

import org.hibernate.internal.util.CharSequenceHelper;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.MutableMutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptorIndicators;

/**
 * @author Steve Ebersole
 */
public class MyCustomJavaTypeDescriptor implements BasicJavaTypeDescriptor<MyCustomJavaType> {
	/**
	 * Singleton access
	 */
	public static final MyCustomJavaTypeDescriptor INSTANCE = new MyCustomJavaTypeDescriptor();

	private final MutableMutabilityPlan<MyCustomJavaType> mutabilityPlan = new MutableMutabilityPlan<MyCustomJavaType>() {
		@Override
		protected MyCustomJavaType deepCopyNotNull(MyCustomJavaType value) {
			return value == null ? null : new MyCustomJavaType( value.getPayload() );
		}
	};

	private MyCustomJavaTypeDescriptor() {
	}

	@Override
	public Class getJavaTypeClass() {
		return MyCustomJavaType.class;
	}

	@Override
	public JdbcTypeDescriptor getRecommendedJdbcType(JdbcTypeDescriptorIndicators context) {
		return MyCustomJdbcTypeDescriptor.INSTANCE;
	}

	@Override
	public MutabilityPlan getMutabilityPlan() {
		return mutabilityPlan;
	}

	@Override
	public MyCustomJavaType fromString(CharSequence string) {
		return CharSequenceHelper.isEmpty( string ) ? null : new MyCustomJavaType( string.toString() );
	}

	@Override
	public MyCustomJavaType wrap(Object value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		else if ( String.class.isInstance( value ) ) {
			return new MyCustomJavaType( (String) value );
		}

		throw new UnsupportedOperationException( "Wrapping value as MyCustomJavaType only supported for String or MyCustomJdbcType : " + value );
	}

	@Override
	public Object unwrap(MyCustomJavaType value, Class type, WrapperOptions options) {
		if ( String.class.isAssignableFrom( type ) ) {
			return value.getPayload();
		}

		throw new UnsupportedOperationException( "Unwrapping MyCustomJavaType value only supported for String or MyCustomJdbcType : " + value );
	}
}
