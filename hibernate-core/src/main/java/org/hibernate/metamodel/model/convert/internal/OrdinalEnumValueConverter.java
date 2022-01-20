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

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.convert.spi.EnumValueConverter;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.EnumJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * BasicValueConverter handling the conversion of an enum based on
 * JPA {@link jakarta.persistence.EnumType#ORDINAL} strategy (storing the ordinal)
 *
 * @author Steve Ebersole
 */
public class OrdinalEnumValueConverter<E extends Enum<E>> implements EnumValueConverter<E,Integer>, Serializable {

	private final EnumJavaType<E> enumJavaType;
	private final JdbcType jdbcType;
	private final JavaType<Integer> relationalJavaType;

	private transient ValueExtractor<Integer> valueExtractor;
	private transient ValueBinder<Integer> valueBinder;

	public OrdinalEnumValueConverter(
			EnumJavaType<E> enumJavaType,
			JdbcType jdbcType,
			JavaType<Integer> relationalJavaType) {
		this.enumJavaType = enumJavaType;
		this.jdbcType = jdbcType;
		this.relationalJavaType = relationalJavaType;

		this.valueExtractor = jdbcType.getExtractor( relationalJavaType );
		this.valueBinder = jdbcType.getBinder( relationalJavaType );
	}

	@Override
	public E toDomainValue(Integer relationalForm) {
		return enumJavaType.fromOrdinal( relationalForm );
	}

	@Override
	public Integer toRelationalValue(E domainForm) {
		return enumJavaType.toOrdinal( domainForm );
	}

	@Override
	public int getJdbcTypeCode() {
		return jdbcType.getJdbcTypeCode();
	}

	@Override
	public EnumJavaType<E> getDomainJavaType() {
		return enumJavaType;
	}

	@Override
	public JavaType<Integer> getRelationalJavaType() {
		return relationalJavaType;
	}

	@Override
	public String toSqlLiteral(Object value) {
		//noinspection rawtypes
		return Integer.toString( ( (Enum) value ).ordinal() );
	}

	private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
		stream.defaultReadObject();

		this.valueExtractor = jdbcType.getExtractor( relationalJavaType );
		this.valueBinder = jdbcType.getBinder( relationalJavaType );
	}

	@Override
	public void writeValue(
			PreparedStatement statement, E value, int position, SharedSessionContractImplementor session)
			throws SQLException {
		valueBinder.bind( statement, toRelationalValue( value ), position, session );
	}
}
