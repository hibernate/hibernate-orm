/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.process.internal;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.MappingException;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Table;
import org.hibernate.tool.schema.extract.spi.ColumnTypeInformation;
import org.hibernate.type.AdjustableBasicType;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.SerializableType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.java.BasicPluralJavaType;
import org.hibernate.type.descriptor.java.EnumJavaType;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.SerializableJavaType;
import org.hibernate.type.descriptor.java.TemporalJavaType;
import org.hibernate.type.descriptor.java.spi.JdbcTypeRecommendationException;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.descriptor.jdbc.ObjectJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.internal.ConvertedBasicTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.EnumType;
import jakarta.persistence.EnumeratedValue;
import jakarta.persistence.TemporalType;

import static org.hibernate.type.SqlTypes.SMALLINT;
import static org.hibernate.type.descriptor.java.JavaTypeHelper.isTemporal;

/**
 * BasicValue.Resolution resolver for cases where no explicit
 * type info was supplied.
 */
public class InferredBasicValueResolver {

	public static <T> BasicValue.Resolution<T> from(
			BasicJavaType<T> explicitJavaType,
			JdbcType explicitJdbcType,
			Type resolvedJavaType,
			Supplier<JavaType<T>> reflectedJtdResolver,
			Function<TypeConfiguration, MutabilityPlan<?>> explicitMutabilityPlanAccess,
			JdbcTypeIndicators stdIndicators,
			Table table,
			Selectable selectable,
			String ownerName,
			String propertyName,
			MetadataBuildingContext buildingContext) {
		final BootstrapContext bootstrapContext = buildingContext.getBootstrapContext();
		final TypeConfiguration typeConfiguration = bootstrapContext.getTypeConfiguration();
		final BasicTypeRegistry basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();

		final JavaType<T> reflectedJtd = reflectedJtdResolver.get();

		// NOTE: the distinction that is made below wrt `explicitJavaType` and `reflectedJtd`
		//       is needed temporarily to trigger "legacy resolution" versus "ORM6 resolution.
		//       Yes, it makes the code a little more complicated but the benefit is well worth
		//       it - saving memory

		final BasicType<T> jdbcMapping;

		if ( explicitJavaType != null ) {
			// we have an explicit JavaType
			if ( isTemporal( explicitJavaType ) ) {
				return fromTemporal(
						(TemporalJavaType<T>) explicitJavaType,
						null,
						explicitJdbcType,
						explicitMutabilityPlanAccess,
						stdIndicators
				);
			}
			else if ( explicitJdbcType != null ) {
				// we also have an explicit JdbcType
				jdbcMapping = basicTypeRegistry.resolve( explicitJavaType, explicitJdbcType );
			}
			else {
				// we need to infer the JdbcType and use that to build the value-mapping
				final JdbcType inferredJdbcType = explicitJavaType.getRecommendedJdbcType( stdIndicators );
				if ( inferredJdbcType instanceof ObjectJdbcType
						&& ( explicitJavaType instanceof SerializableJavaType
							|| explicitJavaType.getJavaType() instanceof Serializable ) ) {
					// Use the SerializableType if possible since ObjectJdbcType is our fallback
					jdbcMapping = new SerializableType( explicitJavaType );
				}
				else {
					jdbcMapping = resolveSqlTypeIndicators(
							stdIndicators,
							basicTypeRegistry.resolve( explicitJavaType, inferredJdbcType ),
							explicitJavaType
					);
				}
			}
		}
		else if ( reflectedJtd != null ) {
			// we were able to determine the "reflected java-type"
			// Use JTD if we know it to apply any specialized resolutions
			if ( reflectedJtd instanceof EnumJavaType enumJavaType ) {
				return fromEnum(
						enumJavaType,
						explicitJdbcType,
						stdIndicators,
						bootstrapContext
				);
			}
			else if ( isTemporal( reflectedJtd ) ) {
				return fromTemporal(
						(TemporalJavaType<T>) reflectedJtd,
						null,
						explicitJdbcType,
						explicitMutabilityPlanAccess,
						stdIndicators
				);
			}
			else if ( explicitJdbcType != null ) {
				// we also have an explicit JdbcType
				jdbcMapping = basicTypeRegistry.resolve( reflectedJtd, explicitJdbcType );
			}
			else {
				// see if there is a registered BasicType for this JavaType and,
				// if so, use it. This mimics the legacy handling.
				final BasicType registeredType =
						registeredType(
								explicitJdbcType,
								explicitMutabilityPlanAccess,
								stdIndicators,
								selectable,
								reflectedJtd,
								bootstrapContext,
								buildingContext.getMetadataCollector().getDatabase().getDialect()
						);

				if ( registeredType != null ) {
					// so here is the legacy resolution
					jdbcMapping = resolveSqlTypeIndicators( stdIndicators, registeredType, reflectedJtd );
				}
				else {
					// there was not a "legacy" BasicType registration,
					// so use `JavaType#getRecommendedJdbcType`, if one,
					// to create a mapping
					final JdbcType recommendedJdbcType;
					try {
						recommendedJdbcType = reflectedJtd.getRecommendedJdbcType( stdIndicators );
					}
					catch (JdbcTypeRecommendationException jtre) {
						if ( buildingContext.getMetadataCollector()
								.getEntityBindingMap().values().stream()
								.anyMatch( pc -> pc.getMappedClass().equals(resolvedJavaType) ) ) {
							throw new MappingException( "Incorrect use of entity type '"
									+ resolvedJavaType.getTypeName()
									+  "' (possibly due to missing association mapping annotation)",
									jtre );
						}
						else {
							throw jtre;
						}
					}
					if ( recommendedJdbcType != null ) {
						jdbcMapping = resolveSqlTypeIndicators(
								stdIndicators,
								basicTypeRegistry.resolve( reflectedJtd, recommendedJdbcType ),
								reflectedJtd
						);
					}
					else if ( reflectedJtd instanceof SerializableJavaType
							|| Serializable.class.isAssignableFrom( reflectedJtd.getJavaTypeClass() ) ) {
						jdbcMapping = new SerializableType( reflectedJtd );
					}
					else {
						// let this fall through to the exception creation below
						jdbcMapping = null;
					}
				}
			}
		}
		else {
			if ( explicitJdbcType != null ) {
				// we have an explicit STD, but no JTD - infer JTD
				// NOTE : yes it's an odd case, but easy to implement here, so...
				Integer length = null;
				Integer scale = null;
				if ( selectable instanceof Column column ) {
					if ( column.getPrecision() != null && column.getPrecision() > 0 ) {
						length = column.getPrecision();
						scale = column.getScale();
					}
					else if ( column.getLength() != null ) {
						if ( column.getLength() > (long) Integer.MAX_VALUE ) {
							length = Integer.MAX_VALUE;
						}
						else {
							length = column.getLength().intValue();
						}
					}
				}

				final JavaType<T> recommendedJtd =
						explicitJdbcType.getJdbcRecommendedJavaTypeMapping( length, scale, typeConfiguration );
				jdbcMapping = resolveSqlTypeIndicators(
						stdIndicators,
						basicTypeRegistry.resolve( recommendedJtd, explicitJdbcType ),
						recommendedJtd
				);
			}
			else {
				// we have neither a JTD nor STD
				throw new MappingException(
						"Could not determine JavaType nor JdbcType to use" +
								" for BasicValue: owner = " + ownerName +
								"; property = " + propertyName +
								"; table = " + table.getName() +
								"; column = " + selectable.getText()
				);
			}
		}

		if ( jdbcMapping == null ) {
			throw new MappingException(
					"Could not determine JavaType nor JdbcType to use" +
							" for " + ( (BasicValue) stdIndicators ).getResolvedJavaType() +
							"; table = " + table.getName() +
							"; column = " + selectable.getText()
			);
		}

		return new InferredBasicValueResolution<>(
				jdbcMapping,
				jdbcMapping.getJavaTypeDescriptor(),
				jdbcMapping.getJavaTypeDescriptor(),
				jdbcMapping.getJdbcType(),
				jdbcMapping,
				determineMutabilityPlan( explicitMutabilityPlanAccess, jdbcMapping.getJavaTypeDescriptor(), typeConfiguration )
		);
	}

	private static <T> BasicType<?> registeredType(
			JdbcType explicitJdbcType,
			Function<TypeConfiguration, MutabilityPlan<?>> explicitMutabilityPlanAccess,
			JdbcTypeIndicators stdIndicators,
			Selectable selectable,
			JavaType<T> reflectedJtd,
			BootstrapContext bootstrapContext,
			Dialect dialect) {
		final TypeConfiguration typeConfiguration = bootstrapContext.getTypeConfiguration();

		if ( reflectedJtd instanceof BasicPluralJavaType<?> pluralJavaType ) {
			final JavaType<?> elementJavaType = pluralJavaType.getElementJavaType();
			final BasicType registeredElementType = registeredElementType(
					explicitJdbcType,
					explicitMutabilityPlanAccess,
					stdIndicators,
					bootstrapContext,
					elementJavaType
			);
			final BasicType<?> registeredType =
					registeredElementType == null
							? null
							: pluralJavaType.resolveType(
									typeConfiguration,
									dialect,
									resolveSqlTypeIndicators( stdIndicators, registeredElementType, elementJavaType ),
									selectable instanceof ColumnTypeInformation information ? information : null,
									stdIndicators
							);
			if ( registeredType instanceof BasicPluralType<?, ?> ) {
				typeConfiguration.getBasicTypeRegistry().register( registeredType );
			}
			return registeredType;
		}
		else {
			return typeConfiguration.getBasicTypeRegistry().getRegisteredType( reflectedJtd.getJavaType() );
		}
	}

	private static BasicType<?> registeredElementType(
			JdbcType explicitJdbcType,
			Function<TypeConfiguration, MutabilityPlan<?>> explicitMutabilityPlanAccess,
			JdbcTypeIndicators stdIndicators,
			BootstrapContext context,
			JavaType<?> elementJtd) {
		if ( elementJtd instanceof EnumJavaType<?> enumJavaType ) {
			return (BasicType<?>) fromEnum( enumJavaType, explicitJdbcType, stdIndicators, context ).getJdbcMapping();
		}
		else if ( isTemporal( elementJtd ) ) {
			final BasicValue.Resolution<?> resolution = fromTemporal(
					(TemporalJavaType<?>) elementJtd,
					null,
					null,
					explicitMutabilityPlanAccess,
					stdIndicators
			);
			return resolution.getLegacyResolvedBasicType();
		}
		else {
			return context.getTypeConfiguration().getBasicTypeRegistry().getRegisteredType( elementJtd.getJavaType() );
		}
	}

	public static <T> BasicType<T> resolveSqlTypeIndicators(
			JdbcTypeIndicators stdIndicators,
			BasicType<T> resolved,
			JavaType<T> domainJtd) {
		if ( resolved instanceof AdjustableBasicType<T> indicatorCapable ) {
			final BasicType<T> indicatedType = indicatorCapable.resolveIndicatedType( stdIndicators, domainJtd );
			return indicatedType != null ? indicatedType : resolved;
		}
		else {
			return resolved;
		}
	}

	public static <E extends Enum<E>> BasicValue.Resolution<E> fromEnum(
			EnumJavaType<E> enumJavaType,
			JdbcType explicitJdbcType,
			JdbcTypeIndicators stdIndicators,
			BootstrapContext context) {
		final Field enumeratedValueField = determineEnumeratedValueField( enumJavaType.getJavaTypeClass() );
		if ( enumeratedValueField != null ) {
			validateEnumeratedValue( enumeratedValueField, stdIndicators );
		}

		final JdbcType jdbcType =
				enumJdbcType( enumJavaType, explicitJdbcType, stdIndicators, context, enumeratedValueField );
		final BasicType<E> basicType =
				enumeratedValueField != null
						? createEnumeratedValueJdbcMapping( enumeratedValueField, enumJavaType, jdbcType, context )
						: context.getTypeConfiguration().getBasicTypeRegistry().resolve( enumJavaType, jdbcType );
		context.registerAdHocBasicType( basicType );
		return new InferredBasicValueResolution<>(
				basicType,
				enumJavaType,
				enumJavaType,
				jdbcType,
				basicType,
				ImmutableMutabilityPlan.instance()
		);
	}

	private static <E extends Enum<E>> JdbcType enumJdbcType(
			EnumJavaType<E> enumJavaType,
			JdbcType explicitJdbcType,
			JdbcTypeIndicators stdIndicators,
			BootstrapContext context,
			Field enumeratedValueField) {
		if ( explicitJdbcType != null ) {
			return explicitJdbcType;
		}
		else if ( enumeratedValueField != null ) {
			final JdbcTypeRegistry jdbcTypeRegistry = context.getTypeConfiguration().getJdbcTypeRegistry();
			final Class<?> fieldType = enumeratedValueField.getType();
			if ( String.class.equals( fieldType ) ) {
				return jdbcTypeRegistry.getDescriptor( SqlTypes.VARCHAR );
			}
			else if ( byte.class.equals( fieldType ) ) {
				return jdbcTypeRegistry.getDescriptor( SqlTypes.TINYINT );
			}
			else if ( short.class.equals( fieldType )
					|| int.class.equals( fieldType ) ) {
				return jdbcTypeRegistry.getDescriptor( SMALLINT );
			}
			else {
				throw new IllegalStateException();
			}
		}
		else {
			return enumJavaType.getRecommendedJdbcType( stdIndicators );
		}
	}

	private static <E extends Enum<E>> BasicType<E> createEnumeratedValueJdbcMapping(
			Field enumeratedValueField,
			EnumJavaType<E> enumJavaType,
			JdbcType jdbcType,
			BootstrapContext context) {
		final JavaType<?> relationalJavaType =
				context.getTypeConfiguration().getJavaTypeRegistry()
						.getDescriptor( enumeratedValueField.getType() );
		return new ConvertedBasicTypeImpl<>(
				ConvertedBasicTypeImpl.EXTERNALIZED_PREFIX + enumJavaType.getTypeName(),
				"EnumeratedValue conversion for " + enumJavaType.getTypeName(),
				jdbcType,
				new EnumeratedValueConverter<>( enumJavaType, relationalJavaType, enumeratedValueField )
		);
	}

	private static <E extends Enum<E>> Field determineEnumeratedValueField(Class<? extends E> enumJavaTypeClass) {
		for ( var field : enumJavaTypeClass.getDeclaredFields() ) {
			if ( field.isAnnotationPresent( EnumeratedValue.class ) ) {
				return field;
			}
		}
		return null;
	}

	private static void validateEnumeratedValue(Field enumeratedValueField, JdbcTypeIndicators stdIndicators) {
		final Class<?> fieldType = enumeratedValueField.getType();
		if ( stdIndicators.getEnumeratedType() == EnumType.STRING ) {
			// JPA says only String is valid here
			// todo (7.0) : support char/Character as well
			if ( !String.class.equals( fieldType )
					&& !char.class.equals( fieldType ) ) {
				throw new MappingException(
						String.format(
								Locale.ROOT,
								"@EnumeratedValue for EnumType.STRING must be placed on a field whose type is String or char: %s.%s",
								enumeratedValueField.getDeclaringClass().getName(),
								enumeratedValueField.getName()
						)
				);
			}
		}
		else {
			assert stdIndicators.getEnumeratedType() == null || stdIndicators.getEnumeratedType() == EnumType.ORDINAL;
			// JPA says only byte, short, or int are valid here
			if ( !byte.class.equals( fieldType )
					&& !short.class.equals( fieldType )
					&& !int.class.equals( fieldType ) ) {
				throw new MappingException(
						String.format(
								Locale.ROOT,
								"@EnumeratedValue for EnumType.ORDINAL must be placed on a field whose type is byte, short, or int: %s.%s",
								enumeratedValueField.getDeclaringClass().getName(),
								enumeratedValueField.getName()
						)
				);
			}
		}
	}

	public static <T> BasicValue.Resolution<T> fromTemporal(
			TemporalJavaType<T> reflectedJtd,
			BasicJavaType<?> explicitJavaType,
			JdbcType explicitJdbcType,
			Function<TypeConfiguration, MutabilityPlan<?>> explicitMutabilityPlanAccess,
			JdbcTypeIndicators stdIndicators) {
		final TypeConfiguration typeConfiguration = stdIndicators.getTypeConfiguration();
		final BasicTypeRegistry basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();
		final TemporalType requestedTemporalPrecision = stdIndicators.getTemporalPrecision();

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Case #1 - explicit JavaType

		if ( explicitJavaType != null ) {
			if ( !isTemporal( explicitJavaType ) ) {
				throw new MappingException(
						"Explicit JavaType [" + explicitJavaType +
								"] defined for temporal value must implement TemporalJavaType"
				);
			}

			@SuppressWarnings("unchecked")
			final TemporalJavaType<T> explicitTemporalJtd = (TemporalJavaType<T>) explicitJavaType;
			if ( requestedTemporalPrecision != null && explicitTemporalJtd.getPrecision() != requestedTemporalPrecision ) {
				throw new MappingException(
						"Temporal precision (`jakarta.persistence.TemporalType`) mismatch... requested precision = " + requestedTemporalPrecision +
								"; explicit JavaType (`" + explicitTemporalJtd + "`) precision = " + explicitTemporalJtd.getPrecision()

				);
			}

			final JdbcType jdbcType = explicitJdbcType != null
					? explicitJdbcType
					: explicitTemporalJtd.getRecommendedJdbcType( stdIndicators );
			final BasicType<T> jdbcMapping = basicTypeRegistry.resolve( explicitTemporalJtd, jdbcType );
			return new InferredBasicValueResolution<>(
					jdbcMapping,
					explicitTemporalJtd,
					explicitTemporalJtd,
					jdbcType,
					jdbcMapping,
					determineMutabilityPlan( explicitMutabilityPlanAccess, explicitTemporalJtd, typeConfiguration )
			);
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Case #2 - explicit JdbcType
		//
		// 		- still a special case because we want to perform the new resolution
		// 		due to the new annotations being used

		if ( explicitJdbcType != null ) {
			final TemporalJavaType<T> temporalJavaType =
					requestedTemporalPrecision != null
							? reflectedJtd.resolveTypeForPrecision( requestedTemporalPrecision, typeConfiguration )
							// Avoid using the DateJavaType and prefer the JdbcTimestampJavaType
							: reflectedJtd.resolveTypeForPrecision( reflectedJtd.getPrecision(), typeConfiguration );
			final BasicType<T> jdbcMapping = basicTypeRegistry.resolve( temporalJavaType, explicitJdbcType );
			return new InferredBasicValueResolution<>(
					jdbcMapping,
					temporalJavaType,
					temporalJavaType,
					explicitJdbcType,
					jdbcMapping,
					temporalJavaType.getMutabilityPlan()
			);
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Case #3 - no explicit JavaType or JdbcType
		//
		// 		- for the moment continue to use the legacy resolution to registered
		// 		BasicType

		final BasicType<T> basicType;
		if ( requestedTemporalPrecision != null
				&& requestedTemporalPrecision != reflectedJtd.getPrecision() ) {
			basicType = basicTypeRegistry.resolve(
					reflectedJtd.resolveTypeForPrecision( requestedTemporalPrecision, typeConfiguration ),
					TemporalJavaType.resolveJdbcTypeCode( requestedTemporalPrecision )
			);
		}
		else {
			basicType = basicTypeRegistry.resolve(
					// Avoid using the DateJavaType and prefer the JdbcTimestampJavaType
					reflectedJtd.resolveTypeForPrecision( reflectedJtd.getPrecision(), typeConfiguration ),
					reflectedJtd.getRecommendedJdbcType( stdIndicators )
			);
		}

		return new InferredBasicValueResolution<>(
				basicType,
				basicType.getJavaTypeDescriptor(),
				basicType.getJavaTypeDescriptor(),
				basicType.getJdbcType(),
				basicType,
				determineMutabilityPlan( explicitMutabilityPlanAccess, reflectedJtd, typeConfiguration )
		);
	}

	private static <T> MutabilityPlan<T> determineMutabilityPlan(
			Function<TypeConfiguration, MutabilityPlan<?>> explicitMutabilityPlanAccess,
			JavaType<T> javaType,
			TypeConfiguration typeConfiguration) {
		if ( explicitMutabilityPlanAccess != null ) {
			final MutabilityPlan<?> mutabilityPlan = explicitMutabilityPlanAccess.apply( typeConfiguration );
			if ( mutabilityPlan != null ) {
				//noinspection unchecked
				return (MutabilityPlan<T>) mutabilityPlan;
			}
		}
		return javaType.getMutabilityPlan();
	}

}
