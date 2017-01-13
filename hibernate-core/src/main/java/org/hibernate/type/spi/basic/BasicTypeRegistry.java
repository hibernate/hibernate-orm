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
import org.hibernate.Incubating;
import org.hibernate.type.TemporalTypeImpl;
import org.hibernate.type.converter.spi.AttributeConverterDefinition;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.TemporalType;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.spi.descriptor.TypeDescriptorRegistryAccess;
import org.hibernate.type.descriptor.java.internal.BigDecimalJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.BigIntegerJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.BlobJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.BooleanJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.ByteArrayJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.ByteJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.CalendarDateJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.CalendarTimeJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.CharacterArrayJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.CharacterJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.ClassJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.ClobJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.CurrencyJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.DoubleJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.DurationJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.FloatJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.internal.InstantJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.IntegerJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.internal.JdbcDateJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.JdbcTimeJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.JdbcTimestampJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.LocalDateJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.LocalDateTimeJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.LocalTimeJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.LocaleJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.LongJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.MutabilityPlan;
import org.hibernate.type.descriptor.java.internal.NClobJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.OffsetDateTimeJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.OffsetTimeJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.PrimitiveByteArrayJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.PrimitiveCharacterArrayJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.SerializableJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.ShortJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.StringJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.TemporalJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.TimeZoneJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.UUIDJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.UrlJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.ZonedDateTimeJavaDescriptor;
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
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.TimeTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.TimestampTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.TinyIntTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.VarbinaryTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.VarcharTypeDescriptor;

/**
 * Registry for BasicType instances for lookupsicType from JavaTypeDescriptor, SqlTypeDescriptor and AttributeConverter.
 *
 * @author Steve Ebersole
 * @author Chris Cranford
 */
@Incubating
public class BasicTypeRegistry {
	private final Map<RegistryKey,BasicType> registrations = new HashMap<>();

	private final TypeConfiguration typeConfiguration;
	private final JdbcRecommendedSqlTypeMappingContext baseJdbcRecommendedSqlTypeMappingContext;

	public BasicTypeRegistry(TypeConfiguration typeConfiguration) {
		this.typeConfiguration = typeConfiguration;
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
			public TypeConfiguration getTypeConfiguration() {
				return typeConfiguration;
			}
		};
		registerBasicTypes();
	}

	public TypeDescriptorRegistryAccess getTypeDescriptorRegistryAccess() {
		return typeConfiguration;
	}

	public JdbcRecommendedSqlTypeMappingContext getBaseJdbcRecommendedSqlTypeMappingContext() {
		return baseJdbcRecommendedSqlTypeMappingContext;
	}

	@SuppressWarnings("unchecked")
	public <T> BasicType<T> getRegisteredBasicType(RegistryKey registryKey) {
		return registrations.get( registryKey );
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
			javaTypeDescriptor = sqlTypeDescriptor.getJdbcRecommendedJavaTypeMapping( jdbcTypeResolutionContext.getTypeConfiguration() );
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

			if ( TemporalJavaDescriptor.class.isInstance( javaTypeDescriptor ) ) {
				impl = new TemporalTypeImpl( (TemporalJavaDescriptor) javaTypeDescriptor, sqlTypeDescriptor, mutabilityPlan, comparator );
			}
			else {
				impl = new BasicTypeImpl( javaTypeDescriptor, sqlTypeDescriptor, mutabilityPlan, comparator );
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
			if ( impl.getColumnMappings()[0].getSqlTypeDescriptor() != parameters.getSqlTypeDescriptor() ) {
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

		if ( !TemporalType.class.isInstance( baseType ) ) {
			throw new IllegalArgumentException( "Expecting a TemporalType, but found [" + baseType + "]" );
		}

		return ( (TemporalType<T>) baseType ).resolveTypeForPrecision(
				parameters.getTemporalPrecision(),
				typeConfiguration
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

		final JavaTypeDescriptor converterDefinedDomainTypeDescriptor = parameters.getAttributeConverterDefinition().getDomainType();
		final JavaTypeDescriptor converterDefinedJdbcTypeDescriptor = parameters.getAttributeConverterDefinition().getJdbcType();

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
		if ( TemporalJavaDescriptor.class.isInstance( javaTypeDescriptor ) ) {
			final TemporalJavaDescriptor javaTemporalTypeDescriptor = (TemporalJavaDescriptor) javaTypeDescriptor;
			impl = new TemporalTypeImpl(
					javaTemporalTypeDescriptor,
					sqlTypeDescriptor,
					mutabilityPlan,
					comparator,
					parameters.getAttributeConverterDefinition()
			);
		}
		else {
			impl = new BasicTypeImpl(
					javaTypeDescriptor,
					sqlTypeDescriptor,
					mutabilityPlan,
					comparator,
					parameters.getAttributeConverterDefinition()
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
		registerBasicType( BooleanJavaDescriptor.INSTANCE, org.hibernate.type.spi.descriptor.sql.BooleanTypeDescriptor.INSTANCE );
		registerBasicType( IntegerJavaDescriptor.INSTANCE, org.hibernate.type.spi.descriptor.sql.BooleanTypeDescriptor.INSTANCE );
		registerBasicType( new BooleanJavaDescriptor( 'T', 'F' ), CharTypeDescriptor.INSTANCE );
		registerBasicType( BooleanJavaDescriptor.INSTANCE, CharTypeDescriptor.INSTANCE );

		registerBasicType( ByteJavaDescriptor.INSTANCE, TinyIntTypeDescriptor.INSTANCE );
		registerBasicType( CharacterJavaDescriptor.INSTANCE, CharTypeDescriptor.INSTANCE );
		registerBasicType( ShortJavaDescriptor.INSTANCE, SmallIntTypeDescriptor.INSTANCE );
		registerBasicType( IntegerJavaDescriptor.INSTANCE, org.hibernate.type.spi.descriptor.sql.IntegerTypeDescriptor.INSTANCE );
		registerBasicType( LongJavaDescriptor.INSTANCE, BigIntTypeDescriptor.INSTANCE );
		registerBasicType( FloatJavaDescriptor.INSTANCE, org.hibernate.type.spi.descriptor.sql.FloatTypeDescriptor.INSTANCE );
		registerBasicType( DoubleJavaDescriptor.INSTANCE, org.hibernate.type.spi.descriptor.sql.DoubleTypeDescriptor.INSTANCE );
		registerBasicType( BigDecimalJavaDescriptor.INSTANCE, NumericTypeDescriptor.INSTANCE );
		registerBasicType( BigIntegerJavaDescriptor.INSTANCE, NumericTypeDescriptor.INSTANCE );

		registerBasicType( StringJavaDescriptor.INSTANCE, VarcharTypeDescriptor.INSTANCE );
		registerBasicType( StringJavaDescriptor.INSTANCE, NVarcharTypeDescriptor.INSTANCE );
		registerBasicType( CharacterJavaDescriptor.INSTANCE, NCharTypeDescriptor.INSTANCE );
		registerBasicType( UrlJavaDescriptor.INSTANCE, VarcharTypeDescriptor.INSTANCE );

		registerBasicType( DurationJavaDescriptor.INSTANCE, BigIntTypeDescriptor.INSTANCE );

		registerTemporalType( InstantJavaDescriptor.INSTANCE, TimestampTypeDescriptor.INSTANCE );
		registerTemporalType( LocalDateTimeJavaDescriptor.INSTANCE, TimestampTypeDescriptor.INSTANCE );
		registerTemporalType( LocalDateJavaDescriptor.INSTANCE, DateTypeDescriptor.INSTANCE );
		registerTemporalType( LocalTimeJavaDescriptor.INSTANCE, TimeTypeDescriptor.INSTANCE );
		registerTemporalType( OffsetDateTimeJavaDescriptor.INSTANCE, TimestampTypeDescriptor.INSTANCE );
		registerTemporalType( OffsetTimeJavaDescriptor.INSTANCE, TimeTypeDescriptor.INSTANCE );
		registerTemporalType( ZonedDateTimeJavaDescriptor.INSTANCE, TimestampTypeDescriptor.INSTANCE );

		registerTemporalType( JdbcDateJavaDescriptor.INSTANCE, DateTypeDescriptor.INSTANCE );
		registerTemporalType( JdbcTimeJavaDescriptor.INSTANCE, TimeTypeDescriptor.INSTANCE );
		registerTemporalType( JdbcTimestampJavaDescriptor.INSTANCE, TimestampTypeDescriptor.INSTANCE );
		registerTemporalType( CalendarTimeJavaDescriptor.INSTANCE, TimestampTypeDescriptor.INSTANCE );
		registerTemporalType( CalendarDateJavaDescriptor.INSTANCE, DateTypeDescriptor.INSTANCE );

		registerBasicType( LocaleJavaDescriptor.INSTANCE, VarcharTypeDescriptor.INSTANCE );
		registerBasicType( CurrencyJavaDescriptor.INSTANCE, VarcharTypeDescriptor.INSTANCE );
		registerBasicType( TimeZoneJavaDescriptor.INSTANCE, VarcharTypeDescriptor.INSTANCE );
		registerBasicType( ClassJavaDescriptor.INSTANCE, VarcharTypeDescriptor.INSTANCE );
		registerBasicType( UUIDJavaDescriptor.INSTANCE, BinaryTypeDescriptor.INSTANCE );
		registerBasicType( UUIDJavaDescriptor.INSTANCE, VarcharTypeDescriptor.INSTANCE );

		registerBasicType( PrimitiveByteArrayJavaDescriptor.INSTANCE, VarbinaryTypeDescriptor.INSTANCE );
		registerBasicType( ByteArrayJavaDescriptor.INSTANCE, VarbinaryTypeDescriptor.INSTANCE );
		registerBasicType( PrimitiveByteArrayJavaDescriptor.INSTANCE, LongVarbinaryTypeDescriptor.INSTANCE );
		registerBasicType( PrimitiveCharacterArrayJavaDescriptor.INSTANCE, VarcharTypeDescriptor.INSTANCE );
		registerBasicType( CharacterArrayJavaDescriptor.INSTANCE, VarcharTypeDescriptor.INSTANCE );
		registerBasicType( StringJavaDescriptor.INSTANCE, LongVarcharTypeDescriptor.INSTANCE );
		registerBasicType( StringJavaDescriptor.INSTANCE, LongNVarcharTypeDescriptor.INSTANCE );
		registerBasicType( BlobJavaDescriptor.INSTANCE, org.hibernate.type.spi.descriptor.sql.BlobTypeDescriptor.DEFAULT );
		registerBasicType( PrimitiveByteArrayJavaDescriptor.INSTANCE, org.hibernate.type.spi.descriptor.sql.BlobTypeDescriptor.DEFAULT );
		registerBasicType( ClobJavaDescriptor.INSTANCE, org.hibernate.type.spi.descriptor.sql.ClobTypeDescriptor.DEFAULT );
		registerBasicType( NClobJavaDescriptor.INSTANCE, org.hibernate.type.spi.descriptor.sql.NClobTypeDescriptor.DEFAULT );
		registerBasicType( StringJavaDescriptor.INSTANCE, org.hibernate.type.spi.descriptor.sql.ClobTypeDescriptor.DEFAULT );
		registerBasicType( StringJavaDescriptor.INSTANCE, org.hibernate.type.spi.descriptor.sql.NClobTypeDescriptor.DEFAULT );
		registerBasicType( SerializableJavaDescriptor.INSTANCE, VarbinaryTypeDescriptor.INSTANCE );

		// todo : ObjectType
		// composed of these two types.
		// StringType.INSTANCE = ( StringTypeDescriptor.INSTANCE, VarcharTypeDescriptor.INSTANCE )
		// SerializableType.INSTANCE = ( SerializableTypeDescriptor.INSTANCE, VarbinaryTypeDescriptor.INSTANCE )
		// based on AnyType

		// Immutable types
		registerTemporalType( JdbcDateJavaDescriptor.INSTANCE, DateTypeDescriptor.INSTANCE, ImmutableMutabilityPlan.INSTANCE );
		registerTemporalType( JdbcTimeJavaDescriptor.INSTANCE, TimeTypeDescriptor.INSTANCE, ImmutableMutabilityPlan.INSTANCE );
		registerTemporalType( JdbcTimestampJavaDescriptor.INSTANCE, TimestampTypeDescriptor.INSTANCE, ImmutableMutabilityPlan.INSTANCE );
		registerTemporalType( CalendarTimeJavaDescriptor.INSTANCE, TimestampTypeDescriptor.INSTANCE, ImmutableMutabilityPlan.INSTANCE );
		registerTemporalType( CalendarDateJavaDescriptor.INSTANCE, DateTypeDescriptor.INSTANCE, ImmutableMutabilityPlan.INSTANCE );
		registerBasicType( PrimitiveByteArrayJavaDescriptor.INSTANCE, VarbinaryTypeDescriptor.INSTANCE, ImmutableMutabilityPlan.INSTANCE );
		registerBasicType( SerializableJavaDescriptor.INSTANCE, VarbinaryTypeDescriptor.INSTANCE, ImmutableMutabilityPlan.INSTANCE );
	}

	private void registerBasicType(JavaTypeDescriptor javaTypeDescriptor, SqlTypeDescriptor sqlTypeDescriptor) {
		registerBasicType( javaTypeDescriptor, sqlTypeDescriptor, null );
	}

	@SuppressWarnings("unchecked")
	private void registerBasicType(JavaTypeDescriptor javaTypeDescriptor,
								   SqlTypeDescriptor sqlTypeDescriptor,
								   MutabilityPlan mutabilityPlan) {
		final BasicType type = new BasicTypeImpl(
				javaTypeDescriptor,
				sqlTypeDescriptor,
				mutabilityPlan,
				null
		);
		register( type, RegistryKey.from( javaTypeDescriptor, sqlTypeDescriptor, null ) );
	}

	private void registerTemporalType(TemporalJavaDescriptor temporalTypeDescriptor, SqlTypeDescriptor sqlTypeDescriptor) {
		registerTemporalType( temporalTypeDescriptor, sqlTypeDescriptor, null );
	}

	@SuppressWarnings("unchecked")
	private void registerTemporalType(TemporalJavaDescriptor temporalTypeDescriptor,
									  SqlTypeDescriptor sqlTypeDescriptor,
									  MutabilityPlan mutabilityPlan) {
		final TemporalType type = new TemporalTypeImpl(
				temporalTypeDescriptor,
				sqlTypeDescriptor,
				mutabilityPlan,
				null
		);
		register( type, RegistryKey.from( temporalTypeDescriptor, sqlTypeDescriptor, null ) );
	}
}
