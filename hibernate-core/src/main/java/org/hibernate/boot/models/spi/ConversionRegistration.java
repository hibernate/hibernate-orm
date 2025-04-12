/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.spi;

import com.fasterxml.classmate.ResolvedType;
import jakarta.persistence.AttributeConverter;
import org.hibernate.boot.model.convert.internal.AutoApplicableConverterDescriptorBypassedImpl;
import org.hibernate.boot.model.convert.internal.AutoApplicableConverterDescriptorStandardImpl;
import org.hibernate.boot.model.convert.internal.ConverterHelper;
import org.hibernate.boot.model.convert.spi.AutoApplicableConverterDescriptor;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.convert.spi.JpaAttributeConverterCreationContext;
import org.hibernate.boot.model.convert.spi.RegisteredConversion;
import org.hibernate.boot.spi.ClassmateContext;
import org.hibernate.models.spi.AnnotationDescriptor;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.type.descriptor.converter.internal.JpaAttributeConverterImpl;
import org.hibernate.type.descriptor.converter.spi.JpaAttributeConverter;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Objects;

/**
 * A registered conversion.
 *
 * @see org.hibernate.annotations.ConverterRegistration
 *
 * @apiNote Largely a copy of {@linkplain RegisteredConversion} to avoid early creation of
 * {@linkplain ConverterDescriptor}. Technically the conversion from ClassDetails to Class
 * should be fine since conversions are only valid for basic types which we will never enhance.
 *
 * @author Steve Ebersole
 */
public class ConversionRegistration {
	private final Class<?> explicitDomainType;
	private final Class<? extends AttributeConverter<?,?>> converterType;
	private final boolean autoApply;
	private final AnnotationDescriptor<? extends Annotation> source;

	public ConversionRegistration(
			Class<?> explicitDomainType,
			Class<? extends AttributeConverter<?,?>> converterType,
			boolean autoApply,
			AnnotationDescriptor<? extends Annotation> source) {
		assert converterType != null;

		this.explicitDomainType = explicitDomainType;
		this.converterType = converterType;
		this.autoApply = autoApply;
		this.source = source;
	}

	@Override
	public boolean equals(Object object) {
		if ( this == object ) {
			return true;
		}
		if ( !(object instanceof ConversionRegistration that) ) {
			return false;
		}
		return autoApply == that.autoApply
			&& Objects.equals( explicitDomainType, that.explicitDomainType )
			&& converterType.equals( that.converterType );
	}

	@Override
	public int hashCode() {
		return Objects.hash( explicitDomainType, converterType );
	}

	public Class<?> getExplicitDomainType() {
		return explicitDomainType;
	}

	public Class<? extends AttributeConverter<?,?>> getConverterType() {
		return converterType;
	}

	public boolean isAutoApply() {
		return autoApply;
	}

	public AnnotationDescriptor<? extends Annotation> getSource() {
		return source;
	}

	@Override
	public String toString() {
		return "ConversionRegistration( " + converterType.getName() + ", " + source.getAnnotationType().getSimpleName() + ", " + autoApply + ")";
	}

	public ConverterDescriptor<?,?> makeConverterDescriptor(ClassmateContext classmateContext) {
		final List<ResolvedType> resolvedParamTypes =
				ConverterHelper.resolveConverterClassParamTypes( converterType, classmateContext );
		final ResolvedType relationalType = resolvedParamTypes.get( 1 );
		final ResolvedType domainTypeToMatch =
				void.class.equals( explicitDomainType )
						? resolvedParamTypes.get( 0 )
						: classmateContext.getTypeResolver().resolve( explicitDomainType );
		return converterDescriptor( converterType, domainTypeToMatch, relationalType, autoApply );
	}

	static <X,Y> ConverterDescriptor<X,Y> converterDescriptor(
			Class<? extends AttributeConverter<? extends X, ? extends Y>> converterType,
			ResolvedType domainTypeToMatch, ResolvedType relationalType, boolean autoApply) {
		@SuppressWarnings("unchecked") // work around weird fussiness in wildcard capture
		final Class<? extends AttributeConverter<X, Y>> converterClass =
				(Class<? extends AttributeConverter<X, Y>>) converterType;
		return new ConverterDescriptorImpl<>( converterClass, domainTypeToMatch, relationalType, autoApply );
	}

	private static class ConverterDescriptorImpl<X,Y> implements ConverterDescriptor<X,Y> {
		private final Class<? extends AttributeConverter<X, Y>> converterType;
		private final ResolvedType domainTypeToMatch;
		private final ResolvedType relationalType;
		private final boolean autoApply;

		private final AutoApplicableConverterDescriptor autoApplyDescriptor;

		public ConverterDescriptorImpl(
				Class<? extends AttributeConverter<X, Y>> converterType,
				ResolvedType domainTypeToMatch,
				ResolvedType relationalType,
				boolean autoApply) {
			this.converterType = converterType;
			this.domainTypeToMatch = domainTypeToMatch;
			this.relationalType = relationalType;
			this.autoApply = autoApply;

			this.autoApplyDescriptor = autoApply
					? new AutoApplicableConverterDescriptorStandardImpl( this )
					: AutoApplicableConverterDescriptorBypassedImpl.INSTANCE;
		}

		@Override
		public Class<? extends AttributeConverter<X,Y>> getAttributeConverterClass() {
			return converterType;
		}

		@Override
		public ResolvedType getDomainValueResolvedType() {
			return domainTypeToMatch;
		}

		@Override
		public ResolvedType getRelationalValueResolvedType() {
			return relationalType;
		}

		@Override
		public AutoApplicableConverterDescriptor getAutoApplyDescriptor() {
			return autoApplyDescriptor;
		}

		@Override
		public JpaAttributeConverter<X, Y> createJpaAttributeConverter(JpaAttributeConverterCreationContext context) {
			final var converterBean = context.getManagedBeanRegistry().getBean( converterType );

			final JavaTypeRegistry javaTypeRegistry = context.getTypeConfiguration().getJavaTypeRegistry();
			javaTypeRegistry.resolveDescriptor( domainTypeToMatch.getErasedType() );

			return new JpaAttributeConverterImpl<>(
					(ManagedBean<? extends AttributeConverter<X, Y>>) converterBean,
					javaTypeRegistry.getDescriptor( converterBean.getBeanClass() ),
					javaTypeRegistry.resolveDescriptor( domainTypeToMatch.getErasedType() ),
					javaTypeRegistry.resolveDescriptor( relationalType.getErasedType() )
			);
		}
	}

}
