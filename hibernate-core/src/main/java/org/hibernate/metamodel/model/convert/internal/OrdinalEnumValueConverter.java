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
import java.sql.Types;

import org.hibernate.metamodel.model.convert.spi.EnumValueConverter;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.EnumJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * BasicValueConverter handling the conversion of an enum based on
 * JPA {@link javax.persistence.EnumType#ORDINAL} strategy (storing the ordinal)
 *
 * @author Steve Ebersole
 */
public class OrdinalEnumValueConverter<E extends Enum> implements EnumValueConverter<E,Integer>, Serializable {

	private final EnumJavaTypeDescriptor<E> enumJavaDescriptor;
	private final SqlTypeDescriptor sqlTypeDescriptor;
	private final JavaTypeDescriptor<Integer> relationalJavaDescriptor;

	private transient ValueExtractor<Integer> valueExtractor;
	private transient ValueBinder<Integer> valueBinder;

	public OrdinalEnumValueConverter(
			EnumJavaTypeDescriptor<E> enumJavaDescriptor,
			SqlTypeDescriptor sqlTypeDescriptor,
			JavaTypeDescriptor<Integer> relationalJavaDescriptor) {
		this.enumJavaDescriptor = enumJavaDescriptor;
		this.sqlTypeDescriptor = sqlTypeDescriptor;
		this.relationalJavaDescriptor = relationalJavaDescriptor;

		this.valueExtractor = sqlTypeDescriptor.getExtractor( relationalJavaDescriptor );
		this.valueBinder = sqlTypeDescriptor.getBinder( relationalJavaDescriptor );
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
	public EnumJavaTypeDescriptor<E> getDomainJavaDescriptor() {
		return enumJavaDescriptor;
	}

	@Override
	public JavaTypeDescriptor<Integer> getRelationalJavaDescriptor() {
		return relationalJavaDescriptor;
	}

	@Override
	@SuppressWarnings("unchecked")
	public String toSqlLiteral(Object value) {
		return Integer.toString( ( (E) value ).ordinal() );
	}

	private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
		stream.defaultReadObject();

		this.valueExtractor = sqlTypeDescriptor.getExtractor( relationalJavaDescriptor );
		this.valueBinder = sqlTypeDescriptor.getBinder( relationalJavaDescriptor );
	}
}
