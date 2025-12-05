/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.convert.internal;

import org.hibernate.boot.spi.ClassmateContext;
import org.hibernate.boot.model.convert.spi.AutoApplicableConverterDescriptor;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.convert.spi.JpaAttributeConverterCreationContext;
import org.hibernate.type.descriptor.converter.internal.AttributeConverterBean;
import org.hibernate.type.descriptor.converter.spi.JpaAttributeConverter;
import org.hibernate.resource.beans.spi.ManagedBean;

import com.fasterxml.classmate.ResolvedType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import static org.hibernate.boot.model.convert.internal.ConverterHelper.resolveConverterClassParamTypes;

/**
 * @author Steve Ebersole
 */
abstract class AbstractConverterDescriptor<X,Y> implements ConverterDescriptor<X,Y> {
	private final Class<? extends AttributeConverter<X,Y>> converterClass;

	private final ResolvedType domainType;
	private final ResolvedType jdbcType;

	private final AutoApplicableConverterDescriptor autoApplicableDescriptor;

	AbstractConverterDescriptor(
			Class<? extends AttributeConverter<X, Y>> converterClass,
			Boolean forceAutoApply,
			ClassmateContext classmateContext) {
		this.converterClass = converterClass;
		final var converterParamTypes = resolveConverterClassParamTypes( converterClass, classmateContext );
		domainType = converterParamTypes.get( 0 );
		jdbcType = converterParamTypes.get( 1 );
		autoApplicableDescriptor = resolveAutoApplicableDescriptor( converterClass, forceAutoApply );
	}

	private AutoApplicableConverterDescriptor resolveAutoApplicableDescriptor(
			Class<? extends AttributeConverter<?,?>> converterClass,
			Boolean forceAutoApply) {
		return isAutoApply( converterClass, forceAutoApply )
				? new AutoApplicableConverterDescriptorStandardImpl( this )
				: AutoApplicableConverterDescriptorBypassedImpl.INSTANCE;
	}

	private static boolean isAutoApply(Class<? extends AttributeConverter<?, ?>> converterClass, Boolean forceAutoApply) {
		if ( forceAutoApply != null ) {
			// if the caller explicitly specified whether to auto-apply, honor that
			return forceAutoApply;
		}
		else {
			// otherwise, look at the converter's @Converter annotation
			final Converter annotation = converterClass.getAnnotation( Converter.class );
			return annotation != null && annotation.autoApply();
		}
	}

	@Override
	public Class<? extends AttributeConverter<X,Y>> getAttributeConverterClass() {
		return converterClass;
	}

	@Override
	public ResolvedType getDomainValueResolvedType() {
		return domainType;
	}

	@Override
	public ResolvedType getRelationalValueResolvedType() {
		return jdbcType;
	}

	@Override
	public AutoApplicableConverterDescriptor getAutoApplyDescriptor() {
		return autoApplicableDescriptor;
	}

	@Override
	public JpaAttributeConverter<X,Y> createJpaAttributeConverter(JpaAttributeConverterCreationContext context) {
		return new AttributeConverterBean<>(
				createManagedBean( context ),
				context.getJavaTypeRegistry().resolveDescriptor( converterClass ),
				getDomainClass(),
				getRelationalClass(),
				context
		);
	}

	@SuppressWarnings("unchecked")
	private Class<Y> getRelationalClass() {
		return (Class<Y>) getRelationalValueResolvedType().getErasedType();
	}

	@SuppressWarnings("unchecked")
	private Class<X> getDomainClass() {
		return (Class<X>) getDomainValueResolvedType().getErasedType();
	}

	protected abstract ManagedBean<? extends AttributeConverter<X,Y>>
	createManagedBean(JpaAttributeConverterCreationContext context);
}
