/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.process.internal;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

import jakarta.persistence.AttributeConverter;
import org.hibernate.annotations.Immutable;
import org.hibernate.boot.model.convert.internal.ConverterDescriptors;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.convert.spi.JpaAttributeConverterCreationContext;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.BasicValue;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.converter.internal.AttributeConverterMutabilityPlan;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.converter.spi.JpaAttributeConverter;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.internal.CustomMutabilityConvertedBasicTypeImpl;
import org.hibernate.type.internal.CustomMutabilityConvertedPrimitiveBasicTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class NamedConverterResolution<J> implements BasicValue.Resolution<J> {

	public static <T> NamedConverterResolution<T> from(
			ConverterDescriptor<T,?> converterDescriptor,
			Function<TypeConfiguration, BasicJavaType<?>> explicitJtdAccess,
			Function<TypeConfiguration, JdbcType> explicitStdAccess,
			Function<TypeConfiguration, MutabilityPlan<?>> explicitMutabilityPlanAccess,
			Type resolvedJavaType,
			JdbcTypeIndicators sqlTypeIndicators,
			JpaAttributeConverterCreationContext converterCreationContext,
			MetadataBuildingContext context) {
		return fromInternal(
				explicitJtdAccess,
				explicitStdAccess,
				explicitMutabilityPlanAccess,
				converter( converterCreationContext, converterDescriptor ),
				resolvedJavaType,
				sqlTypeIndicators,
				context
		);
	}

	public static <T> NamedConverterResolution<T> from(
			String name,
			Function<TypeConfiguration, BasicJavaType<?>> explicitJtdAccess,
			Function<TypeConfiguration, JdbcType> explicitStdAccess,
			Function<TypeConfiguration, MutabilityPlan<?>> explicitMutabilityPlanAccess,
			JdbcTypeIndicators sqlTypeIndicators,
			JpaAttributeConverterCreationContext converterCreationContext,
			MetadataBuildingContext context) {
		assert name.startsWith( ConverterDescriptor.TYPE_NAME_PREFIX );
		final String converterClassName = name.substring( ConverterDescriptor.TYPE_NAME_PREFIX.length() );

		final BootstrapContext bootstrapContext = context.getBootstrapContext();
		final Class<? extends AttributeConverter<T, ?>> converterClass =
				bootstrapContext.getClassLoaderService().classForName( converterClassName );
		final ConverterDescriptor<T,?> converterDescriptor =
				ConverterDescriptors.of( converterClass, bootstrapContext.getClassmateContext() );

		return fromInternal(
				explicitJtdAccess,
				explicitStdAccess,
				explicitMutabilityPlanAccess,
				converter( converterCreationContext, converterDescriptor ),
				null,
				sqlTypeIndicators,
				context
		);
	}

	private static <T, S> JpaAttributeConverter<T, S> converter(
			JpaAttributeConverterCreationContext converterCreationContext,
			ConverterDescriptor<T,S> converterDescriptor) {
		return converterDescriptor.createJpaAttributeConverter(converterCreationContext);
	}

	private static <T,S> NamedConverterResolution<T> fromInternal(
			Function<TypeConfiguration, BasicJavaType<?>> explicitJtdAccess,
			Function<TypeConfiguration, JdbcType> explicitStdAccess,
			Function<TypeConfiguration, MutabilityPlan<?>> explicitMutabilityPlanAccess,
			JpaAttributeConverter<T,S> converter,
			Type resolvedJavaType,
			JdbcTypeIndicators sqlTypeIndicators,
			MetadataBuildingContext context) {
		final TypeConfiguration typeConfiguration = context.getBootstrapContext().getTypeConfiguration();

		//noinspection unchecked
		final JavaType<T> explicitJtd =
				explicitJtdAccess != null
						? (JavaType<T>) explicitJtdAccess.apply( typeConfiguration )
						: null;

		final JavaType<T> domainJtd = explicitJtd != null
				? explicitJtd
				: converter.getDomainJavaType();

		final JdbcType explicitJdbcType = explicitStdAccess != null
				? explicitStdAccess.apply( typeConfiguration )
				: null;

		final JavaType<?> relationalJtd = converter.getRelationalJavaType();

		final JdbcType jdbcType = explicitJdbcType != null
				? explicitJdbcType
				: relationalJtd.getRecommendedJdbcType( sqlTypeIndicators );

		final MutabilityPlan<T> mutabilityPlan = determineMutabilityPlan(
				explicitMutabilityPlanAccess,
				typeConfiguration,
				converter,
				domainJtd
		);

		//noinspection unchecked
		final Class<T> primitiveClass =
				resolvedJavaType instanceof Class<?> clazz && clazz.isPrimitive()
						? (Class<T>) resolvedJavaType
						: null;

		return new NamedConverterResolution<>(
				domainJtd,
				relationalJtd,
				jdbcType,
				converter,
				mutabilityPlan,
				primitiveClass
		);
	}

	private static <T> MutabilityPlan<T> determineMutabilityPlan(
			Function<TypeConfiguration, MutabilityPlan<?>> explicitMutabilityPlanAccess,
			TypeConfiguration typeConfiguration,
			JpaAttributeConverter<T, ?> converter,
			JavaType<T> domainJtd) {
		//noinspection unchecked
		final MutabilityPlan<T> explicitMutabilityPlan =
				explicitMutabilityPlanAccess != null
						? (MutabilityPlan<T>) explicitMutabilityPlanAccess.apply( typeConfiguration )
						: null;
		if ( explicitMutabilityPlan != null ) {
			return explicitMutabilityPlan;
		}
		else if ( converter.getConverterJavaType().getJavaTypeClass().isAnnotationPresent( Immutable.class ) ) {
			return ImmutableMutabilityPlan.instance();
		}
		else if ( !domainJtd.getMutabilityPlan().isMutable()
				&& !isCollection( domainJtd.getJavaTypeClass() ) ) {
			// if the domain JavaType is immutable, use the immutability plan
			// 		- note : ignore this for collection-as-basic mappings.
			return ImmutableMutabilityPlan.instance();
		}
		else {
			return new AttributeConverterMutabilityPlan<>( converter, true );
		}
	}

	private static boolean isCollection(Class<?> javaType) {
		return Collection.class.isAssignableFrom( javaType )
			|| Map.class.isAssignableFrom( javaType );
	}


	private final JavaType<J> domainJtd;
	private final JavaType<?> relationalJtd;
	private final JdbcType jdbcType;

	private final JpaAttributeConverter<J,?> valueConverter;
	private final MutabilityPlan<J> mutabilityPlan;

	private final JdbcMapping jdbcMapping;

	private final BasicType<J> legacyResolvedType;

	private NamedConverterResolution(
			JavaType<J> domainJtd,
			JavaType<?> relationalJtd,
			JdbcType jdbcType,
			JpaAttributeConverter<J,?> valueConverter,
			MutabilityPlan<J> mutabilityPlan,
			Class<J> primitiveClass) {
		assert domainJtd != null;
		this.domainJtd = domainJtd;

		assert relationalJtd != null;
		this.relationalJtd = relationalJtd;

		assert jdbcType != null;
		this.jdbcType = jdbcType;

		assert valueConverter != null;
		this.valueConverter = valueConverter;

		assert mutabilityPlan != null;
		this.mutabilityPlan = mutabilityPlan;

		this.legacyResolvedType = legacyResolvedType(
				ConverterDescriptor.TYPE_NAME_PREFIX
						+ valueConverter.getConverterJavaType().getTypeName(),
				String.format(
						"BasicType adapter for AttributeConverter<%s,%s>",
						domainJtd.getTypeName(),
						relationalJtd.getTypeName()
				),
				jdbcType,
				valueConverter,
				primitiveClass,
				mutabilityPlan
		);
		this.jdbcMapping = legacyResolvedType;
	}

	private static <J> BasicType<J> legacyResolvedType(
			String name,
			String description,
			JdbcType jdbcType,
			BasicValueConverter<J, ?> converter,
			Class<J> primitiveClass,
			MutabilityPlan<J> mutabilityPlan) {
		if ( primitiveClass != null ) {
			assert primitiveClass.isPrimitive();
			return new CustomMutabilityConvertedPrimitiveBasicTypeImpl<>(
					name,
					description,
					jdbcType,
					converter,
					primitiveClass,
					mutabilityPlan
			);
		}
		else {
			return new CustomMutabilityConvertedBasicTypeImpl<>(
					name,
					description,
					jdbcType,
					converter,
					mutabilityPlan
			);
		}
	}

	@Override
	public BasicType<J> getLegacyResolvedBasicType() {
		return legacyResolvedType;
	}

	@Override
	public JavaType<J> getDomainJavaType() {
		return domainJtd;
	}

	@Override
	public JavaType<?> getRelationalJavaType() {
		return relationalJtd;
	}

	@Override
	public JdbcType getJdbcType() {
		return jdbcType;
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return jdbcMapping;
	}

	@Override
	public JpaAttributeConverter<J,?> getValueConverter() {
		return valueConverter;
	}

	@Override
	public MutabilityPlan<J> getMutabilityPlan() {
		return mutabilityPlan;
	}

	@Override
	public String toString() {
		return "NamedConverterResolution(" + valueConverter.getConverterBean().getBeanClass().getName() + ')';
	}

}
