/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.process.internal;

import java.util.function.Function;
import jakarta.persistence.TemporalType;

import org.hibernate.TimeZoneStorageStrategy;
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
@SuppressWarnings("rawtypes")
public class VersionResolution<E> implements BasicValue.Resolution<E> {

	// todo (6.0) : support explicit JTD?
	// todo (6.0) : support explicit STD?

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static <E> VersionResolution<E> from(
			Function<TypeConfiguration, java.lang.reflect.Type> implicitJavaTypeAccess,
			TimeZoneStorageType timeZoneStorageType,
			@SuppressWarnings("unused") MetadataBuildingContext context) {

		// todo (6.0) : add support for Dialect-specific interpretation?

		final TypeConfiguration typeConfiguration = context.getBootstrapContext().getTypeConfiguration();
		final java.lang.reflect.Type implicitJavaType = implicitJavaTypeAccess.apply( typeConfiguration );
		final JavaType registered = typeConfiguration.getJavaTypeRegistry().resolveDescriptor( implicitJavaType );
		final BasicJavaType jtd = (BasicJavaType) registered;

		final JdbcType recommendedJdbcType = jtd.getRecommendedJdbcType(
				new JdbcTypeIndicators() {
					@Override
					public TypeConfiguration getTypeConfiguration() {
						return typeConfiguration;
					}

					@Override
					public TemporalType getTemporalPrecision() {
						// if it is a temporal version, it needs to be a TIMESTAMP
						return TemporalType.TIMESTAMP;
					}

					@Override
					public boolean isPreferJavaTimeJdbcTypesEnabled() {
						return context.isPreferJavaTimeJdbcTypesEnabled();
					}

					@Override
					public boolean isPreferNativeEnumTypesEnabled() {
						return context.isPreferNativeEnumTypesEnabled();
					}

					@Override
					public TimeZoneStorageStrategy getDefaultTimeZoneStorageStrategy() {
						return BasicValue.timeZoneStorageStrategy( timeZoneStorageType, context );
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

		final BasicType<?> basicType = typeConfiguration.getBasicTypeRegistry().resolve( jtd, recommendedJdbcType );
		final BasicType legacyType = typeConfiguration.getBasicTypeRegistry().getRegisteredType( jtd.getJavaType() );

		assert legacyType.getJdbcType().getDefaultSqlTypeCode() == recommendedJdbcType.getDefaultSqlTypeCode();

		return new VersionResolution<>( jtd, recommendedJdbcType, basicType, legacyType );
	}

	private final JavaType javaType;
	private final JdbcType jdbcType;

	private final JdbcMapping jdbcMapping;
	private final BasicType legacyType;

	public VersionResolution(
			JavaType javaType,
			JdbcType jdbcType,
			JdbcMapping jdbcMapping,
			BasicType legacyType) {
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
	@SuppressWarnings("unchecked")
	public BasicType getLegacyResolvedBasicType() {
		return legacyType;
	}

	@Override
	@SuppressWarnings("unchecked")
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
	public BasicValueConverter<E,E> getValueConverter() {
		return legacyType.getValueConverter();
	}

	@Override
	public MutabilityPlan<E> getMutabilityPlan() {
		return ImmutableMutabilityPlan.instance();
	}
}
