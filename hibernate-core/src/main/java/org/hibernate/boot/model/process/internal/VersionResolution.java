/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.process.internal;

import jakarta.annotation.Nonnull;
import jakarta.persistence.TemporalType;

import org.hibernate.type.TimeZoneStorageStrategy;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.BasicValue;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class VersionResolution<E> implements BasicValue.Resolution<E> {

	public static VersionResolution<?> from(
			BasicValue basicValue,
			TimeZoneStorageType timeZoneStorageType,
			MetadataBuildingContext context) {
		final var typeConfiguration = context.getTypeConfiguration();
		final var implicitJavaType = basicValue.impliedJavaType( typeConfiguration );
		final var registered = typeConfiguration.getJavaTypeRegistry().resolveDescriptor( implicitJavaType );
		return resolve( timeZoneStorageType, context, (BasicJavaType<?>) registered );
	}

	private static <E> VersionResolution<E> resolve(
			TimeZoneStorageType timeZoneStorageType,
			MetadataBuildingContext context,
			BasicJavaType<E> basicJavaType) {
		final var typeConfiguration = context.getTypeConfiguration();
		final var mappingPreferences = context.getBuildingPlan().getMappingPreferences();
		final var recommendedJdbcType = basicJavaType.getRecommendedJdbcType(
				new JdbcTypeIndicators() {
					@Override
					@Nonnull
					public TypeConfiguration getTypeConfiguration() {
						return typeConfiguration;
					}

					@Override @SuppressWarnings("deprecation")
					public TemporalType getTemporalPrecision() {
						// if it is a temporal version, it needs to be a TIMESTAMP
						return TemporalType.TIMESTAMP;
					}

					@Override
					public boolean isPreferJavaTimeJdbcTypesEnabled() {
						return mappingPreferences.isPreferJavaTimeJdbcTypesEnabled();
					}

					@Override
					public boolean isPreferNativeEnumTypesEnabled() {
						return mappingPreferences.isPreferNativeEnumTypesEnabled();
					}

					@Override
					@Nonnull
					public TimeZoneStorageStrategy getDefaultTimeZoneStorageStrategy() {
						return BasicValue.timeZoneStorageStrategy( timeZoneStorageType, context );
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
						return context.getMetadataCollector().getDatabase().getDialect();
					}
				}
		);

		final var basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();
		final var basicType = basicTypeRegistry.resolve( basicJavaType, recommendedJdbcType );
		final var legacyType = basicTypeRegistry.getRegisteredType( basicJavaType.getJavaTypeClass() );
		assert legacyType.getJdbcType().getDefaultSqlTypeCode() == recommendedJdbcType.getDefaultSqlTypeCode();

		return new VersionResolution<>( basicJavaType, recommendedJdbcType, basicType, legacyType );
	}

	private final JavaType<E> javaType;
	private final JdbcType jdbcType;

	private final JdbcMapping jdbcMapping;
	private final BasicType<E> legacyType;

	public VersionResolution(
			JavaType<E> javaType,
			JdbcType jdbcType,
			JdbcMapping jdbcMapping,
			BasicType<E> legacyType) {
		this.javaType = javaType;
		this.jdbcType = jdbcType;
		this.jdbcMapping = jdbcMapping;
		this.legacyType = legacyType;
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return jdbcMapping;
	}

	@Override
	public BasicType<E> getLegacyResolvedBasicType() {
		return legacyType;
	}

	@Override
	public JavaType<E> getDomainJavaType() {
		return javaType;
	}

	@Override
	public JavaType<?> getRelationalJavaType() {
		return javaType;
	}

	@Override
	public JdbcType getJdbcType() {
		return jdbcType;
	}

	@Override
	public BasicValueConverter<E,?> getValueConverter() {
		return legacyType.getValueConverter();
	}

	@Override
	public MutabilityPlan<E> getMutabilityPlan() {
		return ImmutableMutabilityPlan.instance();
	}
}
