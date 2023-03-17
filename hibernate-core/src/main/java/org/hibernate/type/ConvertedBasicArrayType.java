/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * A converted basic array type.
 *
 * @author Christian Beikov
 */
public class ConvertedBasicArrayType<T> extends BasicArrayType<T> {

	private final BasicValueConverter<T[], ?> converter;
	private final ValueExtractor<T[]> jdbcValueExtractor;
	private final ValueBinder<T[]> jdbcValueBinder;
	private final JdbcLiteralFormatter<T[]> jdbcLiteralFormatter;

	@SuppressWarnings("unchecked")
	public ConvertedBasicArrayType(
			BasicType<T> baseDescriptor,
			JdbcType arrayJdbcType,
			JavaType<T[]> arrayTypeDescriptor,
			BasicValueConverter<T[], ?> converter) {
		super( baseDescriptor, arrayJdbcType, arrayTypeDescriptor );
		this.converter = converter;
		this.jdbcValueBinder = (ValueBinder<T[]>) arrayJdbcType.getBinder( converter.getRelationalJavaType() );
		this.jdbcValueExtractor = (ValueExtractor<T[]>) arrayJdbcType.getExtractor( converter.getRelationalJavaType() );
		this.jdbcLiteralFormatter = (JdbcLiteralFormatter<T[]>) arrayJdbcType.getJdbcLiteralFormatter( converter.getRelationalJavaType() );
	}

	@Override
	public BasicValueConverter<T[], ?> getValueConverter() {
		return converter;
	}

	@Override
	public JavaType<?> getJdbcJavaType() {
		return converter.getRelationalJavaType();
	}

	@Override
	public ValueExtractor<T[]> getJdbcValueExtractor() {
		return jdbcValueExtractor;
	}

	@Override
	public ValueBinder<T[]> getJdbcValueBinder() {
		return jdbcValueBinder;
	}

	@Override
	public JdbcLiteralFormatter<T[]> getJdbcLiteralFormatter() {
		return jdbcLiteralFormatter;
	}
}
