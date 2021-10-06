/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.process.internal;

import java.util.function.Function;
import jakarta.persistence.TemporalType;

import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.BasicValue;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.BasicJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptorIndicators;
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
			Function<TypeConfiguration, BasicJavaTypeDescriptor> explicitJtdAccess,
			Function<TypeConfiguration, JdbcTypeDescriptor> explicitStdAccess,
			TypeConfiguration typeConfiguration,
			@SuppressWarnings("unused") MetadataBuildingContext context) {

		// todo (6.0) : add support for Dialect-specific interpretation?

		final java.lang.reflect.Type implicitJavaType = implicitJavaTypeAccess.apply( typeConfiguration );
		final JavaTypeDescriptor registered = typeConfiguration.getJavaTypeDescriptorRegistry().resolveDescriptor( implicitJavaType );
		final BasicJavaTypeDescriptor jtd = (BasicJavaTypeDescriptor) registered;

		final JdbcTypeDescriptor recommendedJdbcType = jtd.getRecommendedJdbcType(
				new JdbcTypeDescriptorIndicators() {
					@Override
					public TypeConfiguration getTypeConfiguration() {
						return typeConfiguration;
					}

					@Override
					public TemporalType getTemporalPrecision() {
						// if it is a temporal version, it needs to be a TIMESTAMP
						return TemporalType.TIMESTAMP;
					}
				}
		);

		final BasicType<?> basicType = typeConfiguration.getBasicTypeRegistry().resolve( jtd, recommendedJdbcType );
		final BasicType legacyType = typeConfiguration.getBasicTypeRegistry().getRegisteredType( jtd.getJavaType() );

		assert legacyType.getJdbcTypeDescriptor().equals( recommendedJdbcType );

		return new VersionResolution<>( jtd, recommendedJdbcType, basicType, legacyType );
	}

	private final JavaTypeDescriptor jtd;
	private final JdbcTypeDescriptor jdbcTypeDescriptor;

	private final JdbcMapping jdbcMapping;
	private final BasicType legacyType;

	public VersionResolution(
			JavaTypeDescriptor javaTypeDescriptor,
			JdbcTypeDescriptor jdbcTypeDescriptor,
			JdbcMapping jdbcMapping,
			BasicType legacyType) {
		this.jtd = javaTypeDescriptor;
		this.jdbcTypeDescriptor = jdbcTypeDescriptor;
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
	public JavaTypeDescriptor<E> getDomainJavaDescriptor() {
		return jtd;
	}

	@Override
	public JavaTypeDescriptor<?> getRelationalJavaDescriptor() {
		return jtd;
	}

	@Override
	public JdbcTypeDescriptor getJdbcTypeDescriptor() {
		return jdbcTypeDescriptor;
	}

	@Override
	public BasicValueConverter<E,E> getValueConverter() {
		return null;
	}

	@Override
	public MutabilityPlan<E> getMutabilityPlan() {
		return ImmutableMutabilityPlan.instance();
	}
}
