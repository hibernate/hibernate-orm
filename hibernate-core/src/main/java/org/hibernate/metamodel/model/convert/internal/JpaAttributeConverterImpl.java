/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.convert.internal;

import javax.persistence.AttributeConverter;

import org.hibernate.metamodel.model.convert.spi.JpaAttributeConverter;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.type.descriptor.java.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Standard implementation of JpaAttributeConverter
 *
 * @author Steve Ebersole
 */
public class JpaAttributeConverterImpl<O,R> implements JpaAttributeConverter<O,R> {
	private final ManagedBean<AttributeConverter<O,R>> attributeConverterBean;
	private final JavaTypeDescriptor<AttributeConverter<O, R>> converterJavaTypeDescriptor;
	private final BasicJavaDescriptor<O> domainJavaTypeDescriptor;
	private final BasicJavaDescriptor<R> relationalJavaTypeDescriptor;

	public JpaAttributeConverterImpl(
			ManagedBean<AttributeConverter<O, R>> attributeConverterBean,
			JavaTypeDescriptor<AttributeConverter<O,R>> converterJavaTypeDescriptor,
			JavaTypeDescriptor<O> domainJavaTypeDescriptor,
			JavaTypeDescriptor<R> relationalJavaTypeDescriptor) {
		this.attributeConverterBean = attributeConverterBean;
		this.converterJavaTypeDescriptor = converterJavaTypeDescriptor;
		this.domainJavaTypeDescriptor = (BasicJavaDescriptor<O>) domainJavaTypeDescriptor;
		this.relationalJavaTypeDescriptor = (BasicJavaDescriptor<R>) relationalJavaTypeDescriptor;
	}

	@Override
	public ManagedBean<AttributeConverter<O, R>> getConverterBean() {
		return attributeConverterBean;
	}

	@Override
	public O toDomainValue(R relationalForm) {
		return attributeConverterBean.getBeanInstance().convertToEntityAttribute( relationalForm );
	}

	@Override
	public R toRelationalValue(O domainForm) {
		return attributeConverterBean.getBeanInstance().convertToDatabaseColumn( domainForm );
	}

	@Override
	public JavaTypeDescriptor<AttributeConverter<O, R>> getConverterJavaTypeDescriptor() {
		return converterJavaTypeDescriptor;
	}

	@Override
	public BasicJavaDescriptor<O> getDomainJavaTypeDescriptor() {
		return domainJavaTypeDescriptor;
	}

	@Override
	public BasicJavaDescriptor<R> getRelationalJavaTypeDescriptor() {
		return relationalJavaTypeDescriptor;
	}
}
