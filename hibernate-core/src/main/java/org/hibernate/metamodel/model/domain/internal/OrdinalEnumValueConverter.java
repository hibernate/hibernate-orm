/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.internal.EnumJavaDescriptor;

/**
 * BasicValueConverter handling the conversion of an enum based on
 * JPA {@link javax.persistence.EnumType#ORDINAL} strategy (storing the ordinal)
 *
 * @author Steve Ebersole
 */
public class OrdinalEnumValueConverter<E extends Enum> implements BasicValueConverter<E,Integer> {
	private final EnumJavaDescriptor<E> enumJavaDescriptor;

	public OrdinalEnumValueConverter(EnumJavaDescriptor<E> enumJavaDescriptor) {
		this.enumJavaDescriptor = enumJavaDescriptor;
	}

	@Override
	@SuppressWarnings("unchecked")
	public E toDomainValue(Integer relationalForm, SharedSessionContractImplementor session) {
		return enumJavaDescriptor.fromOrdinal( relationalForm );
	}

	@Override
	public Integer toRelationalValue(E domainForm, SharedSessionContractImplementor session) {
		return enumJavaDescriptor.toOrdinal( domainForm );
	}
}
