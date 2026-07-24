/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.serial.internal;

import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Objects;

import jakarta.persistence.AttributeConverter;

import org.hibernate.boot.mapping.internal.context.MappingResolutionServices;
import org.hibernate.boot.mapping.internal.materialize.BasicValueResolutionDetails;
import org.hibernate.boot.model.convert.internal.ConverterDescriptors;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.convert.spi.RegisteredConversion;

import static org.hibernate.internal.util.GenericsHelper.typeArguments;

/// Declarative description of the converter selected for a basic value.
///
/// Selection concerns such as auto-apply and overrideability are intentionally
/// not retained. The converter has already been selected when this recipe is
/// created; restoration needs only the managed converter class and any effective
/// domain-type override used to build its runtime converter.
///
/// @since 9.0
/// @author Steve Ebersole
public record AttributeConverterRestorationRecipe(
		String converterClassName,
		String explicitDomainTypeName) implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	static AttributeConverterRestorationRecipe from(
			ConverterDescriptor<?, ?> descriptor,
			BasicValueResolutionDetails details) {
		if ( ConverterDescriptors.usesProvidedInstance( descriptor )
				|| !ConverterDescriptors.usesManagedBeanRegistry( descriptor ) ) {
			throw BasicValueRestorationRecipe.unsupported( details, descriptor.getClass() );
		}

		final Class<? extends AttributeConverter<?, ?>> converterClass =
				descriptor.getAttributeConverterClass();
		final Type[] declaredTypes = typeArguments( AttributeConverter.class, converterClass );
		if ( !Objects.equals( descriptor.getRelationalValueResolvedType(), declaredTypes[1] ) ) {
			throw BasicValueRestorationRecipe.unsupported( details, descriptor.getClass() );
		}

		final Type effectiveDomainType = descriptor.getDomainValueResolvedType();
		final String explicitDomainTypeName;
		if ( Objects.equals( effectiveDomainType, declaredTypes[0] ) ) {
			explicitDomainTypeName = null;
		}
		else if ( effectiveDomainType instanceof Class<?> domainClass ) {
			explicitDomainTypeName = domainClass.getName();
		}
		else {
			throw BasicValueRestorationRecipe.unsupported( details, descriptor.getClass() );
		}

		return new AttributeConverterRestorationRecipe(
				converterClass.getName(),
				explicitDomainTypeName
		);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public ConverterDescriptor<?, ?> resolve(MappingResolutionServices services) {
		final Class converterClass = resolveClass(
				converterClassName,
				"attribute converter",
				services
		);
		if ( explicitDomainTypeName == null ) {
			return ConverterDescriptors.of( converterClass );
		}
		final Class<?> explicitDomainType = resolveClass(
				explicitDomainTypeName,
				"converter domain type",
				services
		);
		return new RegisteredConversion( explicitDomainType, converterClass, false )
				.getConverterDescriptor();
	}

	private static Class<?> resolveClass(
			String className,
			String role,
			MappingResolutionServices services) {
		try {
			return services.getClassLoaderService().classForTypeName( className );
		}
		catch (RuntimeException e) {
			throw new IllegalStateException(
					"Could not resolve archived " + role + " class '" + className + "'",
					e
			);
		}
	}
}
