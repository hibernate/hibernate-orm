/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.hibernate.annotations.CollectionIdJavaType;
import org.hibernate.annotations.CollectionIdJdbcType;
import org.hibernate.annotations.CollectionIdJdbcTypeCode;
import org.hibernate.annotations.CollectionIdMutability;
import org.hibernate.annotations.CollectionIdType;
import org.hibernate.annotations.Collate;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.FractionalSeconds;
import org.hibernate.annotations.GeneratedColumn;
import org.hibernate.annotations.JavaType;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.ListIndexJavaType;
import org.hibernate.annotations.ListIndexJdbcType;
import org.hibernate.annotations.ListIndexJdbcTypeCode;
import org.hibernate.annotations.MapKeyJavaType;
import org.hibernate.annotations.MapKeyJdbcType;
import org.hibernate.annotations.MapKeyJdbcTypeCode;
import org.hibernate.annotations.MapKeyMutability;
import org.hibernate.annotations.MapKeyType;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.PartitionKey;
import org.hibernate.annotations.TimeZoneColumn;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.AnyKeyJavaType;
import org.hibernate.annotations.AnyKeyJdbcType;
import org.hibernate.annotations.AnyKeyJdbcTypeCode;
import org.hibernate.annotations.ValueGenerationType;
import org.hibernate.AnnotationException;
import org.hibernate.MappingException;
import org.hibernate.boot.internal.AnyKeyType;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.convert.spi.RegisteredConversion;
import org.hibernate.boot.model.relational.ExportableProducer;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.models.AnnotationPlacementException;
import org.hibernate.boot.mapping.internal.sources.BasicValueSource;
import org.hibernate.boot.mapping.internal.context.BindingContext;
import org.hibernate.boot.mapping.internal.context.BindingOptions;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.boot.mapping.internal.relational.TableReference;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.generator.AnnotationBasedGenerator;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.Generator;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.id.Configurable;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.GeneratorCreator;
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
import static org.hibernate.boot.models.internal.DialectOverrideAnnotationHelper.getOverridableAnnotation;

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
		basicValue.setMemberDetails( source.member() );
		basicValue.setImplicitSourceJavaType( source.sourceJavaType() );
		bindConversion( source, property, basicValue, bindingOptions, bindingState, bindingContext );
		bindJavaType( source, property, basicValue, bindingOptions, bindingState, bindingContext );
		bindJdbcType( source, property, basicValue, bindingOptions, bindingState, bindingContext );
		bindMutability( source, property, basicValue, bindingOptions, bindingState, bindingContext );
		bindLob( source.member(), property, basicValue, bindingOptions, bindingState, bindingContext );
		bindNationalized( source.member(), property, basicValue, bindingOptions, bindingState, bindingContext );
		bindPartitionKey( source.member(), property, basicValue, bindingOptions, bindingState, bindingContext );
		bindEnumerated( source, property, basicValue, bindingOptions, bindingState, bindingContext );
		bindTemporalPrecision( source, property, basicValue, bindingOptions, bindingState, bindingContext );
		bindTimeZoneStorage( source.member(), property, basicValue, bindingOptions, bindingState, bindingContext );
		bindCustomType( source, property, basicValue, bindingOptions, bindingState, bindingContext );
		bindColumnDefault( source, basicValue );
		bindCollation( source, basicValue );
		bindFractionalSeconds( source, basicValue );
		bindGeneratedColumn( source, basicValue );
		bindValueGeneration( source, property, basicValue, bindingContext );
	}

	public static void bindConversion(
			BasicValueSource source,
			Property property,
			BasicValue basicValue,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final Convert conversion = source.conversion();
		if ( conversion != null && conversion.disableConversion() ) {
			return;
		}

		if ( conversion != null ) {
			validateConversionAttributeName( source, conversion );

			//noinspection unchecked
			final Class<AttributeConverter<?, ?>> javaClass = (Class<AttributeConverter<?, ?>>) conversion.converter();
			basicValue.setJpaAttributeConverterDescriptor(
					new RegisteredConversion( source.rawJavaType(), javaClass, false ).getConverterDescriptor()
			);
			return;
		}

		final ConverterDescriptor<?, ?> autoApplyConverter = resolveAutoApplyConverter(
				source,
				bindingState.getMetadataBuildingContext()
		);
		if ( autoApplyConverter != null ) {
			basicValue.setJpaAttributeConverterDescriptor( autoApplyConverter );
		}
	}

	private static ConverterDescriptor<?, ?> resolveAutoApplyConverter(
			BasicValueSource source,
			MetadataBuildingContext metadataBuildingContext) {
		final var autoApplyHandler = metadataBuildingContext.getMetadataCollector()
				.getConverterRegistry()
				.getAttributeConverterAutoApplyHandler();
		return switch ( source.kind() ) {
			case ATTRIBUTE, EMBEDDABLE_MEMBER, IDENTIFIER ->
					autoApplyHandler.findAutoApplyConverterForAttribute( source.member(), metadataBuildingContext );
			case COLLECTION_ELEMENT ->
					autoApplyHandler.findAutoApplyConverterForCollectionElement( source.member(), metadataBuildingContext );
			case MAP_KEY ->
					autoApplyHandler.findAutoApplyConverterForMapKey( source.member(), metadataBuildingContext );
			default -> null;
		};
	}

	private static void bindGeneratedColumn(BasicValueSource source, BasicValue basicValue) {
		if ( !supportsAttributeOrEmbeddableMember( source ) ) {
			return;
		}

		final GeneratedColumn generatedColumn = getOverridableAnnotation(
				source.member(),
				GeneratedColumn.class,
				basicValue.getBuildingContext().getMetadataCollector().getDatabase().getDialect(),
				basicValue.getBuildingContext().getBootstrapContext().getModelsContext()
		);
		if ( generatedColumn == null ) {
			return;
		}

		if ( basicValue.getColumnSpan() != 1 ) {
			throw new AnnotationException(
					"'@GeneratedColumn' may only be applied to single-column mappings but '"
							+ source.member().getName() + "' maps to " + basicValue.getColumnSpan() + " columns"
			);
		}

		final var selectable = basicValue.getColumn();
		if ( selectable instanceof org.hibernate.mapping.Column column ) {
			column.setGeneratedAs( generatedColumn.value() );
		}
		else {
			throw new AnnotationException(
					"'@GeneratedColumn' may only be applied to column mappings but '"
							+ source.member().getName() + "' maps to a formula"
			);
		}
	}

	private static void bindColumnDefault(BasicValueSource source, BasicValue basicValue) {
		if ( !supportsAttributeOrEmbeddableMember( source ) ) {
			return;
		}

		final ColumnDefault columnDefault = getOverridableAnnotation(
				source.member(),
				ColumnDefault.class,
				basicValue.getBuildingContext().getMetadataCollector().getDatabase().getDialect(),
				basicValue.getBuildingContext().getBootstrapContext().getModelsContext()
		);
		if ( columnDefault == null ) {
			return;
		}

		column( source, basicValue, ColumnDefault.class ).setDefaultValue( columnDefault.value() );
	}

	private static void bindCollation(BasicValueSource source, BasicValue basicValue) {
		if ( !supportsAttributeOrEmbeddableMember( source ) ) {
			return;
		}

		final Collate collate = source.member().getDirectAnnotationUsage( Collate.class );
		if ( collate == null ) {
			return;
		}

		for ( org.hibernate.mapping.Column column : basicValue.getColumns() ) {
			column.setCollation( collate.value() );
		}
	}

	@SuppressWarnings({"deprecation", "removal"})
	private static void bindFractionalSeconds(BasicValueSource source, BasicValue basicValue) {
		if ( !supportsAttributeOrEmbeddableMember( source ) ) {
			return;
		}

		final FractionalSeconds fractionalSeconds = source.member().getDirectAnnotationUsage( FractionalSeconds.class );
		if ( fractionalSeconds == null ) {
			return;
		}

		column( source, basicValue, FractionalSeconds.class ).setTemporalPrecision( fractionalSeconds.value() );
	}

	private static org.hibernate.mapping.Column column(
			BasicValueSource source,
			BasicValue basicValue,
			Class<? extends Annotation> annotationType) {
		if ( basicValue.getColumnSpan() != 1 ) {
			throw new AnnotationException(
					"'@" + annotationType.getSimpleName() + "' may only be applied to single-column mappings but '"
							+ source.member().getName() + "' maps to " + basicValue.getColumnSpan() + " columns"
			);
		}

		final var selectable = basicValue.getColumn();
		if ( selectable instanceof org.hibernate.mapping.Column column ) {
			return column;
		}

		throw new AnnotationException(
				"'@" + annotationType.getSimpleName() + "' may only be applied to column mappings but '"
						+ source.member().getName() + "' maps to a formula"
		);
	}

	private static void bindValueGeneration(
			BasicValueSource source,
			Property property,
			BasicValue basicValue,
			BindingContext bindingContext) {
		if ( !supportsAttributeOrEmbeddableMember( source ) || property == null ) {
			return;
		}

		final var generatorAnnotations = source.member().getMetaAnnotated(
				ValueGenerationType.class,
				bindingContext.getBootstrapContext().getModelsContext()
		);
		if ( generatorAnnotations.isEmpty() ) {
			return;
		}
		if ( generatorAnnotations.size() > 1 ) {
			throw new AnnotationException(
					"Property '" + property.getName() + "' has too many generator annotations: " + generatorAnnotations
			);
		}

		property.setValueGeneratorCreator( generatorCreator(
				source.member(),
				generatorAnnotations.get( 0 )
		) );
	}

	private static boolean supportsAttributeOrEmbeddableMember(BasicValueSource source) {
		return source.kind() == BasicValueSource.Kind.ATTRIBUTE
				|| source.kind() == BasicValueSource.Kind.EMBEDDABLE_MEMBER;
	}

	private static <A extends Annotation> GeneratorCreator generatorCreator(
			MemberDetails member,
			A annotation) {
		@SuppressWarnings("unchecked")
		final Class<A> annotationType = (Class<A>) annotation.annotationType();
		final ValueGenerationType generatorAnnotation = annotationType.getAnnotation( ValueGenerationType.class );
		final Class<? extends Generator> generatorClass = generatorAnnotation.generatedBy();
		checkGeneratorClass( generatorClass );
		return (creationContext) -> {
			final Generator generator = instantiateGenerator( annotation, member, annotationType, creationContext, generatorClass );
			callInitialize( annotation, creationContext, generator );
			callConfigure( creationContext, generator );
			checkVersionGenerationAlways( member, generator );
			return generator;
		};
	}

	private static void checkGeneratorClass(Class<? extends Generator> generatorClass) {
		if ( !BeforeExecutionGenerator.class.isAssignableFrom( generatorClass )
				&& !OnExecutionGenerator.class.isAssignableFrom( generatorClass ) ) {
			throw new MappingException( "Generator class '" + generatorClass.getName()
					+ "' must implement either 'BeforeExecutionGenerator' or 'OnExecutionGenerator'" );
		}
	}

	private static <G extends Generator, A extends Annotation> G instantiateGenerator(
			A annotation,
			MemberDetails member,
			Class<A> annotationType,
			GeneratorCreationContext creationContext,
			Class<? extends G> generatorClass) {
		try {
			G generator = construct(
					generatorClass,
					annotationType,
					annotation,
					Member.class,
					member.toJavaMember(),
					GeneratorCreationContext.class,
					creationContext
			);
			if ( generator != null ) {
				return generator;
			}

			generator = construct( generatorClass, annotationType, annotation, GeneratorCreationContext.class, creationContext );
			if ( generator != null ) {
				return generator;
			}

			generator = construct( generatorClass, annotationType, annotation );
			if ( generator != null ) {
				return generator;
			}

			generator = construct( generatorClass, GeneratorCreationContext.class, creationContext );
			if ( generator != null ) {
				return generator;
			}

			final var constructor = generatorClass.getDeclaredConstructor();
			constructor.setAccessible( true );
			return constructor.newInstance();
		}
		catch (NoSuchMethodException e) {
			throw new org.hibernate.InstantiationException(
					"No appropriate constructor for generator class",
					generatorClass
			);
		}
		catch (InvocationTargetException | InstantiationException | IllegalAccessException | IllegalArgumentException e) {
			throw new org.hibernate.InstantiationException(
					"Could not instantiate generator",
					generatorClass,
					e
			);
		}
	}

	private static <G> G construct(
			Class<? extends G> target,
			Class<?> parameterType1,
			Object argument1,
			Class<?> parameterType2,
			Object argument2) throws InvocationTargetException, InstantiationException, IllegalAccessException {
		try {
			final var constructor = target.getDeclaredConstructor( parameterType1, parameterType2 );
			constructor.setAccessible( true );
			return constructor.newInstance( argument1, argument2 );
		}
		catch (NoSuchMethodException e) {
			return null;
		}
	}

	private static <G> G construct(
			Class<? extends G> target,
			Class<?> parameterType1,
			Object argument1,
			Class<?> parameterType2,
			Object argument2,
			Class<?> parameterType3,
			Object argument3) throws InvocationTargetException, InstantiationException, IllegalAccessException {
		try {
			final var constructor = target.getDeclaredConstructor( parameterType1, parameterType2, parameterType3 );
			constructor.setAccessible( true );
			return constructor.newInstance( argument1, argument2, argument3 );
		}
		catch (NoSuchMethodException e) {
			return null;
		}
	}

	private static <G> G construct(
			Class<? extends G> target,
			Class<?> parameterType,
			Object argument) throws InvocationTargetException, InstantiationException, IllegalAccessException {
		try {
			final var constructor = target.getDeclaredConstructor( parameterType );
			constructor.setAccessible( true );
			return constructor.newInstance( argument );
		}
		catch (NoSuchMethodException e) {
			return null;
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static <A extends Annotation> void callInitialize(
			A annotation,
			GeneratorCreationContext creationContext,
			Generator generator) {
		if ( generator instanceof AnnotationBasedGenerator annotationBasedGenerator ) {
			annotationBasedGenerator.initialize( annotation, creationContext );
		}
	}

	private static void callConfigure(GeneratorCreationContext creationContext, Generator generator) {
		if ( generator instanceof Configurable configurable ) {
			configurable.configure( creationContext, new Properties() );
		}
		if ( generator instanceof ExportableProducer exportableProducer ) {
			exportableProducer.registerExportables( creationContext.getDatabase() );
		}
		if ( generator instanceof Configurable configurable ) {
			configurable.initialize( creationContext.getSqlStringGenerationContext() );
		}
	}

	private static void checkVersionGenerationAlways(MemberDetails member, Generator generator) {
		if ( member.hasDirectAnnotationUsage( jakarta.persistence.Version.class ) ) {
			if ( !generator.generatesOnInsert() ) {
				throw new AnnotationException(
						"Property '" + member.getName()
								+ "' is annotated '@Version' but has a 'Generator' which does not generate on inserts"
				);
			}
			if ( !generator.generatesOnUpdate() ) {
				throw new AnnotationException(
						"Property '" + member.getName()
								+ "' is annotated '@Version' but has a 'Generator' which does not generate on updates"
				);
			}
		}
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
				else {
					final var typeAnn = member.getDirectAnnotationUsage( AnyKeyType.class );
					if ( typeAnn != null ) {
						applyAnyKeyType( member, basicValue, typeAnn.value() );
					}
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

	private static void applyAnyKeyType(
			MemberDetails member,
			BasicValue basicValue,
			String typeName) {
		basicValue.setExplicitJavaTypeAccess( (typeConfiguration) -> {
			final var registeredType = typeConfiguration.getBasicTypeRegistry().getRegisteredType( typeName );
			if ( registeredType == null ) {
				throw new MappingException( "Unrecognized @AnyKeyType value - " + typeName + " - " + member.getName() );
			}
			return (BasicJavaType<?>) registeredType.getJavaTypeDescriptor();
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
		final Class<? extends MutabilityPlan<?>> mutabilityClass = switch ( source.kind() ) {
			case COLLECTION_ID -> {
				final var mutabilityAnn = source.member().getDirectAnnotationUsage( CollectionIdMutability.class );
				yield mutabilityAnn == null ? null : mutabilityAnn.value();
			}
			case MAP_KEY -> {
				final var mutabilityAnn = source.member().getDirectAnnotationUsage( MapKeyMutability.class );
				yield mutabilityAnn == null ? null : mutabilityAnn.value();
			}
			default -> null;
		};
		if ( mutabilityClass == null ) {
			return;
		}

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
		final Annotation typeAnn = switch ( source.kind() ) {
			case COLLECTION_ID -> source.member().getDirectAnnotationUsage( CollectionIdType.class );
			case MAP_KEY -> source.member().getDirectAnnotationUsage( MapKeyType.class );
			default -> source.member().getDirectAnnotationUsage( Type.class );
		};
		if ( typeAnn == null ) {
			return;
		}

		basicValue.setTypeAnnotation( typeAnn );
		if ( typeAnn instanceof CollectionIdType collectionIdType ) {
			basicValue.setExplicitTypeParams( extractParameterMap( collectionIdType.parameters() ) );
			basicValue.setExplicitCustomType( collectionIdType.value() );
		}
		else if ( typeAnn instanceof MapKeyType mapKeyType ) {
			basicValue.setExplicitTypeParams( extractParameterMap( mapKeyType.parameters() ) );
			basicValue.setExplicitCustomType( mapKeyType.value() );
		}
		else if ( typeAnn instanceof Type type ) {
			basicValue.setExplicitTypeParams( extractParameterMap( type.parameters() ) );
			basicValue.setExplicitCustomType( type.value() );
		}
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

	public static void bindPartitionKey(
			MemberDetails member,
			Property property,
			BasicValue basicValue,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		if ( member.hasDirectAnnotationUsage( PartitionKey.class ) ) {
			basicValue.setPartitionKey( true );
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
