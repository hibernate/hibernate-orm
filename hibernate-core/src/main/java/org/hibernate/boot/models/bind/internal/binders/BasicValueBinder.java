/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.annotations.CollectionIdJavaType;
import org.hibernate.annotations.CollectionIdJdbcType;
import org.hibernate.annotations.CollectionIdJdbcTypeCode;
import org.hibernate.annotations.CollectionIdMutability;
import org.hibernate.annotations.CollectionIdType;
import org.hibernate.annotations.JavaType;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.ListIndexJavaType;
import org.hibernate.annotations.ListIndexJdbcType;
import org.hibernate.annotations.ListIndexJdbcTypeCode;
import org.hibernate.annotations.MapKeyJavaType;
import org.hibernate.annotations.MapKeyJdbcType;
import org.hibernate.annotations.MapKeyJdbcTypeCode;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.TimeZoneColumn;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.annotations.AnyKeyJavaType;
import org.hibernate.annotations.AnyKeyJdbcType;
import org.hibernate.annotations.AnyKeyJdbcTypeCode;
import org.hibernate.boot.model.convert.spi.RegisteredConversion;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.models.AnnotationPlacementException;
import org.hibernate.boot.models.bind.internal.sources.BasicValueSource;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.bind.spi.TableReference;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Property;
import org.hibernate.models.ModelsException;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.MapKeyTemporal;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import static jakarta.persistence.EnumType.ORDINAL;
import static org.hibernate.annotations.TimeZoneStorageType.AUTO;
import static org.hibernate.annotations.TimeZoneStorageType.COLUMN;

/// Applies source-model basic-value details to an `org.hibernate.mapping.BasicValue`.
///
/// The same `BasicValue` mapping type is used for normal basic attributes,
/// identifier parts, collection elements, map keys, and list indexes.  The
/// [BasicValueSource] records which source role is being bound so this binder
/// can read the correct annotation family, such as `@Enumerated` versus
/// `@MapKeyEnumerated`, or normal Java/JDBC type annotations versus list-index
/// and map-key variants.
///
/// Conversion is also centralized here.  That keeps explicit `@Convert`
/// handling tied to the source descriptor rather than scattered across each
/// caller that happens to create a basic value.
///
/// @since 9.0
/// @author Steve Ebersole
public class BasicValueBinder {

	public static void bindBasicValue(
			BasicValueSource source,
			Property property,
			BasicValue basicValue,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		basicValue.setImplicitJavaTypeAccess( (typeConfiguration) -> source.javaType() );
		bindConversion( source, property, basicValue, bindingOptions, bindingState, bindingContext );
		bindJavaType( source, property, basicValue, bindingOptions, bindingState, bindingContext );
		bindJdbcType( source, property, basicValue, bindingOptions, bindingState, bindingContext );
		bindMutability( source, property, basicValue, bindingOptions, bindingState, bindingContext );
		bindLob( source.member(), property, basicValue, bindingOptions, bindingState, bindingContext );
		bindNationalized( source.member(), property, basicValue, bindingOptions, bindingState, bindingContext );
		bindEnumerated( source, property, basicValue, bindingOptions, bindingState, bindingContext );
		bindTemporalPrecision( source, property, basicValue, bindingOptions, bindingState, bindingContext );
		bindTimeZoneStorage( source.member(), property, basicValue, bindingOptions, bindingState, bindingContext );
		bindCustomType( source, property, basicValue, bindingOptions, bindingState, bindingContext );
	}

	public static void bindConversion(
			BasicValueSource source,
			Property property,
			BasicValue basicValue,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final Convert conversion = source.conversion();
		if ( conversion == null || conversion.disableConversion() ) {
			return;
		}

		validateConversionAttributeName( source, conversion );

		//noinspection unchecked
		final Class<AttributeConverter<?, ?>> javaClass = (Class<AttributeConverter<?, ?>>) conversion.converter();
		basicValue.setJpaAttributeConverterDescriptor(
				new RegisteredConversion( source.javaType(), javaClass, false ).getConverterDescriptor()
		);
	}

	private static void validateConversionAttributeName(BasicValueSource source, Convert conversion) {
		final String attributeName = conversion.attributeName();
		if ( attributeName == null || attributeName.isEmpty() ) {
			return;
		}

		switch ( source.kind() ) {
			case MAP_KEY -> {
				if ( "key".equals( attributeName ) ) {
					return;
				}
			}
			case COLLECTION_ELEMENT -> {
				if ( "value".equals( attributeName ) ) {
					return;
				}
			}
			case EMBEDDABLE_MEMBER -> {
				return;
			}
			default -> {
			}
		}

		throw new ModelsException(
				"@Convert#attributeName did not match basic value source role - "
						+ source.member().getName() + "#" + source.kind()
		);
	}

	public static void bindJavaType(
			BasicValueSource source,
			Property property,
			BasicValue basicValue,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final var member = source.member();
		switch ( source.kind() ) {
			case MAP_KEY -> {
				final var javaTypeAnn = member.getDirectAnnotationUsage( MapKeyJavaType.class );
				if ( javaTypeAnn != null ) {
					applyJavaType( member, basicValue, javaTypeAnn.value() );
				}
			}
			case LIST_INDEX -> {
				final var javaTypeAnn = member.getDirectAnnotationUsage( ListIndexJavaType.class );
				if ( javaTypeAnn != null ) {
					applyJavaType( member, basicValue, javaTypeAnn.value() );
				}
			}
			case ANY_KEY -> {
				final var javaTypeAnn = member.getDirectAnnotationUsage( AnyKeyJavaType.class );
				if ( javaTypeAnn != null ) {
					applyJavaType( member, basicValue, javaTypeAnn.value() );
				}
			}
			case COLLECTION_ID -> {
				final var javaTypeAnn = member.getDirectAnnotationUsage( CollectionIdJavaType.class );
				if ( javaTypeAnn != null ) {
					applyJavaType( member, basicValue, javaTypeAnn.value() );
				}
			}
			default -> bindJavaType( member, property, basicValue, bindingOptions, bindingState, bindingContext );
		}
	}

	public static void bindJdbcType(
			BasicValueSource source,
			Property property,
			BasicValue basicValue,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final var member = source.member();
		switch ( source.kind() ) {
			case MAP_KEY -> {
				final var jdbcTypeAnn = member.getDirectAnnotationUsage( MapKeyJdbcType.class );
				final var jdbcTypeCodeAnn = member.getDirectAnnotationUsage( MapKeyJdbcTypeCode.class );
				bindExplicitJdbcType( member, basicValue, jdbcTypeAnn == null ? null : jdbcTypeAnn.value(), jdbcTypeCodeAnn == null ? null : jdbcTypeCodeAnn.value() );
			}
			case LIST_INDEX -> {
				final var jdbcTypeAnn = member.getDirectAnnotationUsage( ListIndexJdbcType.class );
				final var jdbcTypeCodeAnn = member.getDirectAnnotationUsage( ListIndexJdbcTypeCode.class );
				bindExplicitJdbcType( member, basicValue, jdbcTypeAnn == null ? null : jdbcTypeAnn.value(), jdbcTypeCodeAnn == null ? null : jdbcTypeCodeAnn.value() );
			}
			case ANY_KEY -> {
				final var jdbcTypeAnn = member.getDirectAnnotationUsage( AnyKeyJdbcType.class );
				final var jdbcTypeCodeAnn = member.getDirectAnnotationUsage( AnyKeyJdbcTypeCode.class );
				bindExplicitJdbcType( member, basicValue, jdbcTypeAnn == null ? null : jdbcTypeAnn.value(), jdbcTypeCodeAnn == null ? null : jdbcTypeCodeAnn.value() );
			}
			case COLLECTION_ID -> {
				final var jdbcTypeAnn = member.getDirectAnnotationUsage( CollectionIdJdbcType.class );
				final var jdbcTypeCodeAnn = member.getDirectAnnotationUsage( CollectionIdJdbcTypeCode.class );
				bindExplicitJdbcType( member, basicValue, jdbcTypeAnn == null ? null : jdbcTypeAnn.value(), jdbcTypeCodeAnn == null ? null : jdbcTypeCodeAnn.value() );
			}
			case ANY_DISCRIMINATOR -> {
				final var jdbcTypeAnn = member.getDirectAnnotationUsage( JdbcType.class );
				final var jdbcTypeCodeAnn = member.getDirectAnnotationUsage( JdbcTypeCode.class );
				bindExplicitJdbcType( member, basicValue, jdbcTypeAnn == null ? null : jdbcTypeAnn.value(), jdbcTypeCodeAnn == null ? null : jdbcTypeCodeAnn.value() );
			}
			default -> bindJdbcType( member, property, basicValue, bindingOptions, bindingState, bindingContext );
		}
	}

	public static void bindEnumerated(
			BasicValueSource source,
			Property property,
			BasicValue basicValue,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		if ( source.kind() == BasicValueSource.Kind.MAP_KEY ) {
			final MapKeyEnumerated mapKeyEnumerated = source.member().getDirectAnnotationUsage( MapKeyEnumerated.class );
			if ( mapKeyEnumerated != null ) {
				basicValue.setEnumerationStyle( mapKeyEnumerated.value() );
			}
			return;
		}

		bindEnumerated( source.member(), property, basicValue, bindingOptions, bindingState, bindingContext );
	}

	public static void bindTemporalPrecision(
			BasicValueSource source,
			Property property,
			BasicValue basicValue,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		if ( source.kind() == BasicValueSource.Kind.MAP_KEY ) {
			final MapKeyTemporal mapKeyTemporal = source.member().getDirectAnnotationUsage( MapKeyTemporal.class );
			if ( mapKeyTemporal != null ) {
				basicValue.setTemporalPrecision( mapKeyTemporal.value() );
			}
			return;
		}

		bindTemporalPrecision( source.member(), property, basicValue, bindingOptions, bindingState, bindingContext );
	}

	public static void bindJavaType(
			MemberDetails member,
			Property property,
			BasicValue basicValue,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		// todo : do we need to account for JavaTypeRegistration here?
		final var javaTypeAnn = member.getDirectAnnotationUsage( JavaType.class );
		if ( javaTypeAnn == null ) {
			return;
		}

		applyJavaType( member, basicValue, javaTypeAnn.value() );
	}

	private static void applyJavaType(
			MemberDetails member,
			BasicValue basicValue,
			Class<? extends BasicJavaType<?>> javaTypeClass) {
		basicValue.setExplicitJavaTypeAccess( (typeConfiguration) -> {
			final Class<BasicJavaType<?>> javaClass = (Class<BasicJavaType<?>>) javaTypeClass;
			try {
				return javaClass.getConstructor().newInstance();
			}
			catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
				final ModelsException modelsException = new ModelsException( "Error instantiating local @JavaType - " + member.getName() );
				modelsException.addSuppressed( e );
				throw modelsException;
			}
		} );
	}

	public static void bindJdbcType(
			MemberDetails member,
			Property property,
			BasicValue basicValue,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		// todo : do we need to account for JdbcTypeRegistration here?
		final var jdbcTypeAnn = member.getDirectAnnotationUsage( JdbcType.class );
		final var jdbcTypeCodeAnn = member.getDirectAnnotationUsage( JdbcTypeCode.class );
		bindExplicitJdbcType(
				member,
				basicValue,
				jdbcTypeAnn == null ? null : jdbcTypeAnn.value(),
				jdbcTypeCodeAnn == null ? null : jdbcTypeCodeAnn.value()
		);
	}

	private static void bindExplicitJdbcType(
			MemberDetails member,
			BasicValue basicValue,
			Class<? extends org.hibernate.type.descriptor.jdbc.JdbcType> jdbcTypeClass,
			Integer jdbcTypeCode) {
		if ( jdbcTypeClass != null ) {
			if ( jdbcTypeCode != null ) {
				throw new AnnotationPlacementException(
						"Illegal combination of @JdbcType and @JdbcTypeCode - " + member.getName()
				);
			}

			basicValue.setExplicitJdbcTypeAccess( (typeConfiguration) -> {
				final Class<org.hibernate.type.descriptor.jdbc.JdbcType> javaClass = (Class<org.hibernate.type.descriptor.jdbc.JdbcType>) jdbcTypeClass;
				try {
					return javaClass.getConstructor().newInstance();
				}
				catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
					final ModelsException modelsException = new ModelsException( "Error instantiating local @JdbcType - " + member.getName() );
					modelsException.addSuppressed( e );
					throw modelsException;
				}
			} );
		}
		else if ( jdbcTypeCode != null ) {
			basicValue.setExplicitJdbcTypeCode( jdbcTypeCode );
			basicValue.setExplicitJdbcTypeAccess( (typeConfiguration) -> {
				final var jdbcTypeRegistry = typeConfiguration.getJdbcTypeRegistry();
				return jdbcTypeRegistry.getConstructor( jdbcTypeCode ) == null
						? jdbcTypeRegistry.getDescriptor( jdbcTypeCode )
						: null;
			} );
		}
	}

	public static void bindMutability(
			BasicValueSource source,
			Property property,
			BasicValue basicValue,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		if ( source.kind() != BasicValueSource.Kind.COLLECTION_ID ) {
			return;
		}

		final var mutabilityAnn = source.member().getDirectAnnotationUsage( CollectionIdMutability.class );
		if ( mutabilityAnn == null ) {
			return;
		}

		final var mutabilityClass = mutabilityAnn.value();
		basicValue.setExplicitMutabilityPlanAccess( (typeConfiguration) -> instantiateMutabilityPlan(
				source.member(),
				mutabilityClass
		) );
	}

	private static MutabilityPlan<?> instantiateMutabilityPlan(
			MemberDetails member,
			Class<? extends MutabilityPlan<?>> mutabilityClass) {
		try {
			return mutabilityClass.getConstructor().newInstance();
		}
		catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			final ModelsException modelsException = new ModelsException(
					"Error instantiating local @CollectionIdMutability - " + member.getName()
			);
			modelsException.addSuppressed( e );
			throw modelsException;
		}
	}

	public static void bindCustomType(
			BasicValueSource source,
			Property property,
			BasicValue basicValue,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		if ( source.kind() != BasicValueSource.Kind.COLLECTION_ID ) {
			return;
		}

		final var typeAnn = source.member().getDirectAnnotationUsage( CollectionIdType.class );
		if ( typeAnn == null ) {
			return;
		}

		basicValue.setExplicitTypeParams( extractParameterMap( typeAnn.parameters() ) );
		basicValue.setTypeAnnotation( typeAnn );
		basicValue.setExplicitCustomType( typeAnn.value() );
	}

	private static Map<String, String> extractParameterMap(org.hibernate.annotations.Parameter[] parameters) {
		final Map<String, String> result = new HashMap<>();
		for ( org.hibernate.annotations.Parameter parameter : parameters ) {
			result.put( parameter.name(), parameter.value() );
		}
		return result;
	}

	public static void bindNationalized(
			MemberDetails member,
			Property property,
			BasicValue basicValue,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		if ( member.hasDirectAnnotationUsage( Nationalized.class ) ) {
			basicValue.makeNationalized();
		}
	}

	public static void bindLob(
			MemberDetails member,
			Property property,
			BasicValue basicValue,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		if ( member.hasDirectAnnotationUsage( Lob.class ) ) {
			basicValue.makeLob();
		}
	}

	public static void bindEnumerated(
			MemberDetails member,
			Property property,
			BasicValue basicValue,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final Enumerated enumerated = member.getDirectAnnotationUsage( Enumerated.class );
		if ( enumerated == null ) {
			return;
		}

		basicValue.setEnumerationStyle( enumerated.value() == null ? ORDINAL : enumerated.value() );
	}

	public static void bindTemporalPrecision(
			MemberDetails member,
			Property property,
			BasicValue basicValue,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final Temporal temporalAnn = member.getDirectAnnotationUsage( Temporal.class );
		if ( temporalAnn == null ) {
			return;
		}

		//noinspection deprecation
		final TemporalType precision = temporalAnn.value();
		basicValue.setTemporalPrecision( precision );
	}

	public static void bindTimeZoneStorage(
			MemberDetails member,
			Property property,
			BasicValue basicValue,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final TimeZoneStorage storageAnn = member.getDirectAnnotationUsage( TimeZoneStorage.class );
		final TimeZoneColumn columnAnn = member.getDirectAnnotationUsage( TimeZoneColumn.class );
		if ( storageAnn != null ) {
			final TimeZoneStorageType strategy = storageAnn.value() == null ? AUTO : storageAnn.value();
			if ( strategy != COLUMN && columnAnn != null ) {
				throw new AnnotationPlacementException(
						"Illegal combination of @TimeZoneStorage(" + strategy.name() + ") and @TimeZoneColumn"
				);
			}
			basicValue.setTimeZoneStorageType( strategy );
		}

		if ( columnAnn != null ) {
			final org.hibernate.mapping.Column column = (org.hibernate.mapping.Column) basicValue.getColumn();
			column.setName( columnAnn.name().isEmpty() ? property.getName() + "_tz" : columnAnn.name() );
			column.setSqlType( columnAnn.columnDefinition().isEmpty() ? null : columnAnn.columnDefinition() );

			final var tableName = columnAnn.table().isEmpty() ? null : columnAnn.table();
			TableReference tableByName = null;
			if ( tableName != null ) {
				final Identifier identifier = Identifier.toIdentifier( tableName );
				tableByName = bindingState.getTableByName( identifier.getCanonicalName() );
				basicValue.setTable( tableByName.binding() );
			}

			property.setInsertable( columnAnn.insertable() );
			property.setUpdatable( columnAnn.updatable() );
		}
	}

}
