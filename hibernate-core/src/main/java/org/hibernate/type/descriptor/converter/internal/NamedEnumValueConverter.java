/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.converter.internal;

import java.io.Serializable;

import org.hibernate.type.descriptor.converter.spi.EnumValueConverter;
import org.hibernate.type.descriptor.java.EnumJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * BasicValueConverter handling the conversion of an enum based on
 * JPA {@link jakarta.persistence.EnumType#STRING} strategy (storing the name)
 *
 * @author Steve Ebersole
 *
 * @deprecated we no longer use converters to handle enum mappings
 */
@Deprecated(since="6.3", forRemoval=true)
public class NamedEnumValueConverter<E extends Enum<E>> implements EnumValueConverter<E,String>, Serializable {
	private final EnumJavaType<E> domainTypeDescriptor;
	private final JdbcType jdbcType;
	private final JavaType<String> relationalTypeDescriptor;

	public NamedEnumValueConverter(
			EnumJavaType<E> domainTypeDescriptor,
			JdbcType jdbcType,
			JavaType<String> relationalTypeDescriptor) {
		this.domainTypeDescriptor = domainTypeDescriptor;
		this.jdbcType = jdbcType;
		this.relationalTypeDescriptor = relationalTypeDescriptor;
	}

	@Override
	public EnumJavaType<E> getDomainJavaType() {
		return domainTypeDescriptor;
	}

	@Override
	public JavaType<String> getRelationalJavaType() {
		return relationalTypeDescriptor;
	}

	@Override
	public E toDomainValue(String relationalForm) {
		return domainTypeDescriptor.fromName( relationalForm );
	}

	@Override
	public String toRelationalValue(E domainForm) {
		return domainTypeDescriptor.toName( domainForm );
	}

	@Override
	public int getJdbcTypeCode() {
		return jdbcType.getDefaultSqlTypeCode();
	}

	@Override
	public String toSqlLiteral(Object value) {
		return "'" + ( (Enum<?>) value ).name() + "'";
	}
}
