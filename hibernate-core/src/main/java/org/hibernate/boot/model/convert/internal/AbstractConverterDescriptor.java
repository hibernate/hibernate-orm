/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.convert.internal;

import java.util.List;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.hibernate.AnnotationException;
import org.hibernate.boot.internal.ClassmateContext;
import org.hibernate.boot.model.convert.spi.AutoApplicableConverterDescriptor;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.convert.spi.JpaAttributeConverterCreationContext;
import org.hibernate.metamodel.model.convert.internal.JpaAttributeConverterImpl;
import org.hibernate.metamodel.model.convert.spi.JpaAttributeConverter;
import org.hibernate.resource.beans.spi.ManagedBean;

import com.fasterxml.classmate.ResolvedType;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractConverterDescriptor implements ConverterDescriptor {
	private final Class<? extends AttributeConverter> converterClass;

	private final ResolvedType domainType;
	private final ResolvedType jdbcType;

	private final AutoApplicableConverterDescriptor autoApplicableDescriptor;

	@SuppressWarnings("WeakerAccess")
	public AbstractConverterDescriptor(
			Class<? extends AttributeConverter> converterClass,
			Boolean forceAutoApply,
			ClassmateContext classmateContext) {
		this.converterClass = converterClass;

		final ResolvedType converterType = classmateContext.getTypeResolver().resolve( converterClass );
		final List<ResolvedType> converterParamTypes = converterType.typeParametersFor( AttributeConverter.class );
		if ( converterParamTypes == null ) {
			throw new AnnotationException(
					"Could not extract type parameter information from AttributeConverter implementation ["
							+ converterClass.getName() + "]"
			);
		}
		else if ( converterParamTypes.size() != 2 ) {
			throw new AnnotationException(
					"Unexpected type parameter information for AttributeConverter implementation [" +
							converterClass.getName() + "]; expected 2 parameter types, but found " + converterParamTypes.size()
			);
		}

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
	public Class<? extends AttributeConverter> getAttributeConverterClass() {
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
	@SuppressWarnings("unchecked")
	public JpaAttributeConverter createJpaAttributeConverter(JpaAttributeConverterCreationContext context) {
		return new JpaAttributeConverterImpl(
				createManagedBean( context ),
				context.getJavaTypeDescriptorRegistry().getDescriptor( getAttributeConverterClass() ),
				context.getJavaTypeDescriptorRegistry().getDescriptor( getDomainValueResolvedType().getErasedType() ),
				context.getJavaTypeDescriptorRegistry().getDescriptor( getRelationalValueResolvedType().getErasedType() )
		);
	}

	protected abstract ManagedBean<? extends AttributeConverter> createManagedBean(JpaAttributeConverterCreationContext context);
}
