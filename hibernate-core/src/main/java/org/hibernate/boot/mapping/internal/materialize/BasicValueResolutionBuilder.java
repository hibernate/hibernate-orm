/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.materialize;

import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;

import jakarta.annotation.Nullable;
import org.hibernate.MappingException;
import org.hibernate.boot.internal.AnyKeyType;
import org.hibernate.annotations.AnyKeyJavaType;
import org.hibernate.annotations.AnyKeyJdbcType;
import org.hibernate.annotations.AnyKeyJdbcTypeCode;
import org.hibernate.annotations.CollectionIdJavaType;
import org.hibernate.annotations.CollectionIdJdbcType;
import org.hibernate.annotations.CollectionIdJdbcTypeCode;
import org.hibernate.annotations.CollectionIdMutability;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.ListIndexJavaType;
import org.hibernate.annotations.ListIndexJdbcType;
import org.hibernate.annotations.ListIndexJdbcTypeCode;
import org.hibernate.annotations.MapKeyJavaType;
import org.hibernate.annotations.MapKeyJdbcType;
import org.hibernate.annotations.MapKeyJdbcTypeCode;
import org.hibernate.annotations.MapKeyMutability;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import org.hibernate.boot.model.TypeDefinition;
import org.hibernate.boot.model.convert.internal.AutoApplicableConverterDescriptorBypassedImpl;
import org.hibernate.boot.model.convert.internal.ConverterDescriptors;
import org.hibernate.boot.model.convert.spi.AutoApplicableConverterDescriptor;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.convert.spi.JpaAttributeConverterCreationContext;
import org.hibernate.boot.model.process.internal.InferredBasicValueResolution;
import org.hibernate.boot.model.process.internal.InferredBasicValueResolver;
import org.hibernate.boot.model.process.internal.NamedBasicTypeResolution;
import org.hibernate.boot.model.process.internal.NamedConverterResolution;
import org.hibernate.boot.model.process.internal.VersionResolution;
import org.hibernate.boot.mapping.internal.sources.BasicValueSource;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Table;
import org.hibernate.models.ModelsException;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.tool.schema.extract.spi.ColumnTypeInformation;
import org.hibernate.type.BasicType;
import org.hibernate.type.NumericBooleanConverter;
import org.hibernate.type.TimeZoneStorageStrategy;
import org.hibernate.type.TrueFalseConverter;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.converter.spi.JpaAttributeConverter;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.java.BasicPluralJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.spi.JsonJavaType;
import org.hibernate.type.descriptor.java.spi.RegistryHelper;
import org.hibernate.type.descriptor.java.spi.XmlJavaType;
import org.hibernate.type.descriptor.jdbc.BooleanJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.internal.BasicTypeImpl;
import org.hibernate.type.internal.ConvertedBasicTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.DynamicParameterizedType;

import jakarta.persistence.AttributeConverter;

import static java.lang.Boolean.parseBoolean;
import static org.hibernate.boot.model.convert.spi.ConverterDescriptor.TYPE_NAME_PREFIX;
import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;
import static org.hibernate.internal.util.GenericAssignability.isAssignableFrom;
import static org.hibernate.internal.util.ReflectHelper.reflectedPropertyType;
import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.internal.util.collections.CollectionHelper.isEmpty;

/// Builds [BasicValue.Resolution] instances for explicit materialization paths.
///
/// @since 9.0
/// @author Steve Ebersole
public final class BasicValueResolutionBuilder {
	private BasicValueResolutionBuilder() {
	}

	public static BasicValue.Resolution<?> buildResolution(Input input) {
		final var typeParameters = input.getTypeParameters();
		if ( typeParameters != null
				&& parseBoolean( typeParameters.getProperty( DynamicParameterizedType.IS_DYNAMIC ) )
				&& typeParameters.get( DynamicParameterizedType.PARAMETER_TYPE ) == null ) {
			input.getTypeParameters().put( DynamicParameterizedType.PARAMETER_TYPE, input.createParameterType() );
		}
		return buildResolution( input, typeParameters );
	}

	public static void applyResolution(Input input) {
		if ( input.value().getResolution() != null ) {
			input.value().applyResolution( input.value().getResolution() );
			return;
		}
		final BasicValue.Resolution<?> resolution = buildResolution( input );
		if ( resolution == null ) {
			throw new IllegalStateException( "Unable to resolve BasicValue: " + input.value() );
		}
		input.value().applyResolution( resolution );
	}

	private static BasicValue.Resolution<?> buildResolution(Input input, Properties typeParameters) {
		final var explicitJavaType = input.role().determineExplicitJavaType( input );
		final var explicitJdbcType = input.role().determineExplicitJdbcType( input );
		final var explicitMutabilityPlan = input.role().determineExplicitMutabilityPlan( input );
		if ( input.getExplicitTypeName() != null ) {
			return interpretExplicitlyNamedType(
					input.getExplicitTypeName(),
					explicitJavaType,
					explicitJdbcType,
					explicitMutabilityPlan,
					input.getAttributeConverterDescriptor(),
					typeParameters,
					input::setTypeParameters,
					input,
					input.getBuildingContext()
			);
		}
		else if ( input.isVersion() ) {
			return VersionResolution.from( input.value(), input.getTimeZoneStorageType(), input.getBuildingContext() );
		}
		else {
			final var javaType = determineJavaType( input, explicitJavaType );
			final var converterDescriptor = getConverterDescriptor( input, javaType );
			return converterDescriptor != null
					? converterResolution( input, javaType, converterDescriptor, explicitJavaType, explicitJdbcType, explicitMutabilityPlan )
					: resolution( input, explicitJavaType, explicitJdbcType, explicitMutabilityPlan, javaType );
		}
	}

	private static ConverterDescriptor<?,?> getConverterDescriptor(Input input, JavaType<?> javaType) {
		final var converterDescriptor = input.getAttributeConverterDescriptor();
		if ( input.isSoftDelete() && input.getSoftDeleteStrategy() != SoftDeleteType.TIMESTAMP ) {
			assert converterDescriptor != null;
			@SuppressWarnings("unchecked")
			final var booleanConverterDescriptor =
					(ConverterDescriptor<Boolean, ?>) converterDescriptor;
			final var softDeleteConverterDescriptor =
					getSoftDeleteConverterDescriptor( input, booleanConverterDescriptor, javaType );
			return input.getSoftDeleteStrategy() == SoftDeleteType.ACTIVE
					? new ReversedConverterDescriptor<>( softDeleteConverterDescriptor )
					: softDeleteConverterDescriptor;
		}
		else {
			return converterDescriptor;
		}
	}

	private static ConverterDescriptor<Boolean,?> getSoftDeleteConverterDescriptor(
			Input input,
			ConverterDescriptor<Boolean,?> attributeConverterDescriptor,
			JavaType<?> javaType) {
		final boolean conversionWasUnspecified =
				SoftDelete.UnspecifiedConversion.class.equals( attributeConverterDescriptor.getAttributeConverterClass() );
		if ( conversionWasUnspecified ) {
			final var jdbcType = BooleanJdbcType.INSTANCE.resolveIndicatedType( input, javaType );
			if ( jdbcType.isNumber() ) {
				return ConverterDescriptors.of( NumericBooleanConverter.INSTANCE );
			}
			else if ( jdbcType.isString() ) {
				// here we pick 'T' / 'F' storage, though 'Y' / 'N' is equally valid - its 50/50
				return ConverterDescriptors.of( TrueFalseConverter.INSTANCE );
			}
			else {
				// should indicate BIT or BOOLEAN == no conversion needed
				//		- we still create the converter to properly set up JDBC type, etc
				return ConverterDescriptors.of( PassThruSoftDeleteConverter.INSTANCE );
			}
		}
		else {
			return attributeConverterDescriptor;
		}
	}

	private static BasicValue.Resolution<?> resolution(
			Input input,
			BasicJavaType explicitJavaType,
			JdbcType explicitJdbcType,
			MutabilityPlan<?> explicitMutabilityPlan,
			JavaType<?> javaType) {
		final JavaType<?> basicJavaType;
		final JdbcType jdbcType;
		if ( explicitJdbcType != null ) {
			final var typeConfiguration = input.getTypeConfiguration();
			jdbcType = explicitJdbcType;
			basicJavaType = javaType == null && jdbcType != null
					? jdbcType.getRecommendedJavaType( null, null, typeConfiguration )
					: javaType;
		}
		else {
			jdbcType = null;
			basicJavaType = javaType;
		}
		if ( basicJavaType == null ) {
			throw new MappingException( "Unable to determine JavaType to use : " + input.value() );
		}

		if ( basicJavaType instanceof BasicJavaType<?> castType
				&& ( !basicJavaType.getJavaTypeClass().isEnum() || input.getEnumerationStyle() == null ) ) {
			final var context = input.getBuildingContext();
			final var autoAppliedTypeDef = context.getTypeDefinitionRegistry().resolveAutoApplied( castType );
			if ( autoAppliedTypeDef != null ) {
				CORE_LOGGER.trace( "BasicValue resolution matched auto-applied type definition" );
				return autoAppliedTypeDef.resolve( input.getTypeParameters(), context, input );
			}
		}

		return InferredBasicValueResolver.from(
				explicitJavaType,
				jdbcType,
				input.getResolvedJavaType(),
				() -> determineReflectedJavaType( input ),
				mutabilityPlanAccess( explicitMutabilityPlan ),
				input,
				input.getTable(),
				input.getColumn(),
				input.getOwnerName(),
				input.getPropertyName(),
				input.getBuildingContext()
		);
	}

	private static BasicValue.Resolution<?> converterResolution(
			Input input,
			JavaType<?> javaType,
			ConverterDescriptor<?,?> attributeConverterDescriptor,
			BasicJavaType<?> explicitJavaType,
			JdbcType explicitJdbcType,
			MutabilityPlan<?> explicitMutabilityPlan) {
		final var converterResolution = NamedConverterResolution.from(
				attributeConverterDescriptor,
				explicitJavaType,
				explicitJdbcType,
				explicitMutabilityPlan,
				input.getResolvedJavaType(),
				input,
				input,
				input.getBuildingContext()
		);

		if ( javaType instanceof BasicPluralJavaType<?> pluralJavaType
				&& !isAssignableFrom( attributeConverterDescriptor.getDomainValueResolvedType(),
						javaType.getJavaTypeClass() ) ) {
			final BasicType registeredElementType = converterResolution.getLegacyResolvedBasicType();
			final BasicType<?> registeredType = registeredElementType == null ? null
					: pluralJavaType.resolveType(
							input.getTypeConfiguration(),
							input.getDialect(),
							registeredElementType,
							input.getColumn() instanceof ColumnTypeInformation information ? information : null,
							input
			);
			if ( registeredType != null ) {
				input.getTypeConfiguration().getBasicTypeRegistry().register( registeredType );
				return new InferredBasicValueResolution(
						registeredType,
						registeredType.getJavaTypeDescriptor(),
						registeredType.getJavaTypeDescriptor(),
						registeredType.getJdbcType(),
						registeredType,
						null
				);
			}
		}

		return converterResolution;
	}

	private static JavaType<?> determineJavaType(Input input, BasicJavaType<?> explicitJavaType) {
		if ( explicitJavaType == null ) {
			final var reflectedJtd = determineReflectedJavaType( input );
			if ( reflectedJtd != null ) {
				return reflectedJtd;
			}
		}

		return explicitJavaType;
	}

	private static JavaType<?> determineReflectedJavaType(Input input) {
		final var typeConfiguration = input.getTypeConfiguration();
		final var impliedJavaType = impliedJavaType( input, typeConfiguration );
		if ( impliedJavaType == null ) {
			return null;
		}
		else {
			input.setResolvedJavaType( impliedJavaType );
			return javaType( input, typeConfiguration, impliedJavaType );
		}
	}

	private static java.lang.reflect.Type impliedJavaType(Input input, TypeConfiguration typeConfiguration) {
		if ( input.getResolvedJavaType() != null ) {
			return input.getResolvedJavaType();
		}
		else if ( input.getSourceJavaType() != null ) {
			return input.getSourceJavaType().asReflectType();
		}
		else if ( input.getOwnerName() != null && input.getPropertyName() != null ) {
			return reflectedPropertyType( input.getOwnerName(), input.getPropertyName(), input.classLoaderService() );
		}
		else {
			return null;
		}
	}

	private static JavaType<?> javaType(
			Input input,
			TypeConfiguration typeConfiguration,
			java.lang.reflect.Type impliedJavaType) {
		final var javaType = typeConfiguration.getJavaTypeRegistry().findDescriptor( impliedJavaType );
		return javaType == null ? specialJavaType( input, typeConfiguration, impliedJavaType ) : javaType;
	}

	private static JavaType<?> specialJavaType(
			Input input,
			TypeConfiguration typeConfiguration,
			java.lang.reflect.Type impliedJavaType) {
		final var javaTypeRegistry = typeConfiguration.getJavaTypeRegistry();
		final var jdbcTypeCode = input.getConfiguredJdbcTypeCode();
		if ( jdbcTypeCode != null ) {
			final var explicitMutabilityPlan = input.role().determineExplicitMutabilityPlan( input );
			switch ( jdbcTypeCode ) {
				case org.hibernate.type.SqlTypes.JSON:
					final var jsonJavaType =
							new JsonJavaType<>( impliedJavaType,
									mutabilityPlan( explicitMutabilityPlan, typeConfiguration, impliedJavaType ),
									typeConfiguration );
					javaTypeRegistry.addDescriptor( jsonJavaType );
					return jsonJavaType;
				case org.hibernate.type.SqlTypes.SQLXML:
					final var xmlJavaType =
							new XmlJavaType<>( impliedJavaType,
									mutabilityPlan( explicitMutabilityPlan, typeConfiguration, impliedJavaType ),
									typeConfiguration );
					javaTypeRegistry.addDescriptor( xmlJavaType );
					return xmlJavaType;
			}
		}
		return javaTypeRegistry.resolveDescriptor( impliedJavaType );
	}

	private static MutabilityPlan<?> mutabilityPlan(
			MutabilityPlan<?> explicitMutabilityPlan,
			TypeConfiguration typeConfiguration,
			java.lang.reflect.Type impliedJavaType) {
		return explicitMutabilityPlan != null
				? explicitMutabilityPlan
				: RegistryHelper.INSTANCE.determineMutabilityPlan( impliedJavaType, typeConfiguration );
	}

	private static BasicValue.Resolution<?> interpretExplicitlyNamedType(
			String name,
			BasicJavaType<?> explicitJavaType,
			JdbcType explicitJdbcType,
			MutabilityPlan<?> explicitMutabilityPlan,
			ConverterDescriptor<?,?> converterDescriptor,
			Map<Object,Object> localTypeParams,
			Consumer<Properties> combinedParameterConsumer,
			JdbcTypeIndicators stdIndicators,
			MetadataBuildingContext context) {

		final var managedBeanRegistry = context.getManagedBeanRegistry();
		final var typeConfiguration = context.getTypeConfiguration();

		final var converterCreationContext =
				new JpaAttributeConverterCreationContext() {
					@Override
					public ManagedBeanRegistry getManagedBeanRegistry() {
						return managedBeanRegistry;
					}

					@Override
					public TypeConfiguration getTypeConfiguration() {
						return typeConfiguration;
					}
				};

		if ( name.startsWith( TYPE_NAME_PREFIX  ) ) {
			return NamedConverterResolution.from(
					name,
					explicitJavaType,
					explicitJdbcType,
					explicitMutabilityPlan,
					stdIndicators,
					converterCreationContext,
					context
			);
		}

		if ( name.startsWith( BasicTypeImpl.EXTERNALIZED_PREFIX )
			|| name.startsWith( ConvertedBasicTypeImpl.EXTERNALIZED_PREFIX ) ) {
			return getNamedBasicTypeResolution(
					context.resolveAdHocBasicType( name ),
					explicitMutabilityPlan
			);
		}

		final var basicTypeByName =
				typeConfiguration.getBasicTypeRegistry()
						.getRegisteredType( name );
		if ( basicTypeByName != null ) {
			return getNamedBasicTypeResolution(
					explicitMutabilityPlan,
					converterDescriptor,
					converterCreationContext,
					basicTypeByName
			);
		}

		final var typeDefinition =
				context.getTypeDefinitionRegistry()
						.resolve( name );
		if ( typeDefinition != null ) {
			final var resolution = typeDefinition.resolve(
					localTypeParams,
					explicitMutabilityPlan,
					context,
					stdIndicators
			);
			combinedParameterConsumer.accept( resolution.getCombinedTypeParameters() );
			return resolution;
		}

		try {
			final var typeNamedClass = context.getClassLoaderAccess().classForName( name );
			if ( isEmpty( localTypeParams ) ) {
				final var implicitDefinition =
						new TypeDefinition( name, typeNamedClass, null, null );
				context.getTypeDefinitionRegistry().register( implicitDefinition );
				return implicitDefinition.resolve(
						localTypeParams,
						explicitMutabilityPlan,
						context,
						stdIndicators
				);
			}

			return TypeDefinition.createLocalResolution( name, typeNamedClass, localTypeParams, context );
		}
		catch (ClassLoadingException e) {
			CORE_LOGGER.couldNotResolveTypeName( name, e );
		}

		throw new MappingException( "Could not resolve named type: " + name );
	}

	private static <J> NamedBasicTypeResolution<J> getNamedBasicTypeResolution(
			MutabilityPlan<?> explicitMutabilityPlan,
			ConverterDescriptor<?,?> converterDescriptor,
			JpaAttributeConverterCreationContext converterCreationContext,
			BasicType<J> basicTypeByName) {
		final BasicValueConverter<J,?> valueConverter;
		final JavaType<J> domainJtd;
		if ( converterDescriptor != null ) {
			//noinspection unchecked
			valueConverter =
					(BasicValueConverter<J,?>)
							converterDescriptor.createJpaAttributeConverter( converterCreationContext );
			domainJtd = valueConverter.getDomainJavaType();
		}
		else {
			valueConverter = basicTypeByName.getValueConverter();
			domainJtd = basicTypeByName.getJavaTypeDescriptor();
		}

		return new NamedBasicTypeResolution<>(
				domainJtd,
				basicTypeByName,
				valueConverter,
				castMutabilityPlan( explicitMutabilityPlan )
		);
	}

	private static <J> NamedBasicTypeResolution<J> getNamedBasicTypeResolution(
			BasicType<J> basicType,
			MutabilityPlan<?> explicitMutabilityPlan) {
		return new NamedBasicTypeResolution<>(
				basicType.getJavaTypeDescriptor(),
				basicType,
				null,
				castMutabilityPlan( explicitMutabilityPlan )
		);
	}

	@SuppressWarnings( "unchecked" )
	private static <J> MutabilityPlan<J> castMutabilityPlan(MutabilityPlan<?> explicitMutabilityPlan) {
		return (MutabilityPlan<J>) explicitMutabilityPlan;
	}

	private static java.util.function.Function<TypeConfiguration, MutabilityPlan<?>> mutabilityPlanAccess(
			MutabilityPlan<?> explicitMutabilityPlan) {
		return explicitMutabilityPlan == null ? null : (typeConfiguration) -> explicitMutabilityPlan;
	}

	/**
	 * The annotation-level JDBC type selection for a particular value role.
	 * <p>
	 * Hibernate exposes paired annotations for most basic-value positions:
	 * one names a {@link JdbcType} implementation and the other names a JDBC
	 * type code.  This small carrier lets role-specific lookup code return the
	 * two possibilities together so the common validation and instantiation
	 * rules stay in one place.
	 */
	private record JdbcTypeSelection(
			Class<? extends JdbcType> jdbcTypeClass,
			Integer jdbcTypeCode) {
	}

	/**
	 * Describes the semantic position of the basic value being resolved.
	 * <p>
	 * The role drives annotation lookup for explicit Java type, JDBC type, and
	 * mutability hints.  For example, map keys use {@code @MapKeyJavaType} and
	 * {@code @MapKeyJdbcType}, collection ids use the collection-id variants,
	 * and ordinary attributes use the standard basic-value annotations.
	 * <p>
	 * Keeping these choices here avoids reintroducing deferred callback state
	 * on {@link BasicValue}.  The binder/materializer supplies an {@link Input},
	 * and the role interprets that input at resolution time.
	 */
	public enum BasicValueRole {
		ATTRIBUTE,
		EMBEDDABLE_MEMBER,
		COLLECTION_ELEMENT,
		LIST_INDEX,
		MAP_KEY,
		COLLECTION_ID,
		IDENTIFIER,
		ANY_DISCRIMINATOR,
		ANY_KEY,
		DISCRIMINATOR,
		SOFT_DELETE,
		STATE_MANAGEMENT,
		GENERIC_DECLARATION,
		VERSION;

		public static BasicValueRole from(BasicValueSource.Kind kind) {
			return switch ( kind ) {
				case ATTRIBUTE -> ATTRIBUTE;
				case EMBEDDABLE_MEMBER -> EMBEDDABLE_MEMBER;
				case COLLECTION_ELEMENT -> COLLECTION_ELEMENT;
				case LIST_INDEX -> LIST_INDEX;
				case MAP_KEY -> MAP_KEY;
				case COLLECTION_ID -> COLLECTION_ID;
				case IDENTIFIER -> IDENTIFIER;
				case ANY_DISCRIMINATOR -> ANY_DISCRIMINATOR;
				case ANY_KEY -> ANY_KEY;
				case DISCRIMINATOR -> DISCRIMINATOR;
				case SOFT_DELETE -> SOFT_DELETE;
				case STATE_MANAGEMENT -> STATE_MANAGEMENT;
				case GENERIC_DECLARATION -> GENERIC_DECLARATION;
			};
		}

		@Nullable BasicJavaType<?> determineExplicitJavaType(Input input) {
			if ( input.explicitJavaType() != null ) {
				return input.explicitJavaType();
			}

			final var member = input.member();
			if ( member == null ) {
				return null;
			}

			final Class<? extends BasicJavaType<?>> javaTypeClass = switch ( this ) {
				case MAP_KEY -> {
					final var javaTypeAnn = member.getDirectAnnotationUsage( MapKeyJavaType.class );
					yield javaTypeAnn == null ? null : javaTypeAnn.value();
				}
				case LIST_INDEX -> {
					final var javaTypeAnn = member.getDirectAnnotationUsage( ListIndexJavaType.class );
					yield javaTypeAnn == null ? null : javaTypeAnn.value();
				}
				case COLLECTION_ID -> {
					final var javaTypeAnn = member.getDirectAnnotationUsage( CollectionIdJavaType.class );
					yield javaTypeAnn == null ? null : javaTypeAnn.value();
				}
				case ANY_KEY -> {
					final var javaTypeAnn = member.locateAnnotationUsage(
							AnyKeyJavaType.class,
							input.getBuildingContext().getModelsContext()
					);
					yield javaTypeAnn == null ? null : javaTypeAnn.value();
				}
				default -> {
					final var javaTypeAnn = member.getDirectAnnotationUsage( org.hibernate.annotations.JavaType.class );
					yield javaTypeAnn == null ? null : javaTypeAnn.value();
				}
			};

			if ( javaTypeClass != null ) {
				@SuppressWarnings("unchecked")
				final Class<BasicJavaType<?>> javaClass = (Class<BasicJavaType<?>>) javaTypeClass;
				return input.getBuildingContext()
						.getManagedBeanRegistry()
						.getBean( javaClass )
						.getBeanInstance();
			}

			if ( this == ANY_KEY ) {
				final var typeAnn = member.locateAnnotationUsage(
						AnyKeyType.class,
						input.getBuildingContext().getModelsContext()
				);
				if ( typeAnn != null ) {
					final var registeredType = input.getTypeConfiguration()
							.getBasicTypeRegistry()
							.getRegisteredType( typeAnn.value() );
					if ( registeredType == null ) {
						throw new MappingException( "Unrecognized @AnyKeyType value - " + typeAnn.value() + " - " + member.getName() );
					}
					return (BasicJavaType<?>) registeredType.getJavaTypeDescriptor();
				}
			}

			return null;
		}

		@Nullable JdbcType determineExplicitJdbcType(Input input) {
			if ( input.explicitJdbcType() != null ) {
				return input.explicitJdbcType();
			}

			final var member = input.member();
			if ( member == null ) {
				return null;
			}

			final JdbcTypeSelection selection = switch ( this ) {
				case MAP_KEY -> mapKeyJdbcTypeSelection( member );
				case LIST_INDEX -> listIndexJdbcTypeSelection( member );
				case COLLECTION_ID -> collectionIdJdbcTypeSelection( member );
				case ANY_KEY -> {
					final var modelsContext = input.getBuildingContext().getModelsContext();
					final var jdbcTypeAnn = member.locateAnnotationUsage( AnyKeyJdbcType.class, modelsContext );
					final var jdbcTypeCodeAnn = member.locateAnnotationUsage( AnyKeyJdbcTypeCode.class, modelsContext );
					yield new JdbcTypeSelection(
							jdbcTypeAnn == null ? null : jdbcTypeAnn.value(),
							jdbcTypeCodeAnn == null ? null : jdbcTypeCodeAnn.value()
					);
				}
				default -> standardJdbcTypeSelection( member );
			};

			if ( selection.jdbcTypeClass() != null ) {
				if ( selection.jdbcTypeCode() != null ) {
					throw new ModelsException( "Illegal combination of @JdbcType and @JdbcTypeCode - " + member.getName() );
				}
				return instantiateJdbcType( member, selection.jdbcTypeClass() );
			}
			else if ( selection.jdbcTypeCode() != null ) {
				input.setConfiguredJdbcTypeCode( selection.jdbcTypeCode() );
				final var jdbcTypeRegistry = input.getTypeConfiguration().getJdbcTypeRegistry();
				return jdbcTypeRegistry.getConstructor( selection.jdbcTypeCode() ) == null
						? jdbcTypeRegistry.getDescriptor( selection.jdbcTypeCode() )
						: null;
			}

			return null;
		}

		@Nullable MutabilityPlan<?> determineExplicitMutabilityPlan(Input input) {
			if ( input.explicitMutabilityPlan() != null ) {
				return input.explicitMutabilityPlan();
			}
			if ( input.attributeMutabilityPlanClass() != null ) {
				return instantiateMutabilityPlan( input.member(), input.attributeMutabilityPlanClass() );
			}
			if ( input.attributeImmutable() ) {
				return org.hibernate.type.descriptor.java.ImmutableMutabilityPlan.instance();
			}

			final var member = input.member();
			if ( member == null ) {
				return null;
			}

			final Class<? extends MutabilityPlan<?>> mutabilityClass = switch ( this ) {
				case COLLECTION_ID -> {
					final var mutabilityAnn = member.getDirectAnnotationUsage( CollectionIdMutability.class );
					yield mutabilityAnn == null ? null : mutabilityAnn.value();
				}
				case MAP_KEY -> {
					final var mutabilityAnn = member.getDirectAnnotationUsage( MapKeyMutability.class );
					yield mutabilityAnn == null ? null : mutabilityAnn.value();
				}
				default -> null;
			};

			return mutabilityClass == null
					? null
					: instantiateMutabilityPlan( member, mutabilityClass );
		}

		private static JdbcTypeSelection mapKeyJdbcTypeSelection(MemberDetails member) {
			final var jdbcTypeAnn = member.getDirectAnnotationUsage( MapKeyJdbcType.class );
			final var jdbcTypeCodeAnn = member.getDirectAnnotationUsage( MapKeyJdbcTypeCode.class );
			return new JdbcTypeSelection(
					jdbcTypeAnn == null
							? null
							: jdbcTypeAnn.value(),
					jdbcTypeCodeAnn == null
							? null
							: jdbcTypeCodeAnn.value()
			);
		}

		private static JdbcTypeSelection listIndexJdbcTypeSelection(MemberDetails member) {
			final var jdbcTypeAnn = member.getDirectAnnotationUsage( ListIndexJdbcType.class );
			final var jdbcTypeCodeAnn = member.getDirectAnnotationUsage( ListIndexJdbcTypeCode.class );
			return new JdbcTypeSelection(
					jdbcTypeAnn == null
							? null
							: jdbcTypeAnn.value(),
					jdbcTypeCodeAnn == null
							? null
							: jdbcTypeCodeAnn.value()
			);
		}

		private static JdbcTypeSelection collectionIdJdbcTypeSelection(MemberDetails member) {
			final var jdbcTypeAnn = member.getDirectAnnotationUsage( CollectionIdJdbcType.class );
			final var jdbcTypeCodeAnn = member.getDirectAnnotationUsage( CollectionIdJdbcTypeCode.class );
			return new JdbcTypeSelection(
					jdbcTypeAnn == null
							? null
							: jdbcTypeAnn.value(),
					jdbcTypeCodeAnn == null
							? null
							: jdbcTypeCodeAnn.value()
			);
		}

		private static JdbcTypeSelection standardJdbcTypeSelection(MemberDetails member) {
			final var jdbcTypeAnn = member.getDirectAnnotationUsage( org.hibernate.annotations.JdbcType.class );
			final var jdbcTypeCodeAnn = member.getDirectAnnotationUsage( JdbcTypeCode.class );
			return new JdbcTypeSelection(
					jdbcTypeAnn == null
							? null
							: jdbcTypeAnn.value(),
					jdbcTypeCodeAnn == null
							? null
							: jdbcTypeCodeAnn.value()
			);
		}

		private static JdbcType instantiateJdbcType(
				MemberDetails member,
				Class<? extends JdbcType> jdbcTypeClass) {
			try {
				return jdbcTypeClass.getConstructor().newInstance();
			}
			catch (InstantiationException | IllegalAccessException | java.lang.reflect.InvocationTargetException | NoSuchMethodException e) {
				final ModelsException modelsException = new ModelsException( "Error instantiating local @JdbcType - " + member.getName() );
				modelsException.addSuppressed( e );
				throw modelsException;
			}
		}

		private static MutabilityPlan<?> instantiateMutabilityPlan(
				MemberDetails member,
				Class<? extends MutabilityPlan<?>> mutabilityClass) {
			try {
				return mutabilityClass.getConstructor().newInstance();
			}
			catch (InstantiationException | IllegalAccessException | java.lang.reflect.InvocationTargetException | NoSuchMethodException e) {
				final ModelsException modelsException = new ModelsException(
						"Error instantiating local mutability plan - " + member.getName()
				);
				modelsException.addSuppressed( e );
				throw modelsException;
			}
		}
	}

	/**
	 * Mutable input used to build and apply a {@link BasicValue.Resolution}.
	 * <p>
	 * The input bridges the simple mapping object and the richer binding or
	 * materialization state.  It wraps the target {@link BasicValue}, the
	 * declarative {@link BasicValueSource}, and any facts already discovered by
	 * the binder/materializer, such as an explicit Java type, explicit JDBC
	 * type, converter, mutability plan, temporal precision, or configured JDBC
	 * type code.
	 * <p>
	 * It also implements {@link JdbcTypeIndicators} and
	 * {@link JpaAttributeConverterCreationContext} because the lower-level type
	 * resolution helpers still expect those views.  That keeps the compatibility
	 * surface localized here while {@link BasicValue} itself remains a simpler
	 * holder of mapping state plus an applied resolution.
	 */
	public static class Input implements JdbcTypeIndicators, JpaAttributeConverterCreationContext {
		private final BasicValue value;
		private final BasicValueSource source;
		private final BasicValueRole role;
		private final String ownerName;
		private final String propertyName;
		private final boolean softDelete;
		private final SoftDeleteType softDeleteStrategy;
		private Properties typeParameters;
		private String explicitTypeName;
		private ConverterDescriptor<?,?> attributeConverterDescriptor;
		private org.hibernate.annotations.TimeZoneStorageType timeZoneStorageType;
		private jakarta.persistence.EnumType enumerationStyle;
		private jakarta.persistence.TemporalType temporalPrecision;
		private java.lang.reflect.Type resolvedJavaType;
		private BasicJavaType<?> explicitJavaType;
		private JdbcType explicitJdbcType;
		private MutabilityPlan<?> explicitMutabilityPlan;
		private Integer jdbcTypeCode;
		private boolean attributeImmutable;
		private Class<? extends MutabilityPlan<?>> attributeMutabilityPlanClass;

		private Input(BasicValue value, BasicValueSource source) {
			this.value = value;
			this.source = source;
			this.role = value.isVersion() ? BasicValueRole.VERSION : BasicValueRole.from( source.kind() );
			this.softDelete = value.isSoftDelete();
			this.softDeleteStrategy = value.getSoftDeleteStrategy();
			this.ownerName = value.getOwnerName();
			this.propertyName = value.getPropertyName();
		}

		public static Input create(BasicValue value, BasicValueSource source) {
			return new Input( value, source );
		}

		public BasicValue value() {
			return value;
		}

		public BasicValueSource source() {
			return source;
		}

		public MemberDetails member() {
			return source.member();
		}

		public BasicValueRole role() {
			return role;
		}

		public BasicValueSource.Kind kind() {
			return source.kind();
		}

		public BasicValue.SourceJavaType getSourceJavaType() {
			return source.sourceJavaType();
		}

		public Properties getTypeParameters() {
			return typeParameters;
		}

		public void setTypeParameters(Properties typeParameters) {
			this.typeParameters = typeParameters;
		}

		public void setTypeParameters(Map<String, ?> typeParameters) {
			final var properties = new Properties();
			properties.putAll( typeParameters );
			setTypeParameters( properties );
		}

		public DynamicParameterizedType.ParameterType createParameterType() {
			return value.createResolutionParameterType();
		}

		public String getExplicitTypeName() {
			return explicitTypeName;
		}

		public void setExplicitTypeName(String explicitTypeName) {
			this.explicitTypeName = explicitTypeName;
		}

		public ConverterDescriptor<?, ?> getAttributeConverterDescriptor() {
			return attributeConverterDescriptor == null
					? value.getJpaAttributeConverterDescriptor()
					: attributeConverterDescriptor;
		}

		public void setAttributeConverterDescriptor(ConverterDescriptor<?, ?> attributeConverterDescriptor) {
			this.attributeConverterDescriptor = attributeConverterDescriptor;
		}

		public MetadataBuildingContext getBuildingContext() {
			return value.getBuildingContext();
		}

		@Override
		public TypeConfiguration getTypeConfiguration() {
			return value.getTypeConfiguration();
		}

		public boolean isVersion() {
			return role == BasicValueRole.VERSION;
		}

		public org.hibernate.annotations.TimeZoneStorageType getTimeZoneStorageType() {
			return timeZoneStorageType;
		}

		public void setTimeZoneStorageType(org.hibernate.annotations.TimeZoneStorageType timeZoneStorageType) {
			this.timeZoneStorageType = timeZoneStorageType;
		}

		public boolean isSoftDelete() {
			return softDelete;
		}

		public SoftDeleteType getSoftDeleteStrategy() {
			return softDeleteStrategy;
		}

		public jakarta.persistence.EnumType getEnumerationStyle() {
			return enumerationStyle;
		}

		public void setEnumerationStyle(jakarta.persistence.EnumType enumerationStyle) {
			this.enumerationStyle = enumerationStyle;
		}

		public java.lang.reflect.Type getResolvedJavaType() {
			return resolvedJavaType;
		}

		public void setResolvedJavaType(java.lang.reflect.Type resolvedJavaType) {
			this.resolvedJavaType = resolvedJavaType;
		}

		public BasicJavaType<?> explicitJavaType() {
			return explicitJavaType;
		}

		public void setExplicitJavaType(BasicJavaType<?> explicitJavaType) {
			this.explicitJavaType = explicitJavaType;
		}

		public JdbcType explicitJdbcType() {
			return explicitJdbcType;
		}

		public void setExplicitJdbcType(JdbcType explicitJdbcType) {
			this.explicitJdbcType = explicitJdbcType;
		}

		public MutabilityPlan<?> explicitMutabilityPlan() {
			return explicitMutabilityPlan;
		}

		public void setExplicitMutabilityPlan(MutabilityPlan<?> explicitMutabilityPlan) {
			this.explicitMutabilityPlan = explicitMutabilityPlan;
		}

		public Table getTable() {
			return value.getTable();
		}

		public Selectable getColumn() {
			return value.getColumn();
		}

		public String getOwnerName() {
			return ownerName;
		}

		public String getPropertyName() {
			return propertyName;
		}

		public org.hibernate.boot.registry.classloading.spi.ClassLoaderService classLoaderService() {
			return value.classLoaderService();
		}

		public Integer getConfiguredJdbcTypeCode() {
			return jdbcTypeCode;
		}

		public void setConfiguredJdbcTypeCode(Integer jdbcTypeCode) {
			this.jdbcTypeCode = jdbcTypeCode;
		}

		public boolean attributeImmutable() {
			return attributeImmutable;
		}

		public void markAttributeImmutable() {
			this.attributeImmutable = true;
		}

		public Class<? extends MutabilityPlan<?>> attributeMutabilityPlanClass() {
			return attributeMutabilityPlanClass;
		}

		public void setAttributeMutabilityPlanClass(Class<? extends MutabilityPlan<?>> attributeMutabilityPlanClass) {
			this.attributeMutabilityPlanClass = attributeMutabilityPlanClass;
		}

		@Override
		public org.hibernate.dialect.Dialect getDialect() {
			return value.getDialect();
		}

		@Override
		public jakarta.persistence.EnumType getEnumeratedType() {
			return enumerationStyle;
		}

		@Override
		public jakarta.persistence.TemporalType getTemporalPrecision() {
			return temporalPrecision;
		}

		public void setTemporalPrecision(jakarta.persistence.TemporalType temporalPrecision) {
			this.temporalPrecision = temporalPrecision;
		}

		@Override
		public boolean isNationalized() {
			return value.isNationalized();
		}

			@Override
			public boolean isLob() {
				return value.isLob();
			}

			@Override
			public long getColumnLength() {
				return value.getColumnLength();
			}

			@Override
			public int getColumnPrecision() {
				return value.getColumnPrecision();
			}

			@Override
			public int getColumnScale() {
				return value.getColumnScale();
			}

			@Override
			public int getPreferredSqlTypeCodeForBoolean() {
				return value.getPreferredSqlTypeCodeForBoolean();
			}

		@Override
		public int getPreferredSqlTypeCodeForDuration() {
			return value.getPreferredSqlTypeCodeForDuration();
		}

		@Override
		public int getPreferredSqlTypeCodeForUuid() {
			return value.getPreferredSqlTypeCodeForUuid();
		}

		@Override
		public int getPreferredSqlTypeCodeForInstant() {
			return value.getPreferredSqlTypeCodeForInstant();
		}

		@Override
		public int getPreferredSqlTypeCodeForArray() {
			return value.getPreferredSqlTypeCodeForArray();
		}

		@Override
		public int resolveJdbcTypeCode(int jdbcTypeCode) {
			return value.resolveJdbcTypeCode( jdbcTypeCode );
		}

		@Override
		public ManagedBeanRegistry getManagedBeanRegistry() {
			return value.getManagedBeanRegistry();
		}

		@Override
		public TimeZoneStorageStrategy getDefaultTimeZoneStorageStrategy() {
			return value.getDefaultTimeZoneStorageStrategy();
		}
	}

	/**
	 * Converter descriptor used for {@link SoftDeleteType#ACTIVE}.
	 * <p>
	 * Soft-delete mappings normally interpret the stored value as "deleted".
	 * The ACTIVE strategy uses the same relational representation but flips the
	 * boolean domain semantics, so the descriptor creates a converter wrapper
	 * that negates non-null boolean values.
	 */
	private static class ReversedConverterDescriptor<R> implements ConverterDescriptor<Boolean,R> {
		private final ConverterDescriptor<Boolean,R> underlyingDescriptor;

		public ReversedConverterDescriptor(ConverterDescriptor<Boolean,R> underlyingDescriptor) {
			this.underlyingDescriptor = underlyingDescriptor;
		}

		@Override
		public Class<? extends AttributeConverter<Boolean,R>> getAttributeConverterClass() {
			//noinspection unchecked
			return (Class<? extends AttributeConverter<Boolean, R>>) getClass();
		}

		@Override
		public java.lang.reflect.Type getDomainValueResolvedType() {
			return underlyingDescriptor.getDomainValueResolvedType();
		}

		@Override
		public java.lang.reflect.Type getRelationalValueResolvedType() {
			return underlyingDescriptor.getRelationalValueResolvedType();
		}

		@Override
		public AutoApplicableConverterDescriptor getAutoApplyDescriptor() {
			return AutoApplicableConverterDescriptorBypassedImpl.INSTANCE;
		}

		@Override
		public JpaAttributeConverter<Boolean,R> createJpaAttributeConverter(JpaAttributeConverterCreationContext context) {
			return new ReversedJpaAttributeConverter<>(
					underlyingDescriptor.createJpaAttributeConverter( context ),
					context.getJavaTypeRegistry().getDescriptor( ReversedJpaAttributeConverter.class )
			);
		}
	}

	/**
	 * Converter wrapper used by {@link ReversedConverterDescriptor}.
	 * <p>
	 * The wrapper also acts as its own managed bean because it is synthesized
	 * during basic-value resolution rather than discovered from application
	 * classes.
	 */
	private static class ReversedJpaAttributeConverter<R, B extends AttributeConverter<Boolean, R>>
			implements JpaAttributeConverter<Boolean,R>, AttributeConverter<Boolean,R>, ManagedBean<B> {
		private final JpaAttributeConverter<Boolean,R> underlyingJpaConverter;
		private final JavaType<ReversedJpaAttributeConverter<R,B>> converterJavaType;

		public ReversedJpaAttributeConverter(
				JpaAttributeConverter<Boolean, R> underlyingJpaConverter,
				JavaType<ReversedJpaAttributeConverter<R,B>> converterJavaType) {
			this.underlyingJpaConverter = underlyingJpaConverter;
			this.converterJavaType = converterJavaType;
		}

		@Override
		public Boolean toDomainValue(R relationalValue) {
			final Boolean domainValue = underlyingJpaConverter.toDomainValue( relationalValue );
			return domainValue == null ? null : !domainValue;
		}

		@Override
		public R toRelationalValue(Boolean domainValue) {
			return underlyingJpaConverter.toRelationalValue( domainValue == null ? null : !domainValue );
		}

		@Override
		public Boolean convertToEntityAttribute(R relationalValue) {
			return toDomainValue( relationalValue );
		}

		@Override
		public R convertToDatabaseColumn(Boolean domainValue) {
			return toRelationalValue( domainValue );
		}

		@Override
		public JavaType<Boolean> getDomainJavaType() {
			return underlyingJpaConverter.getDomainJavaType();
		}

		@Override
		public JavaType<R> getRelationalJavaType() {
			return underlyingJpaConverter.getRelationalJavaType();
		}

		@Override
		public JavaType<? extends AttributeConverter<Boolean, R>> getConverterJavaType() {
			return converterJavaType;
		}

		@Override
		public ManagedBean<? extends AttributeConverter<Boolean, R>> getConverterBean() {
			return this;
		}

		@Override
		public Class<B> getBeanClass() {
			//noinspection unchecked
			return (Class<B>) getClass();
		}

		@Override
		public B getBeanInstance() {
			//noinspection unchecked
			return (B) this;
		}
	}

	/**
	 * Converter used when an unspecified soft-delete conversion resolves to a
	 * boolean JDBC representation that already matches Hibernate's domain
	 * boolean.
	 */
	private static class PassThruSoftDeleteConverter implements AttributeConverter<Boolean,Boolean> {
		private static final PassThruSoftDeleteConverter INSTANCE = new PassThruSoftDeleteConverter();

		@Override
		public Boolean convertToDatabaseColumn(Boolean domainValue) {
			return domainValue;
		}

		@Override
		public Boolean convertToEntityAttribute(Boolean relationalValue) {
			return relationalValue;
		}
	}
}
