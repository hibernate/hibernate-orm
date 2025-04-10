/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.convert.spi;

import java.util.List;
import java.util.Objects;

import org.hibernate.boot.model.convert.internal.AutoApplicableConverterDescriptorBypassedImpl;
import org.hibernate.boot.model.convert.internal.AutoApplicableConverterDescriptorStandardImpl;
import org.hibernate.boot.spi.ClassmateContext;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.type.descriptor.converter.internal.JpaAttributeConverterImpl;
import org.hibernate.type.descriptor.converter.spi.JpaAttributeConverter;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;

import com.fasterxml.classmate.ResolvedType;
import jakarta.persistence.AttributeConverter;

import static org.hibernate.boot.model.convert.internal.ConverterHelper.resolveConverterClassParamTypes;

/**
 * A registered conversion.
 *
 * @see org.hibernate.annotations.ConverterRegistration
 *
 * @author Steve Ebersole
 */
public record RegisteredConversion(
		Class<?> explicitDomainType,
		Class<? extends AttributeConverter<?,?>> converterType,
		boolean autoApply,
		ConverterDescriptor converterDescriptor) {

	public RegisteredConversion {
		assert converterType != null;
	}

	public RegisteredConversion(
			Class<?> explicitDomainType,
			Class<? extends AttributeConverter<?,?>> converterType,
			boolean autoApply,
			MetadataBuildingContext context) {
		this( explicitDomainType, converterType, autoApply,
				determineConverterDescriptor( explicitDomainType, converterType, autoApply, context ) );
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof RegisteredConversion that) ) {
			return false;
		}
		return this.autoApply == that.autoApply
			&& Objects.equals( this.explicitDomainType, that.explicitDomainType )
			&& Objects.equals( this.converterType, that.converterType );
	}

	@Override
	public int hashCode() {
		return Objects.hash( explicitDomainType, converterType );
	}

	private static ConverterDescriptor determineConverterDescriptor(
			Class<?> explicitDomainType,
			Class<? extends AttributeConverter<?, ?>> converterType,
			boolean autoApply,
			MetadataBuildingContext context) {
		final ClassmateContext classmateContext = context.getBootstrapContext().getClassmateContext();
		final List<ResolvedType> resolvedParamTypes = resolveConverterClassParamTypes( converterType, classmateContext );
		final ResolvedType relationalType = resolvedParamTypes.get( 1 );
		final ResolvedType domainTypeToMatch =
				void.class.equals( explicitDomainType )
						? resolvedParamTypes.get( 0 )
						: classmateContext.getTypeResolver().resolve( explicitDomainType );
		return new ConverterDescriptorImpl( converterType, domainTypeToMatch, relationalType, autoApply );
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

	public ConverterDescriptor getConverterDescriptor() {
		return converterDescriptor;
	}

	private static class ConverterDescriptorImpl implements ConverterDescriptor {
		private final Class<? extends AttributeConverter<?, ?>> converterType;
		private final ResolvedType domainTypeToMatch;
		private final ResolvedType relationalType;
		private final AutoApplicableConverterDescriptor autoApplyDescriptor;

		public ConverterDescriptorImpl(
				Class<? extends AttributeConverter<?, ?>> converterType,
				ResolvedType domainTypeToMatch,
				ResolvedType relationalType,
				boolean autoApply) {
			this.converterType = converterType;
			this.domainTypeToMatch = domainTypeToMatch;
			this.relationalType = relationalType;
			this.autoApplyDescriptor = autoApply
					? new AutoApplicableConverterDescriptorStandardImpl( this )
					: AutoApplicableConverterDescriptorBypassedImpl.INSTANCE;
		}

		@Override
		public Class<? extends AttributeConverter<?, ?>> getAttributeConverterClass() {
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
		public JpaAttributeConverter<?, ?> createJpaAttributeConverter(JpaAttributeConverterCreationContext context) {
			final ManagedBean<? extends AttributeConverter<?, ?>> converterBean =
					context.getManagedBeanRegistry().getBean( converterType );

			final JavaTypeRegistry javaTypeRegistry = context.getTypeConfiguration().getJavaTypeRegistry();
			javaTypeRegistry.resolveDescriptor( domainTypeToMatch.getErasedType() );

			//noinspection rawtypes, unchecked
			return new JpaAttributeConverterImpl(
					converterBean,
					javaTypeRegistry.getDescriptor(  converterBean.getBeanClass() ),
					javaTypeRegistry.resolveDescriptor( domainTypeToMatch.getErasedType() ),
					javaTypeRegistry.resolveDescriptor( relationalType.getErasedType() )
			);
		}
	}

}
