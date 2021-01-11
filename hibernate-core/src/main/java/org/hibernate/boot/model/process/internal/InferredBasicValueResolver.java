/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.process.internal;

import java.util.function.Function;
import java.util.function.Supplier;
import javax.persistence.EnumType;
import javax.persistence.TemporalType;

import org.hibernate.MappingException;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Table;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.model.convert.internal.NamedEnumValueConverter;
import org.hibernate.metamodel.model.convert.internal.OrdinalEnumValueConverter;
import org.hibernate.metamodel.model.domain.AllowableTemporalParameterType;
import org.hibernate.type.BasicType;
import org.hibernate.type.CustomType;
import org.hibernate.type.SqlTypeDescriptorIndicatorCapable;
import org.hibernate.type.descriptor.java.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.EnumJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.TemporalJavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptorIndicators;
import org.hibernate.type.descriptor.sql.TinyIntTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class InferredBasicValueResolver {
	/**
	 * Create an inference-based resolution
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static BasicValue.Resolution from(
			Function<TypeConfiguration, BasicJavaDescriptor> explicitJavaTypeAccess,
			Function<TypeConfiguration, SqlTypeDescriptor> explicitSqlTypeAccess,
			Supplier<JavaTypeDescriptor> reflectedJtdResolver,
			SqlTypeDescriptorIndicators stdIndicators,
			Table table,
			Selectable selectable,
			String ownerName,
			String propertyName,
			TypeConfiguration typeConfiguration) {

		final BasicJavaDescriptor explicitJavaType = explicitJavaTypeAccess != null ? explicitJavaTypeAccess.apply( typeConfiguration ) : null;
		final SqlTypeDescriptor explicitSqlType = explicitSqlTypeAccess != null ? explicitSqlTypeAccess.apply( typeConfiguration ) : null;

		final BasicJavaDescriptor reflectedJtd = (BasicJavaDescriptor) reflectedJtdResolver.get();

		// NOTE : the distinction that is made below wrt `explicitJavaType` and `reflectedJtd` is
		//		needed temporarily to trigger "legacy resolution" versus "ORM6 resolution.  Yes, it
		//		makes the code a little more complicated but the benefit is well worth it - saving memory

		final BasicType<?> jdbcMapping;
		final BasicType<?> legacyType;

		if ( explicitJavaType != null ) {
			// we have an explicit @JavaType

			if ( explicitSqlType != null ) {
				// we also have an explicit @SqlType(Code)

				jdbcMapping = typeConfiguration.getBasicTypeRegistry().resolve(
						explicitJavaType,
						explicitSqlType
				);
			}
			else {
				// infer the STD
				final SqlTypeDescriptor inferredStd = explicitJavaType.getJdbcRecommendedSqlType( stdIndicators );
				jdbcMapping = typeConfiguration.getBasicTypeRegistry().resolve(
						explicitJavaType,
						inferredStd
				);
			}

			legacyType = jdbcMapping;
		}
		else if ( reflectedJtd != null ) {
			// we were able to determine the "reflected java-type"

			if ( explicitSqlType != null ) {
				// we also have an explicit @SqlType(Code)

				jdbcMapping = typeConfiguration.getBasicTypeRegistry().resolve(
						reflectedJtd,
						explicitSqlType
				);

				legacyType = jdbcMapping;
			}
			else {
				// here we have the legacy case
				//		- we mimic how this used to be done
				final BasicType registeredType = typeConfiguration.getBasicTypeRegistry().getRegisteredType( reflectedJtd.getJavaType() );
				legacyType = resolveSqlTypeIndicators( stdIndicators, registeredType );

				// reuse the "legacy type"
				jdbcMapping = legacyType;
			}
		}
		else {
			if ( explicitSqlType != null ) {
				// we have an explicit STD, but no JTD - infer JTD
				//		- NOTE : yes its an odd case, but its easy to implement here, so...
				final BasicJavaDescriptor recommendedJtd = explicitSqlType.getJdbcRecommendedJavaTypeMapping( typeConfiguration );
				final BasicType<?> resolved = typeConfiguration.getBasicTypeRegistry().resolve(
						recommendedJtd,
						explicitSqlType
				);

				jdbcMapping = resolveSqlTypeIndicators( stdIndicators, resolved );
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
							" for " + ( (BasicValue) stdIndicators ).getResolvedJavaClass() +
							"; table = " + table.getName() +
							"; column = " + selectable.getText()
			);
		}

		return new InferredBasicValueResolution(
				jdbcMapping,
				jdbcMapping.getJavaTypeDescriptor(),
				jdbcMapping.getJavaTypeDescriptor(),
				jdbcMapping.getSqlTypeDescriptor(),
				null,
				legacyType,
				null
		);
	}

	@SuppressWarnings("rawtypes")
	private static BasicType<?> resolveSqlTypeIndicators(
			SqlTypeDescriptorIndicators stdIndicators,
			BasicType<?> resolved) {
		if ( resolved instanceof SqlTypeDescriptorIndicatorCapable ) {
			final SqlTypeDescriptorIndicatorCapable indicatorCapable = (SqlTypeDescriptorIndicatorCapable) resolved;
			final BasicType indicatedType = indicatorCapable.resolveIndicatedType( stdIndicators );
			return indicatedType != null ? indicatedType : resolved;
		}
		else {
			return resolved;
		}
	}

	@SuppressWarnings("rawtypes")
	public static InferredBasicValueResolution fromEnum(
			EnumJavaTypeDescriptor enumJavaDescriptor,
			BasicJavaDescriptor explicitJavaType,
			SqlTypeDescriptor explicitSqlType,
			SqlTypeDescriptorIndicators stdIndicators,
			TypeConfiguration typeConfiguration) {
		final EnumType enumStyle = stdIndicators.getEnumeratedType() != null
				? stdIndicators.getEnumeratedType()
				: EnumType.ORDINAL;

		switch ( enumStyle ) {
			case STRING: {
				final JavaTypeDescriptor<?> relationalJtd;
				if ( explicitJavaType != null ) {
					if ( ! String.class.isAssignableFrom( explicitJavaType.getJavaType() ) ) {
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

				final SqlTypeDescriptor std = explicitSqlType != null ? explicitSqlType : relationalJtd.getJdbcRecommendedSqlType( stdIndicators );

				//noinspection unchecked
				final NamedEnumValueConverter valueConverter = new NamedEnumValueConverter(
						enumJavaDescriptor,
						std,
						relationalJtd
				);

				//noinspection unchecked
				final org.hibernate.type.EnumType legacyEnumType = new org.hibernate.type.EnumType(
						enumJavaDescriptor.getJavaType(),
						valueConverter,
						typeConfiguration
				);

				final CustomType legacyEnumTypeWrapper = new CustomType( legacyEnumType, typeConfiguration );

				final JdbcMapping jdbcMapping = typeConfiguration.getBasicTypeRegistry().resolve( relationalJtd, std );

				//noinspection unchecked
				return new InferredBasicValueResolution(
						jdbcMapping,
						enumJavaDescriptor,
						relationalJtd,
						std,
						valueConverter,
						legacyEnumTypeWrapper,
						ImmutableMutabilityPlan.INSTANCE
				);
			}
			case ORDINAL: {
				final JavaTypeDescriptor<Integer> relationalJtd;
				if ( explicitJavaType != null ) {
					if ( ! Integer.class.isAssignableFrom( explicitJavaType.getJavaType() ) ) {
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

				final SqlTypeDescriptor std = explicitSqlType != null ? explicitSqlType : TinyIntTypeDescriptor.INSTANCE;

				//noinspection unchecked
				final OrdinalEnumValueConverter valueConverter = new OrdinalEnumValueConverter(
						enumJavaDescriptor,
						std,
						relationalJtd
				);

				//noinspection unchecked
				final org.hibernate.type.EnumType legacyEnumType = new org.hibernate.type.EnumType(
						enumJavaDescriptor.getJavaType(),
						valueConverter,
						typeConfiguration
				);

				final CustomType legacyEnumTypeWrapper = new CustomType( legacyEnumType, typeConfiguration );

				final JdbcMapping jdbcMapping = typeConfiguration.getBasicTypeRegistry().resolve( relationalJtd, std );

				//noinspection unchecked
				return new InferredBasicValueResolution(
						jdbcMapping,
						enumJavaDescriptor,
						relationalJtd,
						std,
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
			Function<TypeConfiguration, BasicJavaDescriptor> explicitJavaTypeAccess,
			Function<TypeConfiguration, SqlTypeDescriptor> explicitSqlTypeAccess,
			SqlTypeDescriptorIndicators stdIndicators,
			TypeConfiguration typeConfiguration) {
		final TemporalType requestedTemporalPrecision = stdIndicators.getTemporalPrecision();

		final JavaTypeDescriptor explicitJavaType;
		if ( explicitJavaTypeAccess != null ) {
			explicitJavaType = explicitJavaTypeAccess.apply( typeConfiguration );
		}
		else {
			explicitJavaType = null;
		}

		final SqlTypeDescriptor explicitSqlType;
		if ( explicitSqlTypeAccess != null ) {
			explicitSqlType = explicitSqlTypeAccess.apply( typeConfiguration );
		}
		else {
			explicitSqlType = null;
		}

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
						"Temporal precision (`javax.persistence.TemporalType`) mismatch... requested precision = " + requestedTemporalPrecision +
								"; explicit JavaTypeDescriptor (`" + explicitTemporalJtd + "`) precision = " + explicitTemporalJtd.getPrecision()

				);
			}

			final SqlTypeDescriptor std = explicitSqlType != null ? explicitSqlType : explicitTemporalJtd.getJdbcRecommendedSqlType( stdIndicators );

			final BasicType jdbcMapping = typeConfiguration.getBasicTypeRegistry().resolve( explicitTemporalJtd, std );

			return new InferredBasicValueResolution(
					jdbcMapping,
					explicitTemporalJtd,
					explicitTemporalJtd,
					std,
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

		if ( explicitSqlType != null ) {
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

			final BasicType jdbcMapping = typeConfiguration.getBasicTypeRegistry().resolve( jtd, explicitSqlType );

			return new InferredBasicValueResolution(
					jdbcMapping,
					jtd,
					jtd,
					explicitSqlType,
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

		final BasicType registeredType = typeConfiguration.getBasicTypeRegistry().getRegisteredType( reflectedJtd.getJavaType() );
		final AllowableTemporalParameterType legacyTemporalType = (AllowableTemporalParameterType) registeredType;

		final BasicType basicType;
		if ( requestedTemporalPrecision != null ) {
			basicType = (BasicType) legacyTemporalType.resolveTemporalPrecision(
					requestedTemporalPrecision,
					typeConfiguration
			);
		}
		else {
			basicType = registeredType;
		}

		return new InferredBasicValueResolution(
				basicType,
				basicType.getJavaTypeDescriptor(),
				basicType.getJavaTypeDescriptor(),
				basicType.getSqlTypeDescriptor(),
				null,
				basicType,
				reflectedJtd.getMutabilityPlan()
		);
	}

}
