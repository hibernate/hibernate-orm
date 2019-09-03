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
import java.util.Locale;

import org.hibernate.metamodel.model.convert.spi.EnumValueConverter;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.EnumJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * BasicValueConverter handling the conversion of an enum based on
 * JPA {@link javax.persistence.EnumType#STRING} strategy (storing the name)
 *
 * @author Steve Ebersole
 */
public class NamedEnumValueConverter<E extends Enum> implements EnumValueConverter<E,String>, Serializable {
	private final EnumJavaTypeDescriptor<E> domainTypeDescriptor;
	private final SqlTypeDescriptor sqlTypeDescriptor;
	private final JavaTypeDescriptor<String> relationalTypeDescriptor;

	private transient ValueExtractor<String> valueExtractor;
	private transient ValueBinder<String> valueBinder;

	public NamedEnumValueConverter(
			EnumJavaTypeDescriptor<E> domainTypeDescriptor,
			SqlTypeDescriptor sqlTypeDescriptor,
			JavaTypeDescriptor<String> relationalTypeDescriptor) {
		this.domainTypeDescriptor = domainTypeDescriptor;
		this.sqlTypeDescriptor = sqlTypeDescriptor;
		this.relationalTypeDescriptor = relationalTypeDescriptor;

		this.valueExtractor = sqlTypeDescriptor.getExtractor( relationalTypeDescriptor );
		this.valueBinder = sqlTypeDescriptor.getBinder( relationalTypeDescriptor );
	}

	@Override
	public EnumJavaTypeDescriptor<E> getDomainJavaDescriptor() {
		return domainTypeDescriptor;
	}

	@Override
	public JavaTypeDescriptor<String> getRelationalJavaDescriptor() {
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
		return Types.VARCHAR;
	}

	@Override
	@SuppressWarnings("unchecked")
	public String toSqlLiteral(Object value) {
		return String.format( Locale.ROOT, "'%s'", ( (E) value ).name() );
	}

	private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
		stream.defaultReadObject();

		this.valueExtractor = sqlTypeDescriptor.getExtractor( relationalTypeDescriptor );
		this.valueBinder = sqlTypeDescriptor.getBinder( relationalTypeDescriptor );
	}
}
