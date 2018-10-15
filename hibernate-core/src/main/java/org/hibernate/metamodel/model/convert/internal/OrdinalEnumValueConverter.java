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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.convert.spi.EnumValueConverter;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.EnumJavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.IntegerTypeDescriptor;

/**
 * BasicValueConverter handling the conversion of an enum based on
 * JPA {@link javax.persistence.EnumType#ORDINAL} strategy (storing the ordinal)
 *
 * @author Steve Ebersole
 */
public class OrdinalEnumValueConverter<E extends Enum> implements EnumValueConverter<E,Integer>, Serializable {

	private final EnumJavaTypeDescriptor<E> enumJavaDescriptor;

	private transient ValueExtractor<E> valueExtractor;

	private transient ValueBinder<Integer> valueBinder;

	public OrdinalEnumValueConverter(EnumJavaTypeDescriptor<E> enumJavaDescriptor) {
		this.enumJavaDescriptor = enumJavaDescriptor;
		this.valueExtractor = createValueExtractor( enumJavaDescriptor );
		this.valueBinder = createValueBinder();
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
		return Types.INTEGER;
	}

	@Override
	public EnumJavaTypeDescriptor<E> getJavaDescriptor() {
		return enumJavaDescriptor;
	}

	@Override
	public E readValue(ResultSet resultSet, String name, SharedSessionContractImplementor session) throws SQLException {
		return valueExtractor.extract( resultSet, name, session );
	}

	@Override
	public void writeValue(PreparedStatement statement, E value, int position, SharedSessionContractImplementor session) throws SQLException {
		final Integer jdbcValue = value == null ? null : toRelationalValue( value );

		valueBinder.bind( statement, jdbcValue, position, session );
	}

	@Override
	@SuppressWarnings("unchecked")
	public String toSqlLiteral(Object value) {
		return Integer.toString( ( (E) value ).ordinal() );
	}

	private static <T extends Enum> ValueExtractor<T> createValueExtractor(EnumJavaTypeDescriptor<T> enumJavaDescriptor) {
		return IntegerTypeDescriptor.INSTANCE.getExtractor( enumJavaDescriptor );
	}

	private static ValueBinder<Integer> createValueBinder() {
		return IntegerTypeDescriptor.INSTANCE.getBinder( org.hibernate.type.descriptor.java.IntegerTypeDescriptor.INSTANCE );
	}

	private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
		stream.defaultReadObject();

		this.valueExtractor = createValueExtractor( enumJavaDescriptor );
		this.valueBinder = createValueBinder();
	}
}
