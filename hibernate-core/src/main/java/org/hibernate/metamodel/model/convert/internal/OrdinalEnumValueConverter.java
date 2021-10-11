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
import java.sql.Types;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.convert.spi.EnumValueConverter;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.EnumJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;

/**
 * BasicValueConverter handling the conversion of an enum based on
 * JPA {@link jakarta.persistence.EnumType#ORDINAL} strategy (storing the ordinal)
 *
 * @author Steve Ebersole
 */
public class OrdinalEnumValueConverter<E extends Enum<E>> implements EnumValueConverter<E,Integer>, Serializable {

	private final EnumJavaTypeDescriptor<E> enumJavaDescriptor;
	private final JdbcTypeDescriptor jdbcTypeDescriptor;
	private final JavaType<Integer> relationalJavaDescriptor;

	private transient ValueExtractor<Integer> valueExtractor;
	private transient ValueBinder<Integer> valueBinder;

	public OrdinalEnumValueConverter(
			EnumJavaTypeDescriptor<E> enumJavaDescriptor,
			JdbcTypeDescriptor jdbcTypeDescriptor,
			JavaType<Integer> relationalJavaDescriptor) {
		this.enumJavaDescriptor = enumJavaDescriptor;
		this.jdbcTypeDescriptor = jdbcTypeDescriptor;
		this.relationalJavaDescriptor = relationalJavaDescriptor;

		this.valueExtractor = jdbcTypeDescriptor.getExtractor( relationalJavaDescriptor );
		this.valueBinder = jdbcTypeDescriptor.getBinder( relationalJavaDescriptor );
	}

	@Override
	public E toDomainValue(Integer relationalForm) {
		return enumJavaDescriptor.fromOrdinal( relationalForm );
	}

	@Override
	public Integer toRelationalValue(E domainForm) {
		return enumJavaDescriptor.toOrdinal( domainForm );
	}

	@Override
	public int getJdbcTypeCode() {
		// note, even though we convert the enum to
		// an Integer here, we actually map it to a
		// database column of type TINYINT by default
		return Types.TINYINT;
	}

	@Override
	public EnumJavaTypeDescriptor<E> getDomainJavaDescriptor() {
		return enumJavaDescriptor;
	}

	@Override
	public JavaType<Integer> getRelationalJavaDescriptor() {
		return relationalJavaDescriptor;
	}

	@Override
	public String toSqlLiteral(Object value) {
		//noinspection rawtypes
		return Integer.toString( ( (Enum) value ).ordinal() );
	}

	private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
		stream.defaultReadObject();

		this.valueExtractor = jdbcTypeDescriptor.getExtractor( relationalJavaDescriptor );
		this.valueBinder = jdbcTypeDescriptor.getBinder( relationalJavaDescriptor );
	}

	@Override
	public void writeValue(
			PreparedStatement statement, E value, int position, SharedSessionContractImplementor session)
			throws SQLException {
		valueBinder.bind( statement, toRelationalValue( value ), position, session );
	}
}
