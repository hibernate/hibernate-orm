/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.lang.reflect.ParameterizedType;
import java.util.HashMap;

import org.hibernate.annotations.Parameter;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.resource.beans.internal.FallbackBeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.type.BasicType;
import org.hibernate.type.CustomType;
import org.hibernate.type.descriptor.converter.internal.JpaAttributeConverterImpl;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.internal.ConvertedBasicTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.UserType;

import jakarta.persistence.AttributeConverter;

import static org.hibernate.internal.util.GenericsHelper.extractClass;
import static org.hibernate.internal.util.GenericsHelper.extractParameterizedType;
import static org.hibernate.internal.util.collections.CollectionHelper.mapOfSize;

/**
 * @author Steve Ebersole
 */
public class AnnotationHelper {
	public static HashMap<String, String> extractParameterMap(Parameter[] parameters) {
		final HashMap<String,String> paramMap = mapOfSize( parameters.length );
		for ( int i = 0; i < parameters.length; i++ ) {
			paramMap.put( parameters[i].name(), parameters[i].value() );
		}
		return paramMap;
	}

	public static JdbcMapping resolveUserType(Class<UserType<?>> userTypeClass, MetadataBuildingContext context) {
		final BootstrapContext bootstrapContext = context.getBootstrapContext();
		final UserType<?> userType =
				context.getBuildingOptions().isAllowExtensionsInCdi()
						? bootstrapContext.getManagedBeanRegistry().getBean( userTypeClass ).getBeanInstance()
						: FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( userTypeClass );
		return new CustomType<>( userType, bootstrapContext.getTypeConfiguration() );
	}

	public static <X,Y> JdbcMapping resolveAttributeConverter(
			Class<? extends AttributeConverter<? extends X,? extends Y>> type,
			MetadataBuildingContext context) {
		final BootstrapContext bootstrapContext = context.getBootstrapContext();
		final TypeConfiguration typeConfiguration = bootstrapContext.getTypeConfiguration();

		final var bean = bootstrapContext.getManagedBeanRegistry().getBean( type );

		final ParameterizedType converterParameterizedType = extractParameterizedType( bean.getBeanClass() );
		final Class<?> domainJavaClass = extractClass( converterParameterizedType.getActualTypeArguments()[0] );
		final Class<?> relationalJavaClass = extractClass( converterParameterizedType.getActualTypeArguments()[1] );

		final JavaTypeRegistry registry = typeConfiguration.getJavaTypeRegistry();
		final JavaType<X> domainJtd = registry.resolveDescriptor( domainJavaClass );
		final JavaType<Y> relationalJtd = registry.resolveDescriptor( relationalJavaClass );

		final JpaAttributeConverterImpl<X,Y> valueConverter =
				new JpaAttributeConverterImpl<>(
						(ManagedBean<? extends AttributeConverter<X,Y>>) bean,
						registry.resolveDescriptor( bean.getBeanClass() ),
						domainJtd,
						relationalJtd
				);
		return new ConvertedBasicTypeImpl<>(
				ConverterDescriptor.TYPE_NAME_PREFIX
						+ valueConverter.getConverterJavaType().getTypeName(),
				String.format(
						"BasicType adapter for AttributeConverter<%s,%s>",
						domainJtd.getTypeName(),
						relationalJtd.getTypeName()
				),
				relationalJtd.getRecommendedJdbcType( typeConfiguration.getCurrentBaseSqlTypeIndicators() ),
				valueConverter
		);
	}

	public static BasicType<Object> resolveBasicType(Class<?> type, MetadataBuildingContext context) {
		final TypeConfiguration typeConfiguration = context.getBootstrapContext().getTypeConfiguration();
		final JavaType<Object> jtd = typeConfiguration.getJavaTypeRegistry().findDescriptor( type );
		if ( jtd != null ) {
			final JdbcType jdbcType = jtd.getRecommendedJdbcType(
					new JdbcTypeIndicators() {
						@Override
						public TypeConfiguration getTypeConfiguration() {
							return typeConfiguration;
						}

						@Override
						public int getPreferredSqlTypeCodeForBoolean() {
							return context.getPreferredSqlTypeCodeForBoolean();
						}

						@Override
						public int getPreferredSqlTypeCodeForDuration() {
							return context.getPreferredSqlTypeCodeForDuration();
						}

						@Override
						public int getPreferredSqlTypeCodeForUuid() {
							return context.getPreferredSqlTypeCodeForUuid();
						}

						@Override
						public int getPreferredSqlTypeCodeForInstant() {
							return context.getPreferredSqlTypeCodeForInstant();
						}

						@Override
						public int getPreferredSqlTypeCodeForArray() {
							return context.getPreferredSqlTypeCodeForArray();
						}

						@Override
						public Dialect getDialect() {
							return context.getMetadataCollector().getDatabase().getDialect();
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
		final TypeConfiguration typeConfiguration = context.getBootstrapContext().getTypeConfiguration();
		final JavaType<?> jtd = getJavaType( type, context, typeConfiguration );
		final JdbcType jdbcType = jtd.getRecommendedJdbcType( typeConfiguration.getCurrentBaseSqlTypeIndicators() );
		return typeConfiguration.getBasicTypeRegistry().resolve( jtd, jdbcType );
	}

	private static JavaType<?> getJavaType(
			Class<JavaType<?>> javaTypeClass,
			MetadataBuildingContext context,
			TypeConfiguration typeConfiguration) {
		final JavaType<?> registeredJtd = typeConfiguration.getJavaTypeRegistry().findDescriptor( javaTypeClass );
		if ( registeredJtd != null ) {
			return registeredJtd;
		}
		else if ( !context.getBuildingOptions().isAllowExtensionsInCdi() ) {
			return FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( javaTypeClass );
		}
		else {
			return context.getBootstrapContext().getManagedBeanRegistry()
					.getBean( javaTypeClass )
					.getBeanInstance();
		}
	}
}
