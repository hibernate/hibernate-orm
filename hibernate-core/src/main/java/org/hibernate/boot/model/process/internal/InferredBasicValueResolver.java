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
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.model.convert.internal.NamedEnumValueConverter;
import org.hibernate.metamodel.model.convert.internal.OrdinalEnumValueConverter;
import org.hibernate.type.AdjustableBasicType;
import org.hibernate.type.BasicType;
import org.hibernate.type.CustomType;
import org.hibernate.type.SerializableType;
import org.hibernate.type.descriptor.java.BasicJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.EnumJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.SerializableJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.TemporalJavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptorIndicators;
import org.hibernate.type.descriptor.jdbc.ObjectJdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.TinyIntJdbcTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * BasicValue.Resolution resolver for cases where no explicit
 * type info was supplied.
 */
public class InferredBasicValueResolver {
	/**
	 * Create an inference-based resolution
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static BasicValue.Resolution from(
			Function<TypeConfiguration, BasicJavaTypeDescriptor> explicitJavaTypeAccess,
			Function<TypeConfiguration, JdbcTypeDescriptor> explicitSqlTypeAccess,
			Type resolvedJavaType,
			Supplier<JavaTypeDescriptor> reflectedJtdResolver,
			JdbcTypeDescriptorIndicators stdIndicators,
			Table table,
			Selectable selectable,
			String ownerName,
			String propertyName,
			TypeConfiguration typeConfiguration) {

		final BasicJavaTypeDescriptor explicitJavaType = explicitJavaTypeAccess != null ? explicitJavaTypeAccess.apply( typeConfiguration ) : null;
		final JdbcTypeDescriptor explicitJdbcType = explicitSqlTypeAccess != null ? explicitSqlTypeAccess.apply( typeConfiguration ) : null;

		final BasicJavaTypeDescriptor reflectedJtd = (BasicJavaTypeDescriptor) reflectedJtdResolver.get();

		// NOTE : the distinction that is made below wrt `explicitJavaType` and `reflectedJtd` is
		//		needed temporarily to trigger "legacy resolution" versus "ORM6 resolution.  Yes, it
		//		makes the code a little more complicated but the benefit is well worth it - saving memory

		final BasicType<?> jdbcMapping;
		final BasicType<?> legacyType;

		if ( explicitJavaType != null ) {
			// we have an explicit @JavaType

			if ( explicitJdbcType != null ) {
				// we also have an explicit @SqlType(Code)

				jdbcMapping = typeConfiguration.getBasicTypeRegistry().resolve(
						explicitJavaType,
						explicitJdbcType
				);
				legacyType = jdbcMapping;
			}
			else {
				// we need to infer the JdbcTypeDescriptor and use that to build the value-mapping
				final JdbcTypeDescriptor inferredJdbcType = explicitJavaType.getRecommendedJdbcType( stdIndicators );
				if ( inferredJdbcType instanceof ObjectJdbcTypeDescriptor ) {
					// have a "fallback" JDBC type... see if we can decide a better choice

					if ( explicitJavaType instanceof EnumJavaTypeDescriptor ) {
						return fromEnum(
								(EnumJavaTypeDescriptor) reflectedJtd,
								explicitJavaType,
								null,
								stdIndicators,
								typeConfiguration
						);
					}
					else if ( explicitJavaType instanceof TemporalJavaTypeDescriptor ) {
						return fromTemporal(
								(TemporalJavaTypeDescriptor) reflectedJtd,
								explicitJavaType,
								null,
								resolvedJavaType,
								stdIndicators,
								typeConfiguration
						);
					}
					else if ( explicitJavaType instanceof SerializableJavaTypeDescriptor || explicitJavaType.getJavaType() instanceof Serializable ) {
						legacyType = new SerializableType<>( explicitJavaType );
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
				// we also have an explicit @JdbcType(Code)

				jdbcMapping = typeConfiguration.getBasicTypeRegistry().resolve(
						reflectedJtd,
						explicitJdbcType
				);

				legacyType = jdbcMapping;
			}
			else {
				// Use JTD if we know it to apply any specialized resolutions

				if ( reflectedJtd instanceof EnumJavaTypeDescriptor ) {
					return fromEnum(
							(EnumJavaTypeDescriptor) reflectedJtd,
							null,
							null,
							stdIndicators,
							typeConfiguration
					);
				}
				else if ( reflectedJtd instanceof TemporalJavaTypeDescriptor ) {
					return fromTemporal(
							(TemporalJavaTypeDescriptor) reflectedJtd,
							null,
							null,
							resolvedJavaType,
							stdIndicators,
							typeConfiguration
					);
				}
				else {
					// here we have the legacy case
					//		- we mimic how this used to be done
					final BasicType registeredType = typeConfiguration.getBasicTypeRegistry().getRegisteredType( reflectedJtd.getJavaType() );

					if ( registeredType != null ) {
						legacyType = resolveSqlTypeIndicators( stdIndicators, registeredType, reflectedJtd );
						jdbcMapping = legacyType;
					}
					else if ( reflectedJtd instanceof SerializableJavaTypeDescriptor || reflectedJtd.getJavaType() instanceof Serializable ) {
						legacyType = new SerializableType<>( reflectedJtd );
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
				final BasicJavaTypeDescriptor recommendedJtd = explicitJdbcType.getJdbcRecommendedJavaTypeMapping(
						length,
						scale,
						typeConfiguration
				);
				final BasicType<?> resolved = typeConfiguration.getBasicTypeRegistry().resolve(
						recommendedJtd,
						explicitJdbcType
				);

				jdbcMapping = resolveSqlTypeIndicators( stdIndicators, resolved, recommendedJtd );
				legacyType = jdbcMapping;
			}
			else {
				// we have neither a JTD nor STD

				throw new MappingException(
						"Could not determine JavaTypeDescriptor nor SqlTypeDescriptor to use" +
								" for BasicValue: owner = " + ownerName +
								"; property = " + propertyName +
								"; table = " + table.getName() +
								"; column = " + selectable.getText()
				);
			}
		}

		if ( jdbcMapping == null ) {
			throw new MappingException(
					"Could not determine JavaTypeDescriptor nor SqlTypeDescriptor to use" + "" +
							" for " + ( (BasicValue) stdIndicators ).getResolvedJavaType() +
							"; table = " + table.getName() +
							"; column = " + selectable.getText()
			);
		}

		return new InferredBasicValueResolution(
				jdbcMapping,
				jdbcMapping.getJavaTypeDescriptor(),
				jdbcMapping.getJavaTypeDescriptor(),
				jdbcMapping.getJdbcTypeDescriptor(),
				null,
				legacyType,
				null
		);
	}

	@SuppressWarnings("rawtypes")
	public static BasicType<?> resolveSqlTypeIndicators(
			JdbcTypeDescriptorIndicators stdIndicators,
			BasicType<?> resolved,
			JavaTypeDescriptor<?> domainJtd) {
		if ( resolved instanceof AdjustableBasicType ) {
			final AdjustableBasicType indicatorCapable = (AdjustableBasicType) resolved;
			final BasicType indicatedType = indicatorCapable.resolveIndicatedType( stdIndicators, domainJtd );
			return indicatedType != null ? indicatedType : resolved;
		}
		else {
			return resolved;
		}
	}

	@SuppressWarnings("rawtypes")
	public static InferredBasicValueResolution fromEnum(
			EnumJavaTypeDescriptor enumJavaDescriptor,
			BasicJavaTypeDescriptor explicitJavaType,
			JdbcTypeDescriptor explicitJdbcType,
			JdbcTypeDescriptorIndicators stdIndicators,
			TypeConfiguration typeConfiguration) {
		final EnumType enumStyle = stdIndicators.getEnumeratedType() != null
				? stdIndicators.getEnumeratedType()
				: EnumType.ORDINAL;

		switch ( enumStyle ) {
			case STRING: {
				final JavaTypeDescriptor<?> relationalJtd;
				if ( explicitJavaType != null ) {
					if ( ! String.class.isAssignableFrom( explicitJavaType.getJavaTypeClass() ) ) {
						throw new MappingException(
								"Explicit JavaTypeDescriptor [" + explicitJavaType +
										"] applied to enumerated value with EnumType#STRING" +
										" should handle `java.lang.String` as its relational type descriptor"
						);
					}
					relationalJtd = explicitJavaType;
				}
				else {
					final boolean useCharacter = stdIndicators.getColumnLength() == 1;
					final Class<?> relationalJavaType = useCharacter ? Character.class : String.class;
					relationalJtd = typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( relationalJavaType );
				}

				final JdbcTypeDescriptor jdbcTypeDescriptor = explicitJdbcType != null ? explicitJdbcType : relationalJtd.getRecommendedJdbcType( stdIndicators );

				//noinspection unchecked
				final NamedEnumValueConverter valueConverter = new NamedEnumValueConverter(
						enumJavaDescriptor,
						jdbcTypeDescriptor,
						relationalJtd
				);

				//noinspection unchecked
				final org.hibernate.type.EnumType legacyEnumType = new org.hibernate.type.EnumType(
						enumJavaDescriptor.getJavaTypeClass(),
						valueConverter,
						typeConfiguration
				);

				final CustomType legacyEnumTypeWrapper = new CustomType( legacyEnumType, typeConfiguration );

				final JdbcMapping jdbcMapping = typeConfiguration.getBasicTypeRegistry().resolve( relationalJtd, jdbcTypeDescriptor );

				//noinspection unchecked
				return new InferredBasicValueResolution(
						jdbcMapping,
						enumJavaDescriptor,
						relationalJtd,
						jdbcTypeDescriptor,
						valueConverter,
						legacyEnumTypeWrapper,
						ImmutableMutabilityPlan.INSTANCE
				);
			}
			case ORDINAL: {
				final JavaTypeDescriptor<Integer> relationalJtd;
				if ( explicitJavaType != null ) {
					if ( ! Integer.class.isAssignableFrom( explicitJavaType.getJavaTypeClass() ) ) {
						throw new MappingException(
								"Explicit JavaTypeDescriptor [" + explicitJavaType +
										"] applied to enumerated value with EnumType#ORDINAL" +
										" should handle `java.lang.Integer` as its relational type descriptor"
						);
					}
					//noinspection unchecked
					relationalJtd = explicitJavaType;
				}
				else {
					relationalJtd = typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( Integer.class );
				}

				final JdbcTypeDescriptor jdbcTypeDescriptor = explicitJdbcType != null ? explicitJdbcType : TinyIntJdbcTypeDescriptor.INSTANCE;

				//noinspection unchecked
				final OrdinalEnumValueConverter valueConverter = new OrdinalEnumValueConverter(
						enumJavaDescriptor,
						jdbcTypeDescriptor,
						relationalJtd
				);

				//noinspection unchecked
				final org.hibernate.type.EnumType legacyEnumType = new org.hibernate.type.EnumType(
						enumJavaDescriptor.getJavaTypeClass(),
						valueConverter,
						typeConfiguration
				);

				final CustomType legacyEnumTypeWrapper = new CustomType( legacyEnumType, typeConfiguration );

				final JdbcMapping jdbcMapping = typeConfiguration.getBasicTypeRegistry().resolve( relationalJtd, jdbcTypeDescriptor );

				//noinspection unchecked
				return new InferredBasicValueResolution(
						jdbcMapping,
						enumJavaDescriptor,
						relationalJtd,
						jdbcTypeDescriptor,
						valueConverter,
						legacyEnumTypeWrapper,
						ImmutableMutabilityPlan.INSTANCE
				);
			}
			default: {
				throw new MappingException( "Unknown enumeration-style (JPA EnumType) : " + enumStyle );
			}
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static InferredBasicValueResolution fromTemporal(
			TemporalJavaTypeDescriptor reflectedJtd,
			BasicJavaTypeDescriptor explicitJavaType,
			JdbcTypeDescriptor explicitJdbcType,
			Type resolvedJavaType,
			JdbcTypeDescriptorIndicators stdIndicators,
			TypeConfiguration typeConfiguration) {
		final TemporalType requestedTemporalPrecision = stdIndicators.getTemporalPrecision();

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Case #1 - @JavaType

		if ( explicitJavaType != null ) {
			if ( !TemporalJavaTypeDescriptor.class.isInstance( explicitJavaType ) ) {
				throw new MappingException(
						"Explicit JavaTypeDescriptor [" + explicitJavaType +
								"] defined for temporal value must implement TemporalJavaTypeDescriptor"
				);
			}

			final TemporalJavaTypeDescriptor explicitTemporalJtd = (TemporalJavaTypeDescriptor) explicitJavaType;

			if ( requestedTemporalPrecision != null && explicitTemporalJtd.getPrecision() != requestedTemporalPrecision ) {
				throw new MappingException(
						"Temporal precision (`jakarta.persistence.TemporalType`) mismatch... requested precision = " + requestedTemporalPrecision +
								"; explicit JavaTypeDescriptor (`" + explicitTemporalJtd + "`) precision = " + explicitTemporalJtd.getPrecision()

				);
			}

			final JdbcTypeDescriptor jdbcTypeDescriptor = explicitJdbcType != null ? explicitJdbcType : explicitTemporalJtd.getRecommendedJdbcType( stdIndicators );

			final BasicType jdbcMapping = typeConfiguration.getBasicTypeRegistry().resolve( explicitTemporalJtd, jdbcTypeDescriptor );

			return new InferredBasicValueResolution(
					jdbcMapping,
					explicitTemporalJtd,
					explicitTemporalJtd,
					jdbcTypeDescriptor,
					null,
					jdbcMapping,
					explicitJavaType.getMutabilityPlan()
			);
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Case #2 - SqlType(Code)
		//
		// 		- still a special case because we want to perform the new resolution
		// 		due to the new annotations being used

		if ( explicitJdbcType != null ) {
			final TemporalJavaTypeDescriptor jtd;

			if ( requestedTemporalPrecision != null ) {
				jtd = reflectedJtd.resolveTypeForPrecision(
						requestedTemporalPrecision,
						typeConfiguration
				);
			}
			else {
				jtd = reflectedJtd;
			}

			final BasicType jdbcMapping = typeConfiguration.getBasicTypeRegistry().resolve( jtd, explicitJdbcType );

			return new InferredBasicValueResolution(
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
		// Case #3 - no @JavaType nor @SqlType(Code)
		//
		// 		- for the moment continue to use the legacy resolution to registered
		// 		BasicType
		final BasicType basicType;
		if ( requestedTemporalPrecision != null && requestedTemporalPrecision != reflectedJtd.getPrecision() ) {
			basicType = typeConfiguration.getBasicTypeRegistry().resolve(
					reflectedJtd.resolveTypeForPrecision( requestedTemporalPrecision, typeConfiguration ),
					TemporalJavaTypeDescriptor.resolveJdbcTypeCode( requestedTemporalPrecision )
			);
		}
		else {
			basicType = typeConfiguration.getBasicTypeRegistry().resolve(
					reflectedJtd,
					reflectedJtd.getRecommendedJdbcType( stdIndicators )
			);
		}

		return new InferredBasicValueResolution(
				basicType,
				basicType.getJavaTypeDescriptor(),
				basicType.getJavaTypeDescriptor(),
				basicType.getJdbcTypeDescriptor(),
				null,
				basicType,
				reflectedJtd.getMutabilityPlan()
		);
	}

}
