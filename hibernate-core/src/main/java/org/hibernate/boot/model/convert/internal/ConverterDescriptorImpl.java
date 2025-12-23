/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.convert.internal;

import jakarta.persistence.AttributeConverter;
import org.hibernate.boot.model.convert.spi.AutoApplicableConverterDescriptor;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.convert.spi.JpaAttributeConverterCreationContext;
import org.hibernate.type.descriptor.converter.internal.AttributeConverterBean;
import org.hibernate.type.descriptor.converter.spi.JpaAttributeConverter;

import java.lang.reflect.Type;


class ConverterDescriptorImpl<X, Y> implements ConverterDescriptor<X, Y> {
	private final Class<? extends AttributeConverter<X, Y>> converterType;
	private final Type domainTypeToMatch;
	private final Type relationalType;
	private final AutoApplicableConverterDescriptor autoApplyDescriptor;

	ConverterDescriptorImpl(
			Class<? extends AttributeConverter<X, Y>> converterType,
			Type domainTypeToMatch,
			Type relationalType,
			boolean autoApply) {
		this.converterType = converterType;
		this.domainTypeToMatch = domainTypeToMatch;
		this.relationalType = relationalType;
		this.autoApplyDescriptor = autoApply
				? new AutoApplicableConverterDescriptorStandardImpl( this )
				: AutoApplicableConverterDescriptorBypassedImpl.INSTANCE;
	}

	@Override
	public Class<? extends AttributeConverter<X, Y>> getAttributeConverterClass() {
		return converterType;
	}

	@Override
	public Type getDomainValueResolvedType() {
		return domainTypeToMatch;
	}

	@Override
	public Type getRelationalValueResolvedType() {
		return relationalType;
	}

	@Override
	public AutoApplicableConverterDescriptor getAutoApplyDescriptor() {
		return autoApplyDescriptor;
	}

	@Override
	public JpaAttributeConverter<X, Y> createJpaAttributeConverter(JpaAttributeConverterCreationContext context) {
		final var javaTypeRegistry = context.getTypeConfiguration().getJavaTypeRegistry();
		final var converterBean = context.getManagedBeanRegistry().getBean( converterType );
		return new AttributeConverterBean<>(
				converterBean,
				javaTypeRegistry.resolveDescriptor( converterBean.getBeanClass() ),
				javaTypeRegistry.getDescriptor( domainTypeToMatch ),
				javaTypeRegistry.getDescriptor( relationalType)
		);
	}
}
