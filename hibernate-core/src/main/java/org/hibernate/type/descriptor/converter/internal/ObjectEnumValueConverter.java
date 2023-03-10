/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.converter.internal;

import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.NamedAuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.converter.spi.EnumValueConverter;
import org.hibernate.type.descriptor.java.EnumJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import java.io.Serializable;

import static java.util.Arrays.sort;
import static java.util.Collections.emptySet;
import static org.hibernate.type.descriptor.converter.internal.EnumHelper.getEnumeratedValues;

/**
 * {@link org.hibernate.type.descriptor.converter.spi.BasicValueConverter} handling the
 * conversion of an enum to a PostgreSQL-style named {@code enum} type.
 *
 * @see Dialect#hasNamedEnumTypes()
 *
 * @author Gavin King
 */
public class ObjectEnumValueConverter implements EnumValueConverter, Serializable {
	private final EnumJavaType domainTypeDescriptor;
	private final JdbcType jdbcType;
	private final JavaType relationalTypeDescriptor;

	public ObjectEnumValueConverter(
			EnumJavaType domainTypeDescriptor,
			JdbcType jdbcType,
			JavaType relationalTypeDescriptor) {
		this.domainTypeDescriptor = domainTypeDescriptor;
		this.jdbcType = jdbcType;
		this.relationalTypeDescriptor = relationalTypeDescriptor;
	}

	@Override
	public EnumJavaType getDomainJavaType() {
		return domainTypeDescriptor;
	}

	@Override
	public JavaType getRelationalJavaType() {
		return relationalTypeDescriptor;
	}

	@Override
	public Object toDomainValue(Object relationalForm) {
		return relationalForm instanceof String
				? domainTypeDescriptor.fromName( (String) relationalForm )
				: relationalForm;
	}

	@Override
	public Object toRelationalValue(Object domainForm) {
		return domainForm;
	}

	@Override
	public int getJdbcTypeCode() {
		return jdbcType.getDefaultSqlTypeCode();
	}

	@Override
	public String toSqlLiteral(Object value) {
		return "'" + ( (Enum) value ).name() + "'";
	}

	@Override
	public String getSpecializedTypeDeclaration(JdbcType jdbcType, Dialect dialect) {
		return getDomainJavaType().getJavaTypeClass().getSimpleName();
	}

	@Override
	public AuxiliaryDatabaseObject getAuxiliaryDatabaseObject(JdbcType jdbcType, Dialect dialect, Namespace defaultNamespace) {
		Class enumClass = getDomainJavaType().getJavaTypeClass();
		String[] values = getEnumeratedValues( enumClass );
		sort( values ); //sort alphabetically, to guarantee alphabetical ordering in queries with 'order by'
		String name = enumClass.getSimpleName();
		String create = "create type " + name + " as " + dialect.getEnumTypeDeclaration( values ) +
				"; create cast (varchar as " + name + ") with inout as implicit;";
		String drop = "drop type " + name + " cascade";
		return new NamedAuxiliaryDatabaseObject( name, defaultNamespace, create, drop, emptySet(), true );
	}
}
