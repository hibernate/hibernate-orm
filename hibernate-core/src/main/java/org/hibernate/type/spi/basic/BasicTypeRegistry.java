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

import org.hibernate.HibernateException;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.RegistryKey;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.type.spi.descriptor.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.spi.descriptor.TypeDescriptorRegistryAccess;
import org.hibernate.type.spi.descriptor.java.BigDecimalTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.BigIntegerTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.BlobTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.BooleanTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.ByteArrayTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.ByteTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.CalendarDateTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.CalendarTimeTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.CharacterArrayTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.CharacterTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.ClassTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.ClobTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.CurrencyTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.DoubleTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.DurationJavaDescriptor;
import org.hibernate.type.spi.descriptor.java.FloatTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.spi.descriptor.java.InstantJavaDescriptor;
import org.hibernate.type.spi.descriptor.java.IntegerTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.JdbcDateTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.JdbcTimeTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.JdbcTimestampTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.LocalDateJavaDescriptor;
import org.hibernate.type.spi.descriptor.java.LocalDateTimeJavaDescriptor;
import org.hibernate.type.spi.descriptor.java.LocalTimeJavaDescriptor;
import org.hibernate.type.spi.descriptor.java.LocaleTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.LongTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.MutabilityPlan;
import org.hibernate.type.spi.descriptor.java.NClobTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.OffsetDateTimeJavaDescriptor;
import org.hibernate.type.spi.descriptor.java.OffsetTimeJavaDescriptor;
import org.hibernate.type.spi.descriptor.java.PrimitiveByteArrayTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.PrimitiveCharacterArrayTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.SerializableTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.ShortTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.StringTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.TemporalTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.TimeZoneTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.UUIDTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.UrlTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.ZonedDateTimeJavaDescriptor;
import org.hibernate.type.spi.descriptor.sql.BigIntTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.BinaryTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.CharTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.DateTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.LongNVarcharTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.LongVarbinaryTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.LongVarcharTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.NCharTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.NVarcharTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.NumericTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.SmallIntTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.SqlTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.TimeTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.TimestampTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.TinyIntTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.VarbinaryTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.VarcharTypeDescriptor;

/**
 * Redesign of {@link org.hibernate.type.BasicTypeRegistry} based on idea of "composing"
 * a BasicType from JavaTypeDescriptor, SqlTypeDescriptor and AttributeConverter.
 *
 * @author Steve Ebersole
 * @author Chris Cranford
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
		registerBasicTypes();
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

		final RegistryKey key = RegistryKeyImpl.from( javaTypeDescriptor, sqlTypeDescriptor, null );
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
					public javax.persistence.TemporalType getTemporalPrecision() {
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

		final RegistryKey key = RegistryKeyImpl.from( javaTypeDescriptor, sqlTypeDescriptor, parameters.getAttributeConverterDefinition() );
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

	public void register(BasicType type, RegistryKey registryKey) {
		if ( registryKey == null ) {
			throw new HibernateException( "Cannot register a type with a null registry key." );
		}
		if ( type == null ) {
			throw new HibernateException( "Cannot register a null type." );
		}
		registrations.put( registryKey, type );
	}

	@SuppressWarnings("unchecked")
	private void registerBasicTypes() {
		registerBasicType( BooleanTypeDescriptor.INSTANCE, org.hibernate.type.spi.descriptor.sql.BooleanTypeDescriptor.INSTANCE );
		registerBasicType( IntegerTypeDescriptor.INSTANCE, org.hibernate.type.spi.descriptor.sql.BooleanTypeDescriptor.INSTANCE );
		registerBasicType( new BooleanTypeDescriptor( 'T', 'F' ), CharTypeDescriptor.INSTANCE );
		registerBasicType( BooleanTypeDescriptor.INSTANCE, CharTypeDescriptor.INSTANCE );

		registerBasicType( ByteTypeDescriptor.INSTANCE, TinyIntTypeDescriptor.INSTANCE );
		registerBasicType( CharacterTypeDescriptor.INSTANCE, CharTypeDescriptor.INSTANCE );
		registerBasicType( ShortTypeDescriptor.INSTANCE, SmallIntTypeDescriptor.INSTANCE );
		registerBasicType( IntegerTypeDescriptor.INSTANCE, org.hibernate.type.spi.descriptor.sql.IntegerTypeDescriptor.INSTANCE );
		registerBasicType( LongTypeDescriptor.INSTANCE, BigIntTypeDescriptor.INSTANCE );
		registerBasicType( FloatTypeDescriptor.INSTANCE, org.hibernate.type.spi.descriptor.sql.FloatTypeDescriptor.INSTANCE );
		registerBasicType( DoubleTypeDescriptor.INSTANCE, org.hibernate.type.spi.descriptor.sql.DoubleTypeDescriptor.INSTANCE );
		registerBasicType( BigDecimalTypeDescriptor.INSTANCE, NumericTypeDescriptor.INSTANCE );
		registerBasicType( BigIntegerTypeDescriptor.INSTANCE, NumericTypeDescriptor.INSTANCE );

		registerBasicType( StringTypeDescriptor.INSTANCE, VarcharTypeDescriptor.INSTANCE );
		registerBasicType( StringTypeDescriptor.INSTANCE, NVarcharTypeDescriptor.INSTANCE );
		registerBasicType( CharacterTypeDescriptor.INSTANCE, NCharTypeDescriptor.INSTANCE );
		registerBasicType( UrlTypeDescriptor.INSTANCE, VarcharTypeDescriptor.INSTANCE );

		registerBasicType( DurationJavaDescriptor.INSTANCE, BigIntTypeDescriptor.INSTANCE );

		registerTemporalType( InstantJavaDescriptor.INSTANCE, TimestampTypeDescriptor.INSTANCE );
		registerTemporalType( LocalDateTimeJavaDescriptor.INSTANCE, TimestampTypeDescriptor.INSTANCE );
		registerTemporalType( LocalDateJavaDescriptor.INSTANCE, DateTypeDescriptor.INSTANCE );
		registerTemporalType( LocalTimeJavaDescriptor.INSTANCE, TimeTypeDescriptor.INSTANCE );
		registerTemporalType( OffsetDateTimeJavaDescriptor.INSTANCE, TimestampTypeDescriptor.INSTANCE );
		registerTemporalType( OffsetTimeJavaDescriptor.INSTANCE, TimeTypeDescriptor.INSTANCE );
		registerTemporalType( ZonedDateTimeJavaDescriptor.INSTANCE, TimestampTypeDescriptor.INSTANCE );

		registerTemporalType( JdbcDateTypeDescriptor.INSTANCE, DateTypeDescriptor.INSTANCE );
		registerTemporalType( JdbcTimeTypeDescriptor.INSTANCE, TimeTypeDescriptor.INSTANCE );
		registerTemporalType( JdbcTimestampTypeDescriptor.INSTANCE, TimestampTypeDescriptor.INSTANCE );
		registerTemporalType( CalendarTimeTypeDescriptor.INSTANCE, TimestampTypeDescriptor.INSTANCE );
		registerTemporalType( CalendarDateTypeDescriptor.INSTANCE, DateTypeDescriptor.INSTANCE );

		registerBasicType( LocaleTypeDescriptor.INSTANCE, VarcharTypeDescriptor.INSTANCE );
		registerBasicType( CurrencyTypeDescriptor.INSTANCE, VarcharTypeDescriptor.INSTANCE );
		registerBasicType( TimeZoneTypeDescriptor.INSTANCE, VarcharTypeDescriptor.INSTANCE );
		registerBasicType( ClassTypeDescriptor.INSTANCE, VarcharTypeDescriptor.INSTANCE );
		registerBasicType( UUIDTypeDescriptor.INSTANCE, BinaryTypeDescriptor.INSTANCE );
		registerBasicType( UUIDTypeDescriptor.INSTANCE, VarcharTypeDescriptor.INSTANCE );

		registerBasicType( PrimitiveByteArrayTypeDescriptor.INSTANCE, VarbinaryTypeDescriptor.INSTANCE );
		registerBasicType( ByteArrayTypeDescriptor.INSTANCE, VarbinaryTypeDescriptor.INSTANCE );
		registerBasicType( PrimitiveByteArrayTypeDescriptor.INSTANCE, LongVarbinaryTypeDescriptor.INSTANCE );
		registerBasicType( PrimitiveCharacterArrayTypeDescriptor.INSTANCE, VarcharTypeDescriptor.INSTANCE );
		registerBasicType( CharacterArrayTypeDescriptor.INSTANCE, VarcharTypeDescriptor.INSTANCE );
		registerBasicType( StringTypeDescriptor.INSTANCE, LongVarcharTypeDescriptor.INSTANCE );
		registerBasicType( StringTypeDescriptor.INSTANCE, LongNVarcharTypeDescriptor.INSTANCE );
		registerBasicType( BlobTypeDescriptor.INSTANCE, org.hibernate.type.spi.descriptor.sql.BlobTypeDescriptor.DEFAULT );
		registerBasicType( PrimitiveByteArrayTypeDescriptor.INSTANCE, org.hibernate.type.spi.descriptor.sql.BlobTypeDescriptor.DEFAULT );
		registerBasicType( ClobTypeDescriptor.INSTANCE, org.hibernate.type.spi.descriptor.sql.ClobTypeDescriptor.DEFAULT );
		registerBasicType( NClobTypeDescriptor.INSTANCE, org.hibernate.type.spi.descriptor.sql.NClobTypeDescriptor.DEFAULT );
		registerBasicType( StringTypeDescriptor.INSTANCE, org.hibernate.type.spi.descriptor.sql.ClobTypeDescriptor.DEFAULT );
		registerBasicType( StringTypeDescriptor.INSTANCE, org.hibernate.type.spi.descriptor.sql.NClobTypeDescriptor.DEFAULT );
		registerBasicType( SerializableTypeDescriptor.INSTANCE, VarbinaryTypeDescriptor.INSTANCE );

		// todo : ObjectType
		// composed of these two types.
		// StringType.INSTANCE = ( StringTypeDescriptor.INSTANCE, VarcharTypeDescriptor.INSTANCE )
		// SerializableType.INSTANCE = ( SerializableTypeDescriptor.INSTANCE, VarbinaryTypeDescriptor.INSTANCE )
		// based on AnyType

		// Immutable types
		registerTemporalType( JdbcDateTypeDescriptor.INSTANCE, DateTypeDescriptor.INSTANCE, ImmutableMutabilityPlan.INSTANCE );
		registerTemporalType( JdbcTimeTypeDescriptor.INSTANCE, TimeTypeDescriptor.INSTANCE, ImmutableMutabilityPlan.INSTANCE );
		registerTemporalType( JdbcTimestampTypeDescriptor.INSTANCE, TimestampTypeDescriptor.INSTANCE, ImmutableMutabilityPlan.INSTANCE );
		registerTemporalType( CalendarTimeTypeDescriptor.INSTANCE, TimestampTypeDescriptor.INSTANCE, ImmutableMutabilityPlan.INSTANCE );
		registerTemporalType( CalendarDateTypeDescriptor.INSTANCE, DateTypeDescriptor.INSTANCE, ImmutableMutabilityPlan.INSTANCE );
		registerBasicType( PrimitiveByteArrayTypeDescriptor.INSTANCE, VarbinaryTypeDescriptor.INSTANCE, ImmutableMutabilityPlan.INSTANCE );
		registerBasicType( SerializableTypeDescriptor.INSTANCE, VarbinaryTypeDescriptor.INSTANCE, ImmutableMutabilityPlan.INSTANCE );
	}

	private void registerBasicType(JavaTypeDescriptor javaTypeDescriptor, SqlTypeDescriptor sqlTypeDescriptor) {
		registerBasicType( javaTypeDescriptor, sqlTypeDescriptor, null );
	}

	@SuppressWarnings("unchecked")
	private void registerBasicType(JavaTypeDescriptor javaTypeDescriptor,
			SqlTypeDescriptor sqlTypeDescriptor,
			MutabilityPlan mutabilityPlan) {
		final BasicType type = new BasicTypeImpl(
				typeConfiguration,
				javaTypeDescriptor,
				sqlTypeDescriptor,
				mutabilityPlan,
				null
		);
		register( type, RegistryKeyImpl.from( javaTypeDescriptor, sqlTypeDescriptor, null ) );
	}

	private void registerTemporalType(TemporalTypeDescriptor temporalTypeDescriptor, SqlTypeDescriptor sqlTypeDescriptor) {
		registerTemporalType( temporalTypeDescriptor, sqlTypeDescriptor, null );
	}

	@SuppressWarnings("unchecked")
	private void registerTemporalType(TemporalTypeDescriptor temporalTypeDescriptor,
			SqlTypeDescriptor sqlTypeDescriptor,
			MutabilityPlan mutabilityPlan) {
		final TemporalType type = new TemporalTypeImpl(
				typeConfiguration,
				temporalTypeDescriptor,
				sqlTypeDescriptor,
				mutabilityPlan,
				null
		);
		register( type, RegistryKeyImpl.from( temporalTypeDescriptor, sqlTypeDescriptor, null ) );
	}
}
