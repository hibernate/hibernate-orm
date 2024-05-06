/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.converter.internal;

import java.io.Serializable;
import java.util.Locale;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.converter.spi.EnumValueConverter;
import org.hibernate.type.descriptor.java.EnumJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import static java.util.Arrays.sort;
import static org.hibernate.type.descriptor.converter.internal.EnumHelper.getEnumeratedValues;

/**
 * BasicValueConverter handling the conversion of an enum based on
 * JPA {@link jakarta.persistence.EnumType#STRING} strategy (storing the name)
 *
 * @author Steve Ebersole
 */
public class NamedEnumValueConverter<E extends Enum<E>> implements EnumValueConverter<E, Object>, Serializable {
	private final EnumJavaType<E> domainTypeDescriptor;
	private final JdbcType jdbcType;
	private final JavaType<Object> relationalTypeDescriptor;

	public NamedEnumValueConverter(
			EnumJavaType<E> domainTypeDescriptor,
			JdbcType jdbcType,
			JavaType<?> relationalTypeDescriptor) {
		this.domainTypeDescriptor = domainTypeDescriptor;
		this.jdbcType = jdbcType;
		//noinspection unchecked
		this.relationalTypeDescriptor = (JavaType<Object>) relationalTypeDescriptor;
	}

	@Override
	public EnumJavaType<E> getDomainJavaType() {
		return domainTypeDescriptor;
	}

	@Override
	public JavaType<Object> getRelationalJavaType() {
		return relationalTypeDescriptor;
	}

	@Override
	public E toDomainValue(Object relationalForm) {
		return relationalForm == null
				? null
				: domainTypeDescriptor.fromName( relationalTypeDescriptor.toString( relationalForm ) );
	}

	@Override
	public Object toRelationalValue(E domainForm) {
		final String name = domainTypeDescriptor.toName( domainForm );
		return name == null ? null : relationalTypeDescriptor.fromString( name );
	}

	@Override
	public int getJdbcTypeCode() {
		return jdbcType.getDefaultSqlTypeCode();
	}

	@Override
	public String toSqlLiteral(Object value) {
		//noinspection rawtypes
		return String.format( Locale.ROOT, "'%s'", ( (Enum) value ).name() );
	}

	@Override
	public String getCheckCondition(String columnName, JdbcType jdbcType, Dialect dialect) {
		return dialect.getCheckCondition( columnName, getEnumeratedValues( getDomainJavaType().getJavaTypeClass() ) );
	}

	@Override
	public String getSpecializedTypeDeclaration(JdbcType jdbcType, Dialect dialect) {
		String[] values = getEnumeratedValues( getDomainJavaType().getJavaTypeClass() );
		sort( values ); //sort alphabetically, to guarantee alphabetical ordering in queries with 'order by'
		return dialect.getEnumTypeDeclaration( values );
	}
}
