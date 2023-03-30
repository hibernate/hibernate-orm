/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.process.internal;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.MappingException;
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
import org.hibernate.type.SerializableType;
import org.hibernate.type.descriptor.converter.internal.NamedEnumValueConverter;
import org.hibernate.type.descriptor.converter.internal.OrdinalEnumValueConverter;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.java.BasicPluralJavaType;
import org.hibernate.type.descriptor.java.EnumJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.JavaTypeHelper;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.SerializableJavaType;
import org.hibernate.type.descriptor.java.TemporalJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.descriptor.jdbc.ObjectJdbcType;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.EnumType;
import jakarta.persistence.TemporalType;

import static org.hibernate.type.SqlTypes.SMALLINT;
import static org.hibernate.type.SqlTypes.TINYINT;

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
			Function<TypeConfiguration, MutabilityPlan> explicitMutabilityPlanAccess,
			JdbcTypeIndicators stdIndicators,
			Table table,
			Selectable selectable,
			String ownerName,
			String propertyName,
			MetadataBuildingContext buildingContext) {
		final Dialect dialect = buildingContext.getMetadataCollector().getDatabase().getDialect();
		final TypeConfiguration typeConfiguration = buildingContext.getBootstrapContext().getTypeConfiguration();

		final JavaType<T> reflectedJtd = reflectedJtdResolver.get();

		// NOTE : the distinction that is made below wrt `explicitJavaType` and `reflectedJtd` is
		//		needed temporarily to trigger "legacy resolution" versus "ORM6 resolution.  Yes, it
		//		makes the code a little more complicated but the benefit is well worth it - saving memory

		final BasicType<T> jdbcMapping;

		if ( explicitJavaType != null ) {
			// we have an explicit JavaType
			if ( explicitJavaType instanceof EnumJavaType ) {
				return fromEnum(
						(EnumJavaType) explicitJavaType,
						null,
						explicitJdbcType,
						stdIndicators,
						buildingContext
				);
			}
			else if ( JavaTypeHelper.isTemporal( explicitJavaType ) ) {
				return fromTemporal(
						(TemporalJavaType<T>) explicitJavaType,
						null,
						explicitJdbcType,
						resolvedJavaType,
						explicitMutabilityPlanAccess,
						stdIndicators,
						typeConfiguration
				);
			}
			else if ( explicitJdbcType != null ) {
				// we also have an explicit JdbcType

				jdbcMapping = typeConfiguration.getBasicTypeRegistry().resolve(
						explicitJavaType,
						explicitJdbcType
				);
			}
			else {
				// we need to infer the JdbcType and use that to build the value-mapping
				final JdbcType inferredJdbcType = explicitJavaType.getRecommendedJdbcType( stdIndicators );
				if ( inferredJdbcType instanceof ObjectJdbcType && ( explicitJavaType instanceof SerializableJavaType
						|| explicitJavaType.getJavaType() instanceof Serializable ) ) {
					// Use the SerializableType if possible since ObjectJdbcType is our fallback
					jdbcMapping = new SerializableType( explicitJavaType );
				}
				else {
					jdbcMapping = resolveSqlTypeIndicators(
							stdIndicators,
							typeConfiguration.getBasicTypeRegistry().resolve(
									explicitJavaType,
									inferredJdbcType
							),
							explicitJavaType
					);
				}
			}
		}
		else if ( reflectedJtd != null ) {
			// we were able to determine the "reflected java-type"
			// Use JTD if we know it to apply any specialized resolutions

			if ( reflectedJtd instanceof EnumJavaType ) {
				return fromEnum(
						(EnumJavaType) reflectedJtd,
						null,
						explicitJdbcType,
						stdIndicators,
						buildingContext
				);
			}
			else if ( JavaTypeHelper.isTemporal( reflectedJtd ) ) {
				return fromTemporal(
						(TemporalJavaType<T>) reflectedJtd,
						null,
						explicitJdbcType,
						resolvedJavaType,
						explicitMutabilityPlanAccess,
						stdIndicators,
						typeConfiguration
				);
			}
			else if ( explicitJdbcType != null ) {
				// we also have an explicit JdbcType

				jdbcMapping = typeConfiguration.getBasicTypeRegistry().resolve(
						reflectedJtd,
						explicitJdbcType
				);
			}
			else {
				// see if there is a registered BasicType for this JavaType and, if so, use it.
				// this mimics the legacy handling
				final BasicType registeredType;
				if ( reflectedJtd instanceof BasicPluralJavaType<?> ) {
					final BasicPluralJavaType<?> containerJtd = (BasicPluralJavaType<?>) reflectedJtd;
					final JavaType<?> elementJtd = containerJtd.getElementJavaType();
					final BasicType registeredElementType;
					if ( elementJtd instanceof EnumJavaType ) {
						final EnumeratedValueResolution<?,?> resolution = fromEnum(
								(EnumJavaType<?>) elementJtd,
								null,
								null,
								stdIndicators,
								buildingContext
						);
						registeredElementType = resolution.getJdbcMapping();
					}
					else if ( JavaTypeHelper.isTemporal( elementJtd ) ) {
						final InferredBasicValueResolution resolution = InferredBasicValueResolver.fromTemporal(
								(TemporalJavaType<T>) elementJtd,
								null,
								null,
								resolvedJavaType,
								explicitMutabilityPlanAccess,
								stdIndicators,
								typeConfiguration
						);
						registeredElementType = resolution.getLegacyResolvedBasicType();
					}
					else {
						registeredElementType = typeConfiguration.getBasicTypeRegistry()
								.getRegisteredType( elementJtd.getJavaType() );
					}
					final ColumnTypeInformation columnTypeInformation;
					if ( selectable instanceof ColumnTypeInformation ) {
						columnTypeInformation = (ColumnTypeInformation) selectable;
					}
					else {
						columnTypeInformation = null;
					}
					registeredType = registeredElementType == null ? null : containerJtd.resolveType(
							typeConfiguration,
							dialect,
							resolveSqlTypeIndicators( stdIndicators, registeredElementType, elementJtd ),
							columnTypeInformation,
							stdIndicators
					);
					if ( registeredType instanceof BasicPluralType<?, ?> ) {
						typeConfiguration.getBasicTypeRegistry().register( registeredType );
					}
				}
				else {
					registeredType = typeConfiguration.getBasicTypeRegistry()
							.getRegisteredType( reflectedJtd.getJavaType() );
				}

				if ( registeredType != null ) {
					// so here is the legacy resolution
					jdbcMapping = resolveSqlTypeIndicators( stdIndicators, registeredType, reflectedJtd );
				}
				else {
					// there was not a "legacy" BasicType registration,  so use `JavaType#getRecommendedJdbcType`, if
					// one, to create a mapping
					final JdbcType recommendedJdbcType = reflectedJtd.getRecommendedJdbcType( stdIndicators );
					if ( recommendedJdbcType != null ) {
						jdbcMapping = resolveSqlTypeIndicators(
								stdIndicators,
								typeConfiguration.getBasicTypeRegistry().resolve(
										reflectedJtd,
										recommendedJdbcType
								),
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
				//		- NOTE : yes its an odd case, but its easy to implement here, so...
				Integer length = null;
				Integer scale = null;
				if ( selectable instanceof Column ) {
					final Column column = (Column) selectable;
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

				final JavaType<T> recommendedJtd = explicitJdbcType.getJdbcRecommendedJavaTypeMapping(
						length,
						scale,
						typeConfiguration
				);

				jdbcMapping = resolveSqlTypeIndicators(
						stdIndicators,
						typeConfiguration.getBasicTypeRegistry().resolve(
								recommendedJtd,
								explicitJdbcType
						),
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
					"Could not determine JavaType nor JdbcType to use" + "" +
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

	public static <T> BasicType<T> resolveSqlTypeIndicators(
			JdbcTypeIndicators stdIndicators,
			BasicType<T> resolved,
			JavaType<T> domainJtd) {
		if ( resolved instanceof AdjustableBasicType ) {
			final AdjustableBasicType<T> indicatorCapable = (AdjustableBasicType<T>) resolved;
			final BasicType<T> indicatedType = indicatorCapable.resolveIndicatedType( stdIndicators, domainJtd );
			return indicatedType != null ? indicatedType : resolved;
		}
		else {
			return resolved;
		}
	}

	public static <E extends Enum<E>, R> EnumeratedValueResolution<E,R> fromEnum(
			EnumJavaType<E> enumJavaType,
			BasicJavaType<R> explicitJavaType,
			JdbcType explicitJdbcType,
			JdbcTypeIndicators stdIndicators,
			MetadataBuildingContext context) {
		final EnumType enumStyle = stdIndicators.getEnumeratedType();

		if ( enumStyle == EnumType.STRING ) {
			//noinspection unchecked
			return (EnumeratedValueResolution<E, R>) stringEnumValueResolution(
					enumJavaType,
					explicitJavaType,
					explicitJdbcType,
					stdIndicators,
					context
			);
		}

		if ( enumStyle == EnumType.ORDINAL ) {
			//noinspection unchecked
			return (EnumeratedValueResolution<E, R>) ordinalEnumValueResolution(
					enumJavaType,
					(BasicJavaType<? extends Number>)explicitJavaType,
					explicitJdbcType,
					context
			);
		}

		if ( enumStyle == null ) {
			// NOTE : separate from the explicit ORDINAL check to facilitate
			// handling native database enum types.  In theory anyway - atm
			// we cannot discern an implicit (default value) or explicit style
			// due to HCANN and annotation handling for default values

			//noinspection unchecked
			return (EnumeratedValueResolution<E, R>) ordinalEnumValueResolution(
					enumJavaType,
					(BasicJavaType<? extends Number>)explicitJavaType,
					explicitJdbcType,
					context
			);
		}

		throw new MappingException( "Unknown enumeration-style (JPA EnumType) : " + enumStyle );
	}

	private static <E extends Enum<E>, N extends Number> EnumeratedValueResolution<E,N> ordinalEnumValueResolution(
			EnumJavaType<E> enumJavaType,
			BasicJavaType<N> explicitJavaType,
			JdbcType explicitJdbcType,
			MetadataBuildingContext context) {
		final JdbcType jdbcType = ordinalJdbcType( explicitJdbcType, enumJavaType, context );
		final JavaType<N> relationalJavaType = ordinalJavaType( explicitJavaType, jdbcType, context );

		return new EnumeratedValueResolution<>(
				jdbcType,
				new OrdinalEnumValueConverter<>( enumJavaType, jdbcType, relationalJavaType ),
				context
		);
	}

	private static JdbcType ordinalJdbcType(
			JdbcType explicitJdbcType,
			EnumJavaType<?> enumJavaType,
			MetadataBuildingContext context) {
		return explicitJdbcType != null
				? explicitJdbcType
				: context.getMetadataCollector().getTypeConfiguration().getJdbcTypeRegistry().getDescriptor( enumJavaType.hasManyValues() ? SMALLINT : TINYINT );
	}

	private static <N extends Number> JavaType<N> ordinalJavaType(
			JavaType<N> explicitJavaType,
			JdbcType jdbcType,
			MetadataBuildingContext context) {
		if ( explicitJavaType != null ) {
			if ( !Integer.class.isAssignableFrom( explicitJavaType.getJavaTypeClass() ) ) {
				throw new MappingException(
						"Explicit JavaType [" + explicitJavaType +
								"] applied to enumerated value with EnumType#ORDINAL" +
								" should handle `java.lang.Integer` as its relational type descriptor"
				);
			}
			return explicitJavaType;
		}
		else {
			return jdbcType.getJdbcRecommendedJavaTypeMapping(
					null,
					null,
					context.getMetadataCollector().getTypeConfiguration()
			);
		}
	}

	private static <E extends Enum<E>> EnumeratedValueResolution<E,String> stringEnumValueResolution(
			EnumJavaType<E> enumJavaType,
			BasicJavaType<?> explicitJavaType,
			JdbcType explicitJdbcType,
			JdbcTypeIndicators stdIndicators,
			MetadataBuildingContext context) {
		final JdbcType jdbcType = explicitJdbcType == null
				? enumJavaType.getRecommendedJdbcType( stdIndicators )
				: explicitJdbcType;
		final JavaType<String> relationalJtd = stringJavaType( explicitJavaType, stdIndicators, context );

		return new EnumeratedValueResolution<>(
				jdbcType,
				new NamedEnumValueConverter<>( enumJavaType, jdbcType, relationalJtd ),
				context
		);
	}

	private static JdbcType stringJdbcType(JdbcType explicitJdbcType, JdbcTypeIndicators stdIndicators, JavaType<String> relationalJtd) {
		return explicitJdbcType != null
				? explicitJdbcType
				: relationalJtd.getRecommendedJdbcType( stdIndicators );
	}

	private static JavaType<String> stringJavaType(
			BasicJavaType<?> explicitJavaType,
			JdbcTypeIndicators stdIndicators,
			MetadataBuildingContext context) {
		if ( explicitJavaType != null ) {
			if ( ! String.class.isAssignableFrom( explicitJavaType.getJavaTypeClass() ) ) {
				throw new MappingException(
						"Explicit JavaType [" + explicitJavaType +
								"] applied to enumerated value with EnumType#STRING" +
								" should handle `java.lang.String` as its relational type descriptor"
				);
			}
			return (JavaType<String>) explicitJavaType;
		}
		else {
			return context.getMetadataCollector().getTypeConfiguration().getJavaTypeRegistry()
					.getDescriptor( stdIndicators.getColumnLength() == 1 ? Character.class : String.class );
		}
	}

	public static <T> InferredBasicValueResolution<T,T> fromTemporal(
			TemporalJavaType<T> reflectedJtd,
			BasicJavaType<?> explicitJavaType,
			JdbcType explicitJdbcType,
			Type resolvedJavaType,
			Function<TypeConfiguration, MutabilityPlan> explicitMutabilityPlanAccess,
			JdbcTypeIndicators stdIndicators,
			TypeConfiguration typeConfiguration) {
		final TemporalType requestedTemporalPrecision = stdIndicators.getTemporalPrecision();

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Case #1 - explicit JavaType

		if ( explicitJavaType != null ) {
			if ( !JavaTypeHelper.isTemporal( explicitJavaType ) ) {
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

			final BasicType<T> jdbcMapping = typeConfiguration.getBasicTypeRegistry().resolve( explicitTemporalJtd, jdbcType );

			final MutabilityPlan<T> mutabilityPlan = determineMutabilityPlan( explicitMutabilityPlanAccess, explicitTemporalJtd, typeConfiguration );
			return new InferredBasicValueResolution<>(
					jdbcMapping,
					explicitTemporalJtd,
					explicitTemporalJtd,
					jdbcType,
					jdbcMapping,
					mutabilityPlan
			);
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Case #2 - explicit JdbcType
		//
		// 		- still a special case because we want to perform the new resolution
		// 		due to the new annotations being used

		if ( explicitJdbcType != null ) {
			final TemporalJavaType<T> jtd;

			if ( requestedTemporalPrecision != null ) {
				jtd = reflectedJtd.resolveTypeForPrecision(
						requestedTemporalPrecision,
						typeConfiguration
				);
			}
			else {
				jtd = reflectedJtd;
			}

			final BasicType<T> jdbcMapping = typeConfiguration.getBasicTypeRegistry().resolve( jtd, explicitJdbcType );

			return new InferredBasicValueResolution<>(
					jdbcMapping,
					jtd,
					jtd,
					explicitJdbcType,
					jdbcMapping,
					jtd.getMutabilityPlan()
			);
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Case #3 - no explicit JavaType or JdbcType
		//
		// 		- for the moment continue to use the legacy resolution to registered
		// 		BasicType
		final BasicType<T> basicType;
		if ( requestedTemporalPrecision != null && requestedTemporalPrecision != reflectedJtd.getPrecision() ) {
			basicType = typeConfiguration.getBasicTypeRegistry().resolve(
					reflectedJtd.resolveTypeForPrecision( requestedTemporalPrecision, typeConfiguration ),
					TemporalJavaType.resolveJdbcTypeCode( requestedTemporalPrecision )
			);
		}
		else {
			basicType = typeConfiguration.getBasicTypeRegistry().resolve(
					reflectedJtd,
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

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static <T> MutabilityPlan<T> determineMutabilityPlan(
			Function<TypeConfiguration, MutabilityPlan> explicitMutabilityPlanAccess,
			JavaType<T> jtd,
			TypeConfiguration typeConfiguration) {
		if ( explicitMutabilityPlanAccess != null ) {
			final MutabilityPlan<T> mutabilityPlan = explicitMutabilityPlanAccess.apply( typeConfiguration );
			if ( mutabilityPlan != null ) {
				return mutabilityPlan;
			}
		}
		return jtd.getMutabilityPlan();
	}

}
