/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.process.internal;

import java.util.function.Function;
import javax.persistence.TemporalType;

import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.BasicValue;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.type.BasicType;
import org.hibernate.type.RowVersionType;
import org.hibernate.type.descriptor.java.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptorIndicators;
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
			Function<TypeConfiguration, BasicJavaDescriptor> explicitJtdAccess,
			Function<TypeConfiguration, SqlTypeDescriptor> explicitStdAccess,
			TypeConfiguration typeConfiguration,
			@SuppressWarnings("unused") MetadataBuildingContext context) {

		// todo (6.0) : add support for Dialect-specific interpretation?

		final java.lang.reflect.Type implicitJavaType = implicitJavaTypeAccess.apply( typeConfiguration );
		final JavaTypeDescriptor registered = typeConfiguration.getJavaTypeDescriptorRegistry().resolveDescriptor( implicitJavaType );

		if ( registered instanceof PrimitiveByteArrayTypeDescriptor ) {
			return new VersionResolution<>(
					RowVersionType.INSTANCE.getJavaTypeDescriptor(),
					RowVersionType.INSTANCE.getSqlTypeDescriptor(),
					RowVersionType.INSTANCE,
					RowVersionType.INSTANCE
			);
		}

		final BasicJavaDescriptor jtd = (BasicJavaDescriptor) registered;

		final SqlTypeDescriptor std = jtd.getJdbcRecommendedSqlType(
				new SqlTypeDescriptorIndicators() {
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

		final BasicType<?> basicType = typeConfiguration.getBasicTypeRegistry().resolve( jtd, std );
		final BasicType legacyType = typeConfiguration.getBasicTypeRegistry().getRegisteredType( jtd.getJavaType() );

		assert legacyType.getSqlTypeDescriptor().equals( std );

		return new VersionResolution<>( jtd, std, basicType, legacyType );
	}

	private final JavaTypeDescriptor jtd;
	private final SqlTypeDescriptor std;

	private final JdbcMapping jdbcMapping;
	private final BasicType legacyType;

	public VersionResolution(
			JavaTypeDescriptor javaTypeDescriptor,
			SqlTypeDescriptor sqlTypeDescriptor,
			JdbcMapping jdbcMapping,
			BasicType legacyType) {
		this.jtd = javaTypeDescriptor;
		this.std = sqlTypeDescriptor;
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
	public SqlTypeDescriptor getRelationalSqlTypeDescriptor() {
		return std;
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
