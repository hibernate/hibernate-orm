/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.convert.internal;

import java.util.List;

import org.hibernate.boot.internal.ClassmateContext;
import org.hibernate.boot.model.convert.spi.AutoApplicableConverterDescriptor;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.convert.spi.JpaAttributeConverterCreationContext;
import org.hibernate.type.descriptor.converter.internal.JpaAttributeConverterImpl;
import org.hibernate.type.descriptor.converter.spi.JpaAttributeConverter;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.type.descriptor.java.JavaType;

import com.fasterxml.classmate.ResolvedType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import static org.hibernate.boot.model.convert.internal.ConverterHelper.resolveConverterClassParamTypes;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractConverterDescriptor implements ConverterDescriptor {
	private final Class<? extends AttributeConverter<?,?>> converterClass;

	private final ResolvedType domainType;
	private final ResolvedType jdbcType;

	private final AutoApplicableConverterDescriptor autoApplicableDescriptor;

	public AbstractConverterDescriptor(
			Class<? extends AttributeConverter<?,?>> converterClass,
			Boolean forceAutoApply,
			ClassmateContext classmateContext) {
		this.converterClass = converterClass;

		final List<ResolvedType> converterParamTypes = resolveConverterClassParamTypes( converterClass, classmateContext );

		this.domainType = converterParamTypes.get( 0 );
		this.jdbcType = converterParamTypes.get( 1 );

		this.autoApplicableDescriptor = resolveAutoApplicableDescriptor( converterClass, forceAutoApply );
	}

	private AutoApplicableConverterDescriptor resolveAutoApplicableDescriptor(
			Class<? extends AttributeConverter> converterClass,
			Boolean forceAutoApply) {
		final boolean autoApply;

		if ( forceAutoApply != null ) {
			// if the caller explicitly specified whether to auto-apply, honor that
			autoApply = forceAutoApply;
		}
		else {
			// otherwise, look at the converter's @Converter annotation
			final Converter annotation = converterClass.getAnnotation( Converter.class );
			autoApply = annotation != null && annotation.autoApply();
		}

		return autoApply
				? new AutoApplicableConverterDescriptorStandardImpl( this )
				: AutoApplicableConverterDescriptorBypassedImpl.INSTANCE;
	}

	@Override
	public Class<? extends AttributeConverter<?,?>> getAttributeConverterClass() {
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
	public JpaAttributeConverter<?,?> createJpaAttributeConverter(JpaAttributeConverterCreationContext context) {
		final JavaType<?> converterJtd = context
				.getJavaTypeRegistry()
				.getDescriptor( getAttributeConverterClass() );

		final Class<?> domainJavaType = getDomainValueResolvedType().getErasedType();
		final Class<?> jdbcJavaType = getRelationalValueResolvedType().getErasedType();

		return new JpaAttributeConverterImpl(
				createManagedBean( context ),
				converterJtd,
				domainJavaType,
				jdbcJavaType,
				context
		);
	}

	protected abstract ManagedBean<? extends AttributeConverter<?, ?>> createManagedBean(JpaAttributeConverterCreationContext context);
}
