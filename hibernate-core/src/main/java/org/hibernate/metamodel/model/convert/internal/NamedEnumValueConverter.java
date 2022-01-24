/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.convert.internal;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Locale;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.convert.spi.EnumValueConverter;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.EnumJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * BasicValueConverter handling the conversion of an enum based on
 * JPA {@link jakarta.persistence.EnumType#STRING} strategy (storing the name)
 *
 * @author Steve Ebersole
 */
public class NamedEnumValueConverter<E extends Enum<E>> implements EnumValueConverter<E,String>, Serializable {
	private final EnumJavaType<E> domainTypeDescriptor;
	private final JdbcType jdbcType;
	private final JavaType<String> relationalTypeDescriptor;

	private transient ValueExtractor<String> valueExtractor;
	private transient ValueBinder<String> valueBinder;

	public NamedEnumValueConverter(
			EnumJavaType<E> domainTypeDescriptor,
			JdbcType jdbcType,
			JavaType<String> relationalTypeDescriptor) {
		this.domainTypeDescriptor = domainTypeDescriptor;
		this.jdbcType = jdbcType;
		this.relationalTypeDescriptor = relationalTypeDescriptor;

		this.valueExtractor = jdbcType.getExtractor( relationalTypeDescriptor );
		this.valueBinder = jdbcType.getBinder( relationalTypeDescriptor );
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
		return jdbcType.getJdbcTypeCode();
	}

	public int getDefaultSqlTypeCode() {
		return jdbcType.getDefaultSqlTypeCode();
	}

	@Override
	public String toSqlLiteral(Object value) {
		//noinspection rawtypes
		return String.format( Locale.ROOT, "'%s'", ( (Enum) value ).name() );
	}

	private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
		stream.defaultReadObject();

		this.valueExtractor = jdbcType.getExtractor( relationalTypeDescriptor );
		this.valueBinder = jdbcType.getBinder( relationalTypeDescriptor );
	}

	@Override
	public void writeValue(
			PreparedStatement statement,
			E value,
			int position,
			SharedSessionContractImplementor session) throws SQLException {
		final String jdbcValue = value == null ? null : value.name();
		valueBinder.bind( statement, jdbcValue, position, session );
	}
}
