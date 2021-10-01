/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.io.Serializable;
import java.sql.Types;

import org.hibernate.Incubating;
import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.java.BooleanTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptorIndicators;

/**
 * A type that maps between {@link Types#BOOLEAN BOOLEAN} and {@link Boolean}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class BooleanType
		extends AbstractSingleColumnStandardBasicType<Boolean>
		implements PrimitiveType<Boolean>, DiscriminatorType<Boolean>, AdjustableBasicType<Boolean> {
	public static final BooleanType INSTANCE = new BooleanType();

	public BooleanType() {
		this( org.hibernate.type.descriptor.jdbc.BooleanTypeDescriptor.INSTANCE, BooleanTypeDescriptor.INSTANCE );
	}

	protected BooleanType(JdbcTypeDescriptor jdbcTypeDescriptor, BooleanTypeDescriptor javaTypeDescriptor) {
		super( jdbcTypeDescriptor, javaTypeDescriptor );
	}

	@Incubating
	public BooleanType(JdbcTypeDescriptor jdbcTypeDescriptor, JavaTypeDescriptor<Boolean> javaTypeDescriptor) {
		super( jdbcTypeDescriptor, javaTypeDescriptor );
	}

	@Override
	public String getName() {
		return "boolean";
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[] { getName(), boolean.class.getName(), Boolean.class.getName() };
	}
	@Override
	public Class getPrimitiveClass() {
		return boolean.class;
	}
	@Override
	public Serializable getDefaultValue() {
		return Boolean.FALSE;
	}
	@Override
	public Boolean stringToObject(CharSequence string) {
		return fromString( string );
	}

	@Override
	public String objectToSQLString(Boolean value, Dialect dialect) {
		return dialect.toBooleanValueString( value );
	}

	@Override
	public <X> BasicType<X> resolveIndicatedType(
			JdbcTypeDescriptorIndicators indicators,
			JavaTypeDescriptor<X> domainJtd) {
		final int preferredSqlTypeCodeForBoolean = indicators.getPreferredSqlTypeCodeForBoolean();
		final JdbcTypeDescriptor jdbcTypeDescriptor;
		// We treat BIT like BOOLEAN because it uses the same JDBC access methods
		if ( preferredSqlTypeCodeForBoolean != Types.BIT && preferredSqlTypeCodeForBoolean != getJdbcTypeDescriptor().getJdbcTypeCode() ) {
			jdbcTypeDescriptor = indicators.getTypeConfiguration()
					.getJdbcTypeDescriptorRegistry()
					.getDescriptor( preferredSqlTypeCodeForBoolean );
		}
		else {
			jdbcTypeDescriptor = indicators.getTypeConfiguration()
					.getJdbcTypeDescriptorRegistry()
					.getDescriptor( Types.BOOLEAN );
		}
		if ( jdbcTypeDescriptor != getJdbcTypeDescriptor() ) {
			//noinspection unchecked
			return (BasicType<X>) indicators.getTypeConfiguration()
					.getBasicTypeRegistry()
					.resolve( getJavaTypeDescriptor(), jdbcTypeDescriptor, getName() );
		}

		//noinspection unchecked
		return (BasicType<X>) this;
	}
}
