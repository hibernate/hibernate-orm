/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.convert.spi;

import java.util.List;
import java.util.Objects;

import org.hibernate.boot.model.convert.internal.AutoApplicableConverterDescriptorBypassedImpl;
import org.hibernate.boot.model.convert.internal.AutoApplicableConverterDescriptorStandardImpl;
import org.hibernate.boot.model.convert.internal.ConverterHelper;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.type.descriptor.converter.internal.JpaAttributeConverterImpl;
import org.hibernate.type.descriptor.converter.spi.JpaAttributeConverter;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import com.fasterxml.classmate.ResolvedType;
import jakarta.persistence.AttributeConverter;

/**
 * A registered conversion.
 *
 * @see org.hibernate.annotations.ConverterRegistration
 *
 * @author Steve Ebersole
 */
public class RegisteredConversion {
	private final Class<?> explicitDomainType;
	private final Class<? extends AttributeConverter<?,?>> converterType;
	private final boolean autoApply;

	private final ConverterDescriptor converterDescriptor;

	public RegisteredConversion(
			Class<?> explicitDomainType,
			Class<? extends AttributeConverter<?,?>> converterType,
			boolean autoApply,
			MetadataBuildingContext context) {
		assert converterType != null;

		this.explicitDomainType = explicitDomainType;
		this.converterType = converterType;
		this.autoApply = autoApply;

		this.converterDescriptor = determineConverterDescriptor( explicitDomainType, converterType, autoApply, context );
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		RegisteredConversion that = (RegisteredConversion) o;
		return autoApply == that.autoApply
				&& Objects.equals( explicitDomainType, that.explicitDomainType )
				&& converterType.equals( that.converterType );
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
		final List<ResolvedType> resolvedParamTypes = ConverterHelper.resolveConverterClassParamTypes(
				converterType,
				context.getBootstrapContext().getClassmateContext()
		);
		final ResolvedType relationalType = resolvedParamTypes.get( 1 );
		final ResolvedType domainTypeToMatch;
		if ( !void.class.equals( explicitDomainType ) ) {
			domainTypeToMatch = context.getBootstrapContext().getClassmateContext().getTypeResolver().resolve( explicitDomainType );
		}
		else {
			domainTypeToMatch = resolvedParamTypes.get( 0 );
		}

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
		private final boolean autoApply;

		private final AutoApplicableConverterDescriptor autoApplyDescriptor;

		public ConverterDescriptorImpl(
				Class<? extends AttributeConverter<?, ?>> converterType,
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

		@SuppressWarnings("unchecked")
		@Override
		public JpaAttributeConverter<?, ?> createJpaAttributeConverter(JpaAttributeConverterCreationContext context) {
			final ManagedBean<? extends AttributeConverter<?, ?>> converterBean =
					context.getManagedBeanRegistry().getBean( converterType );

			final TypeConfiguration typeConfiguration = context.getTypeConfiguration();
			final JavaTypeRegistry javaTypeRegistry = typeConfiguration.getJavaTypeRegistry();
			javaTypeRegistry.resolveDescriptor( domainTypeToMatch.getErasedType() );

			//noinspection rawtypes
			return new JpaAttributeConverterImpl(
					converterBean,
					javaTypeRegistry.getDescriptor(  converterBean.getBeanClass() ),
					javaTypeRegistry.resolveDescriptor( domainTypeToMatch.getErasedType() ),
					javaTypeRegistry.resolveDescriptor( relationalType.getErasedType() )
			);
		}
	}

}
