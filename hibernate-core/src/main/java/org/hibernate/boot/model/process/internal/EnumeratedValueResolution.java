/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.process.internal;

import java.util.Locale;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.BasicValue;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.ConvertedBasicType;
import org.hibernate.type.descriptor.converter.internal.NamedEnumValueConverter;
import org.hibernate.type.descriptor.converter.internal.OrdinalEnumValueConverter;
import org.hibernate.type.descriptor.converter.spi.EnumValueConverter;
import org.hibernate.type.descriptor.java.EnumJavaType;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.internal.ConvertedBasicTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.EnumType;

import static org.hibernate.type.SqlTypes.CHAR;
import static org.hibernate.type.SqlTypes.SMALLINT;
import static org.hibernate.type.SqlTypes.TINYINT;
import static org.hibernate.type.SqlTypes.VARCHAR;

/**
 * Resolution for {@linkplain Enum enum} mappings using {@link jakarta.persistence.Enumerated},
 * either implicitly or explicitly
 *
 * @author Steve Ebersole
 */
public class EnumeratedValueResolution<E extends Enum<E>,R> implements BasicValue.Resolution<E> {
	public static final String PREFIX = "enum::";

	private final EnumValueConverter<E,R> valueConverter;
	private final ConvertedBasicType<E> jdbcMapping;

	public EnumeratedValueResolution(
			JdbcType jdbcType,
			EnumValueConverter<E, R> valueConverter,
			MetadataBuildingContext context) {
		this.valueConverter = valueConverter;

		final String externalizableName = createName( valueConverter );
		this.jdbcMapping = new ConvertedBasicTypeImpl<>( externalizableName, jdbcType, valueConverter );

		// todo (enum) : register database objects if needed
	}

	private String createName(EnumValueConverter<E, R> valueConverter) {
		return String.format(
				Locale.ROOT,
				PREFIX + "%s::%s",
				valueConverter.getDomainJavaType().getJavaType().getName(),
				enumStyle( valueConverter ).name()
		);
	}

	private static EnumType enumStyle(EnumValueConverter<?,?> valueConverter) {
		if ( valueConverter instanceof NamedEnumValueConverter ) {
			return EnumType.STRING;
		}
		else if ( valueConverter instanceof OrdinalEnumValueConverter ) {
			return EnumType.ORDINAL;
		}
		throw new UnsupportedOperationException();
	}

	@Override
	public ConvertedBasicType<E> getJdbcMapping() {
		return jdbcMapping;
	}

	@Override
	public ConvertedBasicType<E> getLegacyResolvedBasicType() {
		return jdbcMapping;
	}

	@Override
	public JavaType<E> getDomainJavaType() {
		return jdbcMapping.getJavaTypeDescriptor();
	}

	@Override
	public JavaType<?> getRelationalJavaType() {
		return jdbcMapping.getJdbcJavaType();
	}

	@Override
	public JdbcType getJdbcType() {
		return jdbcMapping.getJdbcType();
	}

	@Override
	public EnumValueConverter<E,R> getValueConverter() {
		return valueConverter;
	}

	@Override
	public MutabilityPlan<E> getMutabilityPlan() {
		return ImmutableMutabilityPlan.instance();
	}

	public static <E extends Enum<E>> EnumeratedValueResolution<E,?> fromName(
			String name,
			JdbcTypeIndicators jdbcTypeIndicators,
			MetadataBuildingContext context) {
		assert name != null;
		assert name.startsWith( PREFIX );

		final String[] parts = StringHelper.split( "::", name );
		assert parts.length == 3;
		assert "enum".equals( parts[0] );

		final TypeConfiguration typeConfiguration = context.getBootstrapContext().getTypeConfiguration();
		final JavaTypeRegistry javaTypeRegistry = typeConfiguration.getJavaTypeRegistry();
		final JdbcTypeRegistry jdbcTypeRegistry = typeConfiguration.getJdbcTypeRegistry();

		final Class<E> enumClass = resolveEnumClass( parts[1], context.getBootstrapContext() );
		final jakarta.persistence.EnumType style = jakarta.persistence.EnumType.valueOf( parts[ 2 ] );

		//noinspection unchecked,rawtypes
		final EnumJavaType<E> enumJavaType = (EnumJavaType) javaTypeRegistry.getDescriptor( enumClass );
		final JdbcType jdbcType;
		final EnumValueConverter<E,?> converter;

		if ( style == EnumType.ORDINAL ) {
			jdbcType = jdbcTypeRegistry.getDescriptor( enumJavaType.hasManyValues() ? SMALLINT : TINYINT );

			final JavaType<Integer> jdbcJavaType = jdbcType.getJdbcRecommendedJavaTypeMapping(
					jdbcTypeIndicators.getColumnPrecision(),
					jdbcTypeIndicators.getColumnScale(),
					typeConfiguration
			);
			converter = new OrdinalEnumValueConverter<>( enumJavaType, jdbcType, jdbcJavaType );
		}
		else if ( style == EnumType.STRING ) {
			jdbcType = jdbcTypeRegistry.getDescriptor( jdbcTypeIndicators.getColumnLength() == 1 ? CHAR : VARCHAR );
			final JavaType<String> jdbcJavaType = jdbcType.getJdbcRecommendedJavaTypeMapping(
					jdbcTypeIndicators.getColumnPrecision(),
					jdbcTypeIndicators.getColumnScale(),
					typeConfiguration
			);
			converter = new NamedEnumValueConverter<>( enumJavaType, jdbcType, jdbcJavaType );
		}
		else {
			throw new IllegalArgumentException( );
		}

		return new EnumeratedValueResolution<>( jdbcType, converter, context );
	}

	private static <E extends Enum<E>> Class<E> resolveEnumClass(String enumClassName, BootstrapContext bootstrapContext) {
		final ServiceRegistry serviceRegistry = bootstrapContext.getServiceRegistry();
		final ClassLoaderService classLoaderService = serviceRegistry.getService( ClassLoaderService.class );

		return classLoaderService.classForName( enumClassName );
	}
}
