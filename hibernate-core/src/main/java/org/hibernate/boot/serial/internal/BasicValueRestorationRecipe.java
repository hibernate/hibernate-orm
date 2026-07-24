/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.serial.internal;

import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Properties;

import org.hibernate.annotations.SoftDeleteType;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.boot.mapping.internal.materialize.BasicValueResolutionBuilder;
import org.hibernate.boot.mapping.internal.materialize.BasicValueResolutionDetails;
import org.hibernate.boot.mapping.internal.sources.BasicValueSource;

import jakarta.persistence.EnumType;
import jakarta.persistence.TemporalType;

/// Immutable declarative input for restoring a [org.hibernate.mapping.BasicValue.Resolution].
///
/// @since 9.0
/// @author Steve Ebersole
public record BasicValueRestorationRecipe(
		BasicValueSource source,
		BasicValueResolutionBuilder.BasicValueRole role,
		String ownerName,
		String propertyName,
		boolean softDelete,
		SoftDeleteType softDeleteStrategy,
		Properties typeParameters,
		String explicitTypeName,
		AttributeConverterRestorationRecipe converter,
		TimeZoneStorageType timeZoneStorageType,
		EnumType enumerationStyle,
		TemporalType temporalPrecision,
		Integer jdbcTypeCode,
		String resolvedJavaTypeName,
		String explicitJavaTypeDescriptorClassName,
		String explicitJdbcTypeDescriptorClassName,
		String explicitMutabilityPlanClassName,
		boolean attributeImmutable,
		String attributeMutabilityPlanClassName) implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	public BasicValueRestorationRecipe {
		if ( typeParameters != null ) {
			final Properties copy = new Properties();
			copy.putAll( typeParameters );
			typeParameters = copy;
		}
	}

	public static BasicValueRestorationRecipe from(BasicValueResolutionDetails details) {
		final var converter = details.getAttributeConverterDescriptor();
		final var resolvedJavaType = details.getResolvedJavaType();
		if ( resolvedJavaType != null && !( resolvedJavaType instanceof Class<?> ) && details.source().type() == null ) {
			throw unsupported( details, resolvedJavaType.getClass() );
		}
		return new BasicValueRestorationRecipe(
				details.source(),
				details.role(),
				details.getOwnerName(),
				details.getPropertyName(),
				details.isSoftDelete(),
				details.getSoftDeleteStrategy(),
				details.getTypeParameters(),
				details.getExplicitTypeName(),
				converter == null ? null : AttributeConverterRestorationRecipe.from( converter, details ),
				details.getTimeZoneStorageType(),
				details.getEnumerationStyle(),
				details.getTemporalPrecision(),
				details.getConfiguredJdbcTypeCode(),
				resolvedJavaType instanceof Class<?> javaClass ? javaClass.getName() : null,
				descriptorClassName( details, details.explicitJavaType() ),
				descriptorClassName( details, details.explicitJdbcType() ),
				descriptorClassName( details, details.explicitMutabilityPlan() ),
				details.attributeImmutable(),
				details.attributeMutabilityPlanClass() == null
						? null
						: details.attributeMutabilityPlanClass().getName()
		);
	}

	private static String descriptorClassName(BasicValueResolutionDetails details, Object descriptor) {
		if ( descriptor == null ) {
			return null;
		}
		final Class<?> descriptorClass = descriptor.getClass();
		if ( descriptorClass.isAnonymousClass()
				|| descriptorClass.isLocalClass()
				|| descriptorClass.isSynthetic()
				|| descriptorClass.isHidden()
				|| Proxy.isProxyClass( descriptorClass )
				|| descriptorClass.isMemberClass() && !Modifier.isStatic( descriptorClass.getModifiers() ) ) {
			throw unsupported( details, descriptorClass );
		}
		return descriptorClass.getName();
	}

	static IllegalArgumentException unsupported(BasicValueResolutionDetails details, Class<?> descriptorClass) {
		return new IllegalArgumentException(
				"BasicValue resolution for '" + details.getOwnerName() + "." + details.getPropertyName()
						+ "' contains unsupported instance-only descriptor " + descriptorClass.getName()
		);
	}
}
