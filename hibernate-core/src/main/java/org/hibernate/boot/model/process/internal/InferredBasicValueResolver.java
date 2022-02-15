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
import jakarta.persistence.EnumType;
import jakarta.persistence.TemporalType;

import org.hibernate.MappingException;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Table;
import org.hibernate.metamodel.model.convert.internal.NamedEnumValueConverter;
import org.hibernate.metamodel.model.convert.internal.OrdinalEnumValueConverter;
import org.hibernate.type.AdjustableBasicType;
import org.hibernate.type.BasicType;
import org.hibernate.type.CustomType;
import org.hibernate.type.SerializableType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.java.EnumJavaType;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.SerializableJavaType;
import org.hibernate.type.descriptor.java.TemporalJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.descriptor.jdbc.ObjectJdbcType;
import org.hibernate.type.spi.TypeConfiguration;

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
			JdbcTypeIndicators stdIndicators,
			Table table,
			Selectable selectable,
			String ownerName,
			String propertyName,
			TypeConfiguration typeConfiguration) {
		final JavaType<T> reflectedJtd = reflectedJtdResolver.get();

		// NOTE : the distinction that is made below wrt `explicitJavaType` and `reflectedJtd` is
		//		needed temporarily to trigger "legacy resolution" versus "ORM6 resolution.  Yes, it
		//		makes the code a little more complicated but the benefit is well worth it - saving memory

		final BasicType<T> jdbcMapping;
		final BasicType<T> legacyType;

		if ( explicitJavaType != null ) {
			// we have an explicit JavaType

			if ( explicitJdbcType != null ) {
				// we also have an explicit JdbcType

				jdbcMapping = typeConfiguration.getBasicTypeRegistry().resolve(
						explicitJavaType,
						explicitJdbcType
				);
				legacyType = jdbcMapping;
			}
			else {
				if ( explicitJavaType instanceof TemporalJavaType ) {
					return fromTemporal(
							(TemporalJavaType<T>) explicitJavaType,
							null,
							null,
							resolvedJavaType,
							stdIndicators,
							typeConfiguration
					);
				}
				// we need to infer the JdbcType and use that to build the value-mapping
				final JdbcType inferredJdbcType = explicitJavaType.getRecommendedJdbcType( stdIndicators );
				if ( inferredJdbcType instanceof ObjectJdbcType ) {
					// have a "fallback" JDBC type... see if we can decide a better choice

					if ( explicitJavaType instanceof EnumJavaType ) {
						return fromEnum(
								(EnumJavaType) reflectedJtd,
								explicitJavaType,
								null,
								stdIndicators,
								typeConfiguration
						);
					}
					else if ( explicitJavaType instanceof TemporalJavaType ) {
						return fromTemporal(
								(TemporalJavaType<T>) reflectedJtd,
								explicitJavaType,
								null,
								resolvedJavaType,
								stdIndicators,
								typeConfiguration
						);
					}
					else if ( explicitJavaType instanceof SerializableJavaType
							|| explicitJavaType.getJavaType() instanceof Serializable ) {
						legacyType = new SerializableType( explicitJavaType );
						jdbcMapping = legacyType;
					}
					else {
						jdbcMapping = typeConfiguration.getBasicTypeRegistry().resolve(
								explicitJavaType,
								inferredJdbcType
						);
						legacyType = jdbcMapping;
					}
				}
				else {
					jdbcMapping = typeConfiguration.getBasicTypeRegistry().resolve(
							explicitJavaType,
							inferredJdbcType
					);
					legacyType = jdbcMapping;
				}
			}
		}
		else if ( reflectedJtd != null ) {
			// we were able to determine the "reflected java-type"
			if ( explicitJdbcType != null ) {
				// we also have an explicit JdbcType

				jdbcMapping = typeConfiguration.getBasicTypeRegistry().resolve(
						reflectedJtd,
						explicitJdbcType
				);

				legacyType = jdbcMapping;
			}
			else {
				// Use JTD if we know it to apply any specialized resolutions

				if ( reflectedJtd instanceof EnumJavaType ) {
					return fromEnum(
							(EnumJavaType) reflectedJtd,
							null,
							null,
							stdIndicators,
							typeConfiguration
					);
				}
				else if ( reflectedJtd instanceof TemporalJavaType ) {
					return fromTemporal(
							(TemporalJavaType<T>) reflectedJtd,
							null,
							null,
							resolvedJavaType,
							stdIndicators,
							typeConfiguration
					);
				}
				else {
					// see if there is a registered BasicType for this JavaType and, if so, use it.
					// this mimics the legacy handling
					final BasicType<T> registeredType = typeConfiguration.getBasicTypeRegistry()
							.getRegisteredType( reflectedJtd.getJavaType() );

					if ( registeredType != null ) {
						// so here is the legacy resolution
						legacyType = resolveSqlTypeIndicators( stdIndicators, registeredType, reflectedJtd );
						jdbcMapping = legacyType;
					}
					else {
						// there was not a "legacy" BasicType registration,  so use `JavaType#getRecommendedJdbcType`, if
						// one, to create a mapping
						final JdbcType recommendedJdbcType = reflectedJtd.getRecommendedJdbcType( stdIndicators );
						if ( recommendedJdbcType != null ) {
							jdbcMapping = typeConfiguration.getBasicTypeRegistry().resolve(
									reflectedJtd,
									recommendedJdbcType
							);
							legacyType = jdbcMapping;
						}
						else if ( reflectedJtd instanceof SerializableJavaType
								|| Serializable.class.isAssignableFrom( reflectedJtd.getJavaTypeClass() ) ) {
							legacyType = new SerializableType( reflectedJtd );
							jdbcMapping = legacyType;
						}
						else {
							// let this fall through to the exception creation below
							legacyType = null;
							jdbcMapping = null;
						}
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

				final BasicJavaType<T> recommendedJtd = explicitJdbcType.getJdbcRecommendedJavaTypeMapping(
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
						recommendedJtd );
				legacyType = jdbcMapping;
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
				null,
				legacyType,
				null
		);
	}

	/**
	 * Create an inference-based resolution
	 */
	public static <T> BasicValue.Resolution<T> from(
			Function<TypeConfiguration, BasicJavaType> explicitJavaTypeAccess,
			Function<TypeConfiguration, JdbcType> explicitSqlTypeAccess,
			Type resolvedJavaType,
			Supplier<JavaType<T>> reflectedJtdResolver,
			JdbcTypeIndicators stdIndicators,
			Table table,
			Selectable selectable,
			String ownerName,
			String propertyName,
			TypeConfiguration typeConfiguration) {

		final BasicJavaType<T> explicitJavaType = explicitJavaTypeAccess != null
				? explicitJavaTypeAccess.apply( typeConfiguration )
				: null;
		final JdbcType explicitJdbcType = explicitSqlTypeAccess
				!= null ? explicitSqlTypeAccess.apply( typeConfiguration )
				: null;
		return InferredBasicValueResolver.from(
				explicitJavaType,
				explicitJdbcType,
				resolvedJavaType,
				reflectedJtdResolver,
				stdIndicators,
				table,
				selectable,
				ownerName,
				propertyName,
				typeConfiguration
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

	public static <E extends Enum<E>> InferredBasicValueResolution<E,?> fromEnum(
			EnumJavaType<E> enumJavaType,
			BasicJavaType<?> explicitJavaType,
			JdbcType explicitJdbcType,
			JdbcTypeIndicators stdIndicators,
			TypeConfiguration typeConfiguration) {
		final EnumType enumStyle = stdIndicators.getEnumeratedType() != null
				? stdIndicators.getEnumeratedType()
				: EnumType.ORDINAL;

		switch ( enumStyle ) {
			case STRING: {
				return stringEnumValueResolution(
						enumJavaType,
						explicitJavaType,
						explicitJdbcType,
						stdIndicators,
						typeConfiguration
				);
			}
			case ORDINAL: {
				return ordinalEnumValueResolution(
						enumJavaType,
						explicitJavaType,
						explicitJdbcType,
						typeConfiguration
				);
			}
			default: {
				throw new MappingException( "Unknown enumeration-style (JPA EnumType) : " + enumStyle );
			}
		}
	}

	private static <E extends Enum<E>> InferredBasicValueResolution<E, Integer> ordinalEnumValueResolution(
			EnumJavaType<E> enumJavaType,
			BasicJavaType<?> explicitJavaType,
			JdbcType explicitJdbcType,
			TypeConfiguration typeConfiguration) {
		final JavaType<Integer> relationalJtd;
		if ( explicitJavaType != null ) {
			if ( ! Integer.class.isAssignableFrom( explicitJavaType.getJavaTypeClass() ) ) {
				throw new MappingException(
						"Explicit JavaType [" + explicitJavaType +
								"] applied to enumerated value with EnumType#ORDINAL" +
								" should handle `java.lang.Integer` as its relational type descriptor"
				);
			}
			//noinspection unchecked
			relationalJtd = (BasicJavaType<Integer>) explicitJavaType;
		}
		else {
			relationalJtd = typeConfiguration.getJavaTypeRegistry().getDescriptor( Integer.class );
		}

		final JdbcType jdbcType = explicitJdbcType != null
				? explicitJdbcType
				: typeConfiguration.getJdbcTypeRegistry().getDescriptor( SqlTypes.TINYINT );

		final OrdinalEnumValueConverter<E> valueConverter = new OrdinalEnumValueConverter<>(
				enumJavaType,
				jdbcType,
				relationalJtd
		);

		return new InferredBasicValueResolution<>(
				typeConfiguration.getBasicTypeRegistry().resolve(relationalJtd, jdbcType),
				enumJavaType,
				relationalJtd,
				jdbcType,
				valueConverter,
				new CustomType<>(
						new org.hibernate.type.EnumType<>(
								enumJavaType.getJavaTypeClass(),
								valueConverter,
								typeConfiguration
						),
						typeConfiguration
				),
				ImmutableMutabilityPlan.instance()
		);
	}

	private static <E extends Enum<E>> InferredBasicValueResolution<E, String> stringEnumValueResolution(
			EnumJavaType<E> enumJavaType,
			BasicJavaType<?> explicitJavaType,
			JdbcType explicitJdbcType,
			JdbcTypeIndicators stdIndicators,
			TypeConfiguration typeConfiguration) {
		final JavaType<String> relationalJtd;
		if ( explicitJavaType != null ) {
			if ( ! String.class.isAssignableFrom( explicitJavaType.getJavaTypeClass() ) ) {
				throw new MappingException(
						"Explicit JavaType [" + explicitJavaType +
								"] applied to enumerated value with EnumType#STRING" +
								" should handle `java.lang.String` as its relational type descriptor"
				);
			}
			//noinspection unchecked
			relationalJtd = (BasicJavaType<String>) explicitJavaType;
		}
		else {
			final boolean useCharacter = stdIndicators.getColumnLength() == 1;
			relationalJtd = typeConfiguration.getJavaTypeRegistry()
					.getDescriptor( useCharacter ? Character.class : String.class );
		}

		final JdbcType jdbcType = explicitJdbcType != null
				? explicitJdbcType
				: relationalJtd.getRecommendedJdbcType(stdIndicators);

		final NamedEnumValueConverter<E> valueConverter = new NamedEnumValueConverter<>(
				enumJavaType,
				jdbcType,
				relationalJtd
		);

		return new InferredBasicValueResolution<>(
				typeConfiguration.getBasicTypeRegistry().resolve(relationalJtd, jdbcType),
				enumJavaType,
				relationalJtd,
				jdbcType,
				valueConverter,
				new CustomType<>(
						new org.hibernate.type.EnumType<>(
								enumJavaType.getJavaTypeClass(),
								valueConverter,
								typeConfiguration
						),
						typeConfiguration
				),
				ImmutableMutabilityPlan.instance()
		);
	}

	public static <T> InferredBasicValueResolution<T,T> fromTemporal(
			TemporalJavaType<T> reflectedJtd,
			BasicJavaType<?> explicitJavaType,
			JdbcType explicitJdbcType,
			Type resolvedJavaType,
			JdbcTypeIndicators stdIndicators,
			TypeConfiguration typeConfiguration) {
		final TemporalType requestedTemporalPrecision = stdIndicators.getTemporalPrecision();

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Case #1 - explicit JavaType

		if ( explicitJavaType != null ) {
			if ( !(explicitJavaType instanceof TemporalJavaType) ) {
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

			return new InferredBasicValueResolution<>(
					jdbcMapping,
					explicitTemporalJtd,
					explicitTemporalJtd,
					jdbcType,
					null,
					jdbcMapping,
					explicitTemporalJtd.getMutabilityPlan()
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
					null,
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
				null,
				basicType,
				reflectedJtd.getMutabilityPlan()
		);
	}

}
