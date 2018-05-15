/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.converter.custom;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.MutableMutabilityPlan;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class MyCustomJavaTypeDescriptor implements BasicJavaDescriptor<MyCustomJavaType> {
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
	public SqlTypeDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context) {
		return MyCustomSqlTypeDescriptor.INSTANCE;
	}

	@Override
	public MutabilityPlan getMutabilityPlan() {
		return mutabilityPlan;
	}

	@Override
	public MyCustomJavaType fromString(String string) {
		return StringHelper.isEmpty( string ) ? null : new MyCustomJavaType( string );
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
