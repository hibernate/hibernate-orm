/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import jakarta.annotation.Nonnull;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.resource.beans.internal.FallbackBeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.type.BasicType;
import org.hibernate.type.CustomType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.internal.ConvertedBasicTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.UserType;

import jakarta.persistence.AttributeConverter;

import static org.hibernate.type.descriptor.converter.internal.ConverterHelper.createJpaAttributeConverter;

/**
 * @author Steve Ebersole
 */
public class AnnotationHelper {

	public static JdbcMapping resolveUserType(Class<UserType<?>> userTypeClass, MetadataBuildingContext context) {
		final var userType =
				context.getBuildingPlan().isAllowExtensionsInCdi()
						? context.getManagedBeanRegistry().getBean( userTypeClass ).getBeanInstance()
						: FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( userTypeClass );
		return new CustomType<>( userType, context.getTypeConfiguration() );
	}

	public static <X,Y> JdbcMapping resolveAttributeConverter(
			Class<? extends AttributeConverter<? extends X,? extends Y>> type,
			MetadataBuildingContext context) {
		final var typeConfiguration = context.getTypeConfiguration();
		final var bean = context.getManagedBeanRegistry().getBean( type );
		@SuppressWarnings("unchecked")
		final var castBean = (ManagedBean<? extends AttributeConverter<X,Y>>) bean;
		final var registry = typeConfiguration.getJavaTypeRegistry();
		final var valueConverter = createJpaAttributeConverter( castBean, registry );
		return new ConvertedBasicTypeImpl<>(
				ConverterDescriptor.TYPE_NAME_PREFIX
						+ valueConverter.getConverterJavaType().getTypeName(),
				String.format(
						"BasicType adapter for AttributeConverter<%s,%s>",
						valueConverter.getDomainJavaType().getTypeName(),
						valueConverter.getRelationalJavaType().getTypeName()
				),
				registry.resolveDescriptor( valueConverter.getRelationalJavaType().getJavaTypeClass() )
						.getRecommendedJdbcType( typeConfiguration.getCurrentBaseSqlTypeIndicators() ),
				valueConverter
		);
	}

	public static BasicType<?> resolveBasicType(Class<?> type, MetadataBuildingContext context) {
		final var typeConfiguration = context.getTypeConfiguration();
		final var mappingPreferences = context.getBuildingPlan().getMappingPreferences();
		final var jtd = typeConfiguration.getJavaTypeRegistry().findDescriptor( type );
		if ( jtd != null ) {
			final JdbcType jdbcType = jtd.getRecommendedJdbcType(
					new JdbcTypeIndicators() {
						@Override
						@Nonnull
						public TypeConfiguration getTypeConfiguration() {
							return typeConfiguration;
						}

						@Override
						public int getPreferredSqlTypeCodeForBoolean() {
							return mappingPreferences.getPreferredSqlTypeCodeForBoolean();
						}

						@Override
						public int getPreferredSqlTypeCodeForDuration() {
							return mappingPreferences.getPreferredSqlTypeCodeForDuration();
						}

						@Override
						public int getPreferredSqlTypeCodeForUuid() {
							return mappingPreferences.getPreferredSqlTypeCodeForUuid();
						}

						@Override
						public int getPreferredSqlTypeCodeForInstant() {
							return mappingPreferences.getPreferredSqlTypeCodeForInstant();
						}

						@Override
						public int getPreferredSqlTypeCodeForArray() {
							return mappingPreferences.getPreferredSqlTypeCodeForArray();
						}

						@Override
						public Dialect getDialect() {
							return context.getServiceComponents().getJdbcServices().getDialect();
						}
					}
			);
			return typeConfiguration.getBasicTypeRegistry().resolve( jtd, jdbcType );
		}
		else {
			return null;
		}
	}

	public static JdbcMapping resolveJavaType(Class<JavaType<?>> type, MetadataBuildingContext context) {
		final var typeConfiguration = context.getTypeConfiguration();
		final var jtd = getJavaType( type, context, typeConfiguration );
		final var jdbcType = jtd.getRecommendedJdbcType( typeConfiguration.getCurrentBaseSqlTypeIndicators() );
		return typeConfiguration.getBasicTypeRegistry().resolve( jtd, jdbcType );
	}

	private static JavaType<?> getJavaType(
			Class<JavaType<?>> javaTypeClass,
			MetadataBuildingContext context,
			TypeConfiguration typeConfiguration) {
		final var registeredJtd =
				typeConfiguration.getJavaTypeRegistry()
						.findDescriptor( javaTypeClass );
		if ( registeredJtd != null ) {
			return registeredJtd;
		}
		else if ( !context.getBuildingPlan().isAllowExtensionsInCdi() ) {
			return FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( javaTypeClass );
		}
		else {
			return context.getManagedBeanRegistry()
					.getBean( javaTypeClass ).getBeanInstance();
		}
	}
}
