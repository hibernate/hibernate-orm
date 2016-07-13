/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi.basic;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.EnumType;
import javax.persistence.TemporalType;

import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.type.spi.descriptor.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.spi.descriptor.TypeDescriptorRegistryAccess;
import org.hibernate.type.spi.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.MutabilityPlan;
import org.hibernate.type.spi.descriptor.java.TemporalTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.SqlTypeDescriptor;

/**
 * Redesign of {@link org.hibernate.type.BasicTypeRegistry} based on idea of "composing"
 * a BasicType from JavaTypeDescriptor, SqlTypeDescriptor and AttributeConverter.
 *
 * @author Steve Ebersole
 */
public class BasicTypeRegistry {
	private final Map<RegistryKey,BasicType> registrations = new HashMap<>();

	private final TypeConfiguration typeConfiguration;
	private final TypeDescriptorRegistryAccess context;
	private final JdbcRecommendedSqlTypeMappingContext baseJdbcRecommendedSqlTypeMappingContext;

	public BasicTypeRegistry(TypeConfiguration typeConfiguration, TypeDescriptorRegistryAccess context) {
		this.typeConfiguration = typeConfiguration;
		this.context = context;
		this.baseJdbcRecommendedSqlTypeMappingContext = new JdbcRecommendedSqlTypeMappingContext() {
			@Override
			public boolean isNationalized() {
				return false;
			}

			@Override
			public boolean isLob() {
				return false;
			}

			@Override
			public EnumType getEnumeratedType() {
				return EnumType.STRING;
			}

			@Override
			public TypeDescriptorRegistryAccess getTypeDescriptorRegistryAccess() {
				return context;
			}
		};
	}

	public TypeDescriptorRegistryAccess getTypeDescriptorRegistryAccess() {
		return context;
	}

	public JdbcRecommendedSqlTypeMappingContext getBaseJdbcRecommendedSqlTypeMappingContext() {
		return baseJdbcRecommendedSqlTypeMappingContext;
	}

	@SuppressWarnings("unchecked")
	public <T> BasicType<T> resolveBasicType(
			BasicTypeParameters<T> parameters,
			JdbcRecommendedSqlTypeMappingContext jdbcTypeResolutionContext) {
		if ( parameters == null ) {
			throw new IllegalArgumentException( "BasicTypeParameters must not be null" );
		}

		// IMPL NOTE : resolving a BasicType follows very different algorithms based on what
		// specific information is available (non-null) from the BasicTypeParameters.  To help
		// facilitate that, we try to break this down into a number of sub-methods for some
		// high-level differences

		if ( parameters.getTemporalPrecision() != null ) {
			return resolveBasicTypeWithTemporalPrecision( parameters, jdbcTypeResolutionContext );
		}

		if ( parameters.getAttributeConverterDefinition() != null ) {
			return resolveConvertedBasicType( parameters, jdbcTypeResolutionContext );
		}


		JavaTypeDescriptor<T> javaTypeDescriptor = parameters.getJavaTypeDescriptor();
		SqlTypeDescriptor sqlTypeDescriptor = parameters.getSqlTypeDescriptor();

		if ( javaTypeDescriptor == null ) {
			if ( sqlTypeDescriptor == null ) {
				throw new IllegalArgumentException( "BasicTypeParameters must define either a JavaTypeDescriptor or a SqlTypeDescriptor (if not providing AttributeConverter)" );
			}
			javaTypeDescriptor = sqlTypeDescriptor.getJdbcRecommendedJavaTypeMapping( jdbcTypeResolutionContext.getTypeDescriptorRegistryAccess() );
		}

		if ( sqlTypeDescriptor == null ) {
			sqlTypeDescriptor = javaTypeDescriptor.getJdbcRecommendedSqlType( jdbcTypeResolutionContext );
		}

		final RegistryKey key = RegistryKey.from( javaTypeDescriptor, sqlTypeDescriptor, null );
		BasicType impl = registrations.get( key );
		if ( !isMatch( impl, parameters ) ) {
			MutabilityPlan<T> mutabilityPlan = parameters.getMutabilityPlan();
			if ( mutabilityPlan == null ) {
				mutabilityPlan = javaTypeDescriptor.getMutabilityPlan();
			}

			Comparator<T> comparator = parameters.getComparator();
			if ( comparator == null ) {
				comparator = javaTypeDescriptor.getComparator();
			}

			if ( TemporalTypeDescriptor.class.isInstance( javaTypeDescriptor ) ) {
				impl = new TemporalTypeImpl( typeConfiguration, (TemporalTypeDescriptor) javaTypeDescriptor, sqlTypeDescriptor, mutabilityPlan, comparator );
			}
			else {
				impl = new BasicTypeImpl( typeConfiguration, javaTypeDescriptor, sqlTypeDescriptor, mutabilityPlan, comparator );
			}

			registrations.put( key, impl );
		}

		return impl;
	}

	private <T> boolean isMatch(BasicType<T> impl, BasicTypeParameters<T> parameters) {
		if ( impl == null ) {
			return false;
		}

		if ( parameters.getJavaTypeDescriptor() != null ) {
			if ( impl.getJavaTypeDescriptor() != parameters.getJavaTypeDescriptor() ) {
				return false;
			}
		}

		if ( parameters.getSqlTypeDescriptor() != null ) {
			if ( impl.getColumnMapping().getSqlTypeDescriptor() != parameters.getSqlTypeDescriptor() ) {
				return false;
			}
		}

		if ( parameters.getMutabilityPlan() != null ) {
			if ( impl.getMutabilityPlan() != parameters.getMutabilityPlan() ) {
				return false;
			}
		}

		if ( parameters.getComparator() != null ) {
			if ( impl.getComparator() != parameters.getComparator() ) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Builds a BasicType when we have temporal precision (JPA's TemporalType) associated
	 * with the request
	 */
	@SuppressWarnings("unchecked")
	private <T> BasicType<T> resolveBasicTypeWithTemporalPrecision(
			BasicTypeParameters<T> parameters,
			JdbcRecommendedSqlTypeMappingContext jdbcTypeResolutionContext) {
		assert parameters != null;
		assert parameters.getTemporalPrecision() != null;

		final BasicType baseType = resolveBasicType(
				new BasicTypeParameters<T>() {
					@Override
					public JavaTypeDescriptor<T> getJavaTypeDescriptor() {
						return parameters.getJavaTypeDescriptor();
					}

					@Override
					public SqlTypeDescriptor getSqlTypeDescriptor() {
						return parameters.getSqlTypeDescriptor();
					}

					@Override
					public AttributeConverterDefinition getAttributeConverterDefinition() {
						return parameters.getAttributeConverterDefinition();
					}

					@Override
					public MutabilityPlan<T> getMutabilityPlan() {
						return parameters.getMutabilityPlan();
					}

					@Override
					public Comparator<T> getComparator() {
						return parameters.getComparator();
					}

					@Override
					public TemporalType getTemporalPrecision() {
						return null;
					}
				},
				jdbcTypeResolutionContext
		);

		if ( !org.hibernate.type.spi.basic.TemporalType.class.isInstance( baseType ) ) {
			throw new IllegalArgumentException( "Expecting a TemporalType, but found [" + baseType + "]" );
		}

		return ( ( org.hibernate.type.spi.basic.TemporalType<T>) baseType ).resolveTypeForPrecision(
				parameters.getTemporalPrecision(),
				this
		);
	}

	/**
	 * Builds a BasicType when we have an AttributeConverter associated with the request
	 */
	@SuppressWarnings("unchecked")
	private <T> BasicType<T> resolveConvertedBasicType(
			BasicTypeParameters<T> parameters,
			JdbcRecommendedSqlTypeMappingContext jdbcTypeResolutionContext) {
		assert parameters != null;
		assert parameters.getAttributeConverterDefinition() != null;

		final JavaTypeDescriptor converterDefinedDomainTypeDescriptor = context.getJavaTypeDescriptorRegistry().getDescriptor(
				parameters.getAttributeConverterDefinition().getDomainType()
		);
		final JavaTypeDescriptor converterDefinedJdbcTypeDescriptor = context.getJavaTypeDescriptorRegistry().getDescriptor(
				parameters.getAttributeConverterDefinition().getJdbcType()
		);

		JavaTypeDescriptor javaTypeDescriptor = parameters.getJavaTypeDescriptor();
		if ( javaTypeDescriptor == null ) {
			javaTypeDescriptor = converterDefinedDomainTypeDescriptor;
		}
		else {
			// todo : check that they match?
		}

		SqlTypeDescriptor sqlTypeDescriptor = parameters.getSqlTypeDescriptor();
		if ( sqlTypeDescriptor == null ) {
			sqlTypeDescriptor = converterDefinedJdbcTypeDescriptor.getJdbcRecommendedSqlType( jdbcTypeResolutionContext );
		}

		final RegistryKey key = RegistryKey.from( javaTypeDescriptor, sqlTypeDescriptor, parameters.getAttributeConverterDefinition() );
		final BasicType existing = registrations.get( key );
		if ( isMatch( existing, parameters ) ) {
			return existing;
		}

		MutabilityPlan<T> mutabilityPlan = parameters.getMutabilityPlan();
		if ( mutabilityPlan == null ) {
			mutabilityPlan = javaTypeDescriptor.getMutabilityPlan();
		}

		Comparator<T> comparator = parameters.getComparator();
		if ( comparator == null ) {
			comparator = javaTypeDescriptor.getComparator();
		}

		final BasicType<T> impl;
		if ( TemporalTypeDescriptor.class.isInstance( javaTypeDescriptor ) ) {
			impl = new TemporalTypeImpl(
					typeConfiguration,
					(TemporalTypeDescriptor) javaTypeDescriptor,
					sqlTypeDescriptor,
					mutabilityPlan,
					comparator,
					parameters.getAttributeConverterDefinition().getAttributeConverter(),
					converterDefinedJdbcTypeDescriptor
			);
		}
		else {
			impl = new BasicTypeImpl(
					typeConfiguration,
					javaTypeDescriptor,
					sqlTypeDescriptor,
					mutabilityPlan,
					comparator,
					parameters.getAttributeConverterDefinition().getAttributeConverter(),
					converterDefinedJdbcTypeDescriptor
			);
		}
		registrations.put( key, impl );
		return impl;
	}
}
