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

import static java.util.Collections.emptySet;

/**
 * BasicValueConverter handling the conversion of an enum based on
 * JPA {@link jakarta.persistence.EnumType#STRING} strategy (storing the name)
 *
 * @author Steve Ebersole
 */
public class NativeEnumValueConverter<E extends Enum<E>> implements EnumValueConverter<E,Object>, Serializable {
	private final EnumJavaType<E> domainTypeDescriptor;
	private final JdbcType jdbcType;
	private final JavaType<Object> relationalTypeDescriptor;

	public NativeEnumValueConverter(
			EnumJavaType<E> domainTypeDescriptor,
			JdbcType jdbcType,
			JavaType<Object> relationalTypeDescriptor) {
		this.domainTypeDescriptor = domainTypeDescriptor;
		this.jdbcType = jdbcType;
		this.relationalTypeDescriptor = relationalTypeDescriptor;
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
		return relationalForm instanceof String
				? domainTypeDescriptor.fromName( (String) relationalForm )
				: (E) relationalForm;
	}

	@Override
	public Object toRelationalValue(E domainForm) {
		return domainForm;
	}

	@Override
	public int getJdbcTypeCode() {
		return jdbcType.getDefaultSqlTypeCode();
	}

	@Override
	public String toSqlLiteral(Object value) {
		return "'" + ( (Enum<?>) value ).name() + "'";
	}

	@Override
	public String getCheckCondition(String columnName, JdbcType jdbcType, Dialect dialect) {
		return dialect.getCheckCondition( columnName, getDomainJavaType().getJavaTypeClass() );
	}

	@Override
	public String getSpecializedTypeDeclaration(JdbcType jdbcType, Dialect dialect) {
		return dialect.getEnumTypeDeclaration( getDomainJavaType().getJavaTypeClass() );
	}

	@Override
	public AuxiliaryDatabaseObject getAuxiliaryDatabaseObject(JdbcType jdbcType, Dialect dialect, Namespace defaultNamespace) {
		Class<? extends Enum<?>> enumClass = getDomainJavaType().getJavaTypeClass();
		String name = enumClass.getSimpleName();
		String[] create = dialect.getCreateEnumTypeCommand( enumClass );
		if ( create != null ) {
			String[] drop = new String[] { "drop type " + name + " cascade" }; //TODO: move to Dialect
			return new NamedAuxiliaryDatabaseObject( name, defaultNamespace, create, drop, emptySet(), true );
		}
		else {
			return null;
		}
	}
}
