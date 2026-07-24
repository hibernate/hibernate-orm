/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.materialize;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;

import jakarta.annotation.Nullable;
import jakarta.persistence.EnumType;
import jakarta.persistence.TemporalType;
import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
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
import org.hibernate.boot.model.internal.Constructors;
import org.hibernate.boot.model.convert.internal.AutoApplicableConverterDescriptorBypassedImpl;
import org.hibernate.boot.model.convert.internal.ConverterDescriptors;
import org.hibernate.boot.model.convert.spi.AutoApplicableConverterDescriptor;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.convert.spi.JpaAttributeConverterCreationContext;
import org.hibernate.boot.model.process.internal.InferredBasicValueResolution;
import org.hibernate.boot.model.process.internal.InferredBasicValueResolver;
import org.hibernate.boot.model.process.internal.NamedBasicTypeResolution;
import org.hibernate.boot.model.process.internal.NamedConverterResolution;
import org.hibernate.boot.model.process.internal.UserTypeResolution;
import org.hibernate.boot.model.process.internal.VersionResolution;
import org.hibernate.boot.mapping.internal.context.MappingResolutionServices;
import org.hibernate.boot.mapping.internal.context.MappingResolutionState;
import org.hibernate.boot.mapping.internal.sources.BasicValueSource;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.BasicValue;
import org.hibernate.models.ModelsException;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.extract.spi.ColumnTypeInformation;
import org.hibernate.type.BasicType;
import org.hibernate.type.CustomType;
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
import org.hibernate.type.spi.TypeConfigurationAware;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.AnnotationBasedUserType;
import org.hibernate.usertype.DynamicParameterizedType;
import org.hibernate.usertype.UserType;
import org.hibernate.usertype.UserTypeCreationContext;
import org.hibernate.resource.beans.internal.FallbackBeanInstanceProducer;

import jakarta.persistence.AttributeConverter;

import static java.lang.Boolean.parseBoolean;
import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;
import static org.hibernate.internal.util.GenericsHelper.typeArguments;
import static org.hibernate.internal.util.GenericAssignability.isAssignableFrom;
import static org.hibernate.internal.util.ReflectHelper.reflectedPropertyType;
import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.internal.util.collections.CollectionHelper.isEmpty;
import static org.hibernate.internal.util.collections.CollectionHelper.isNotEmpty;
import static org.hibernate.mapping.MappingHelper.injectParameters;

/// Builds [BasicValue.Resolution] instances for explicit materialization paths.
///
/// @since 9.0
/// @author Steve Ebersole
public final class BasicValueResolutionBuilder {
	private static int userTypeBeanCounter;

	private BasicValueResolutionBuilder() {
	}

	public static void applyResolution(
			BasicValueResolutionDetails details,
			MappingResolutionServices services,
			MappingResolutionState state,
			MetadataBuildingContext buildingContext) {
		if ( details.value().getResolution() != null ) {
			details.value().applyResolution( details.value().getResolution(), state );
			return;
		}
		state.captureResolutionDetails( details );
		final BasicValue.Resolution<?> resolution = buildResolution( details, services, state, buildingContext );
		if ( resolution == null ) {
			throw new IllegalStateException( "Unable to resolve BasicValue: " + details.value() );
		}
		details.value().applyResolution( resolution, state );
	}

	private static BasicValue.Resolution<?> buildResolution(
			BasicValueResolutionDetails details,
			MappingResolutionServices services,
			MappingResolutionState state,
			MetadataBuildingContext buildingContext) {
		final var typeParameters = details.getTypeParameters();
		if ( typeParameters != null
				&& parseBoolean( typeParameters.getProperty( DynamicParameterizedType.IS_DYNAMIC ) )
				&& typeParameters.get( DynamicParameterizedType.PARAMETER_TYPE ) == null ) {
			details.getTypeParameters().put(
					DynamicParameterizedType.PARAMETER_TYPE,
					details.createParameterType( services.getClassLoaderService() )
			);
		}
		return buildResolution( details, services, state, buildingContext, typeParameters );
	}

	private static BasicValue.Resolution<?> buildResolution(
			BasicValueResolutionDetails details,
			MappingResolutionServices services,
			MappingResolutionState state,
			MetadataBuildingContext buildingContext,
			Properties typeParameters) {
		if ( details.getExplicitCustomType() != null ) {
			return explicitCustomTypeResolution( details, services, state, buildingContext );
		}

		final var explicitJavaType = details.role().determineExplicitJavaType( details, services, state );
		final var explicitJdbcType = details.role().determineExplicitJdbcType( details, services, state );
		final var explicitMutabilityPlan = details.role().determineExplicitMutabilityPlan( details, services, state );
		if ( details.getExplicitTypeName() != null ) {
			return interpretExplicitlyNamedType(
					details.getExplicitTypeName(),
					explicitJavaType,
					explicitJdbcType,
					explicitMutabilityPlan,
					details.getAttributeConverterDescriptor(),
					typeParameters,
					details::setTypeParameters,
					new BasicValueJdbcTypeIndicators( details, services, state ),
					services,
					state
			);
		}
		else if ( details.isVersion() ) {
			return VersionResolution.from( details.value(), details.getTimeZoneStorageType(), services, state );
		}
		else {
			final var javaType = determineJavaType( details, services, state, explicitJavaType );
			final var converterDescriptor = getConverterDescriptor( details, services, state, javaType );
			return converterDescriptor != null
					? converterResolution( details, services, state, javaType, converterDescriptor, explicitJavaType, explicitJdbcType, explicitMutabilityPlan )
					: resolution( details, services, state, explicitJavaType, explicitJdbcType, explicitMutabilityPlan, javaType );
		}
	}

	private static BasicValue.Resolution<?> explicitCustomTypeResolution(
			BasicValueResolutionDetails details,
			MappingResolutionServices services,
			MappingResolutionState state,
			MetadataBuildingContext buildingContext) {
		final var parameters = details.buildCustomTypeProperties();
		final var customType = new CustomType<>(
				getConfiguredUserTypeBean(
						details,
						services,
						state,
						buildingContext,
						details.getExplicitCustomType(),
						details.value().getTypeAnnotation(),
						parameters
				),
				services.getTypeConfiguration()
		);
		// envers - grr
		details.value().setTypeParameters( parameters );
		return new UserTypeResolution<>( customType, null, parameters );
	}

	private static UserType<?> getConfiguredUserTypeBean(
			BasicValueResolutionDetails details,
			MappingResolutionServices services,
			MappingResolutionState state,
			MetadataBuildingContext buildingContext,
			Class<? extends UserType<?>> explicitCustomType,
			Annotation typeAnnotation,
			Properties parameters) {
		final var typeInstance = createUserTypeInstance(
				details,
				services,
				state,
				buildingContext,
				explicitCustomType,
				parameters,
				typeAnnotation
		);
		if ( typeInstance instanceof TypeConfigurationAware configurationAware ) {
			configurationAware.setTypeConfiguration( services.getTypeConfiguration() );
		}
		addParameterType( details, services, parameters, typeInstance );
		injectParameters( typeInstance, parameters );
		return typeInstance;
	}

	private static UserType<?> createUserTypeInstance(
			BasicValueResolutionDetails details,
			MappingResolutionServices services,
			MappingResolutionState state,
			MetadataBuildingContext buildingContext,
			Class<? extends UserType<?>> customType,
			Properties parameters,
			Annotation typeAnnotation) {
		final var creationContext = new UserTypeCreationContextImpl( details, services, buildingContext, parameters );
		final var typeInstance = instantiateUserType( services, state, customType, typeAnnotation, creationContext );
		if ( typeInstance instanceof AnnotationBasedUserType<?, ?> annotationBased ) {
			initializeAnnotationBasedUserType( typeAnnotation, annotationBased, creationContext );
		}
		return typeInstance;
	}

	private static void addParameterType(
			BasicValueResolutionDetails details,
			MappingResolutionServices services,
			Properties properties,
			UserType<?> typeInstance) {
		if ( typeInstance instanceof DynamicParameterizedType
				&& parseBoolean( properties.getProperty( DynamicParameterizedType.IS_DYNAMIC ) )
				&& properties.get( DynamicParameterizedType.PARAMETER_TYPE ) == null ) {
			properties.put(
					DynamicParameterizedType.PARAMETER_TYPE,
					details.createParameterType( services.getClassLoaderService() )
			);
		}
	}

	private static <A extends Annotation> void initializeAnnotationBasedUserType(
			Annotation typeAnnotation,
			AnnotationBasedUserType<A, ?> annotationBased,
			UserTypeCreationContext creationContext) {
		if ( typeAnnotation == null ) {
			throw new AnnotationException( String.format(
					"Custom type '%s' implements 'AnnotationBasedUserType' but no custom type annotation is present",
					annotationBased.getClass().getName() ) );
		}
		annotationBased.initialize( castAnnotationType( typeAnnotation, annotationBased ), creationContext );
	}

	private static <A extends Annotation> A castAnnotationType(
			Annotation typeAnnotation,
			AnnotationBasedUserType<A, ?> annotationBased ) {
		final var annotationType = annotationBased.getClass();
		final var typeArguments = typeArguments( AnnotationBasedUserType.class, annotationType );
		if ( typeArguments.length > 0
				&& typeArguments[0] instanceof Class<?> annotationClass ) {
			if ( !annotationClass.isInstance( typeAnnotation ) ) {
				throw new AnnotationException( String.format( "Annotation '%s' is not assignable to '%s'",
						annotationType.getName(), annotationClass.getName() ) );
			}
			@SuppressWarnings("unchecked") // safe, we just checked it
			final var castAnnotation = (A) typeAnnotation;
			return castAnnotation;
		}
		throw new AssertionFailure( "Could not find implementing interface" );
	}

	private static <T extends UserType<?>, A extends Annotation> T instantiateUserType(
			MappingResolutionServices services,
			MappingResolutionState state,
			Class<T> customType,
			A typeAnnotation,
			UserTypeCreationContext creationContext) {
		try {
			T userType;
			if ( typeAnnotation != null ) {
				@SuppressWarnings("unchecked") // totally safe
				final var annotationType = (Class<A>) typeAnnotation.annotationType();
				// attempt to instantiate it with the annotation and context object as constructor arguments
				userType =
						Constructors.construct( customType,
								annotationType, typeAnnotation,
								UserTypeCreationContext.class, creationContext );
				if ( userType != null ) {
					return userType;
				}
				// attempt to instantiate it with the annotation as a constructor argument
				userType = Constructors.construct( customType, annotationType, typeAnnotation );
				if ( userType != null ) {
					return userType;
				}
			}

			// attempt to instantiate it with the context object as a constructor argument
			userType = Constructors.construct( customType, UserTypeCreationContext.class, creationContext );
			if ( userType != null ) {
				return userType;
			}
		}
		catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
			throw new org.hibernate.InstantiationException( "Could not instantiate custom type", customType, e );
		}

		return state.options().isAllowExtensionsInCdi()
				? getUserTypeBean( services, customType, creationContext.getParameters() ).getBeanInstance()
				: FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( customType );
	}

	private static <T> ManagedBean<? extends T> getUserTypeBean(
			MappingResolutionServices services,
			Class<T> explicitCustomType,
			Properties properties) {
		final var producer = services.getCustomTypeProducer();
		final var managedBeanRegistry = services.getManagedBeanRegistry();
		if ( isNotEmpty( properties ) ) {
			final String name = explicitCustomType.getName() + userTypeBeanCounter++;
			return managedBeanRegistry.getBean( name, explicitCustomType, producer );
		}
		else {
			return managedBeanRegistry.getBean( explicitCustomType, producer );
		}
	}

	private record UserTypeCreationContextImpl(
			BasicValueResolutionDetails details,
			MappingResolutionServices services,
			MetadataBuildingContext buildingContext,
			Properties parameters) implements UserTypeCreationContext {

		@Override
		public MetadataBuildingContext getBuildingContext() {
			return buildingContext;
		}

		@Override
		public ServiceRegistry getServiceRegistry() {
			return services.getServiceRegistry();
		}

		@Override
		public MemberDetails getMemberDetails() {
			return details.member();
		}

		@Override
		public Properties getParameters() {
			return parameters;
		}
	}

	private static ConverterDescriptor<?,?> getConverterDescriptor(
			BasicValueResolutionDetails details,
			MappingResolutionServices services,
			MappingResolutionState state,
			JavaType<?> javaType) {
		final var converterDescriptor = details.getAttributeConverterDescriptor();
		if ( details.isSoftDelete() && details.getSoftDeleteStrategy() != SoftDeleteType.TIMESTAMP ) {
			assert converterDescriptor != null;
			@SuppressWarnings("unchecked")
			final var booleanConverterDescriptor =
					(ConverterDescriptor<Boolean, ?>) converterDescriptor;
			final var softDeleteConverterDescriptor =
					getSoftDeleteConverterDescriptor( details, services, state, booleanConverterDescriptor, javaType );
			return details.getSoftDeleteStrategy() == SoftDeleteType.ACTIVE
					? new ReversedConverterDescriptor<>( softDeleteConverterDescriptor )
					: softDeleteConverterDescriptor;
		}
		else {
			return converterDescriptor;
		}
	}

	private static ConverterDescriptor<Boolean,?> getSoftDeleteConverterDescriptor(
			BasicValueResolutionDetails details,
			MappingResolutionServices services,
			MappingResolutionState state,
			ConverterDescriptor<Boolean,?> attributeConverterDescriptor,
			JavaType<?> javaType) {
		final boolean conversionWasUnspecified =
				SoftDelete.UnspecifiedConversion.class.equals( attributeConverterDescriptor.getAttributeConverterClass() );
		if ( conversionWasUnspecified ) {
			final var jdbcType = BooleanJdbcType.INSTANCE.resolveIndicatedType(
					new BasicValueJdbcTypeIndicators( details, services, state ),
					javaType
			);
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
			BasicValueResolutionDetails details,
			MappingResolutionServices services,
			MappingResolutionState state,
			BasicJavaType explicitJavaType,
			JdbcType explicitJdbcType,
			MutabilityPlan<?> explicitMutabilityPlan,
			JavaType<?> javaType) {
		final var indicators = new BasicValueJdbcTypeIndicators( details, services, state );
		final JavaType<?> basicJavaType;
		final JdbcType jdbcType;
		if ( explicitJdbcType != null ) {
			final var typeConfiguration = services.getTypeConfiguration();
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
			throw new MappingException( "Unable to determine JavaType to use : " + details.value() );
		}

		if ( basicJavaType instanceof BasicJavaType<?> castType
				&& ( !basicJavaType.getJavaTypeClass().isEnum() || details.getEnumerationStyle() == null ) ) {
			final var autoAppliedTypeDef = state.typeDefinitionRegistry().resolveAutoApplied( castType );
			if ( autoAppliedTypeDef != null ) {
				CORE_LOGGER.trace( "BasicValue resolution matched auto-applied type definition" );
				return autoAppliedTypeDef.resolve( details.getTypeParameters(), services, state, indicators );
			}
		}

		return InferredBasicValueResolver.from(
				explicitJavaType,
				jdbcType,
				details.getResolvedJavaType(),
				() -> determineReflectedJavaType( details, services, state ),
				mutabilityPlanAccess( explicitMutabilityPlan ),
				indicators,
				details.getTable(),
				details.getColumn(),
				details.getOwnerName(),
				details.getPropertyName(),
				services,
				state
		);
	}

	private static BasicValue.Resolution<?> converterResolution(
			BasicValueResolutionDetails details,
			MappingResolutionServices services,
			MappingResolutionState state,
			JavaType<?> javaType,
			ConverterDescriptor<?,?> attributeConverterDescriptor,
			BasicJavaType<?> explicitJavaType,
			JdbcType explicitJdbcType,
			MutabilityPlan<?> explicitMutabilityPlan) {
		final var indicators = new BasicValueJdbcTypeIndicators( details, services, state );
		final var converterCreationContext = new BasicValueConverterCreationContext( services );
		final var converterResolution = NamedConverterResolution.from(
				attributeConverterDescriptor,
				explicitJavaType,
				explicitJdbcType,
				explicitMutabilityPlan,
				details.getResolvedJavaType(),
				indicators,
				converterCreationContext,
				services
		);

		if ( javaType instanceof BasicPluralJavaType<?> pluralJavaType
				&& !isAssignableFrom( attributeConverterDescriptor.getDomainValueResolvedType(),
						javaType.getJavaTypeClass() ) ) {
			final BasicType registeredElementType = converterResolution.getLegacyResolvedBasicType();
			final BasicType<?> registeredType = registeredElementType == null ? null
					: pluralJavaType.resolveType(
							services.getTypeConfiguration(),
							state.database().getDialect(),
							registeredElementType,
							details.getColumn() instanceof ColumnTypeInformation information ? information : null,
							indicators
					);
			if ( registeredType != null ) {
				services.getTypeConfiguration().getBasicTypeRegistry().register( registeredType );
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

	private static JavaType<?> determineJavaType(
			BasicValueResolutionDetails details,
			MappingResolutionServices services,
			MappingResolutionState state,
			BasicJavaType<?> explicitJavaType) {
		if ( explicitJavaType == null ) {
			final var reflectedJtd = determineReflectedJavaType( details, services, state );
			if ( reflectedJtd != null ) {
				return reflectedJtd;
			}
		}

		return explicitJavaType;
	}

	private static JavaType<?> determineReflectedJavaType(
			BasicValueResolutionDetails details,
			MappingResolutionServices services,
			MappingResolutionState state) {
		final var typeConfiguration = services.getTypeConfiguration();
		final var impliedJavaType = impliedJavaType( details, services );
		if ( impliedJavaType == null ) {
			return null;
		}
		else {
			details.setResolvedJavaType( impliedJavaType );
			return javaType( details, services, state, typeConfiguration, impliedJavaType );
		}
	}

	private static Type impliedJavaType(
			BasicValueResolutionDetails details,
			MappingResolutionServices services) {
		if ( details.getResolvedJavaType() != null ) {
			return details.getResolvedJavaType();
		}
		else if ( details.getSourceJavaType() != null ) {
			return details.getSourceJavaType().asReflectType();
		}
		else if ( details.getOwnerName() != null && details.getPropertyName() != null ) {
			return reflectedPropertyType( details.getOwnerName(), details.getPropertyName(), services.getClassLoaderService() );
		}
		else {
			return null;
		}
	}

	private static JavaType<?> javaType(
			BasicValueResolutionDetails details,
			MappingResolutionServices services,
			MappingResolutionState state,
			TypeConfiguration typeConfiguration,
			Type impliedJavaType) {
		final var javaType = typeConfiguration.getJavaTypeRegistry().findDescriptor( impliedJavaType );
		return javaType == null ? specialJavaType( details, services, state, typeConfiguration, impliedJavaType ) : javaType;
	}

	private static JavaType<?> specialJavaType(
			BasicValueResolutionDetails details,
			MappingResolutionServices services,
			MappingResolutionState state,
			TypeConfiguration typeConfiguration,
			Type impliedJavaType) {
		final var javaTypeRegistry = typeConfiguration.getJavaTypeRegistry();
		final var jdbcTypeCode = details.getConfiguredJdbcTypeCode();
		if ( jdbcTypeCode != null ) {
			final var explicitMutabilityPlan = details.role().determineExplicitMutabilityPlan( details, services, state );
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
			Type impliedJavaType) {
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
			MappingResolutionServices services,
			MappingResolutionState state) {

		final var typeConfiguration = services.getTypeConfiguration();
		final var converterCreationContext = new BasicValueConverterCreationContext( services );

		if ( name.startsWith( BasicTypeImpl.EXTERNALIZED_PREFIX )
			|| name.startsWith( ConvertedBasicTypeImpl.EXTERNALIZED_PREFIX ) ) {
			return getNamedBasicTypeResolution(
					state.resolveAdHocBasicType( name ),
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
				state.typeDefinitionRegistry()
						.resolve( name );
		if ( typeDefinition != null ) {
			final var resolution = typeDefinition.resolve(
					localTypeParams,
					explicitMutabilityPlan,
					services,
					state,
					stdIndicators
			);
			combinedParameterConsumer.accept( resolution.getCombinedTypeParameters() );
			return resolution;
		}

		try {
			final var typeNamedClass = services.getClassLoaderAccess().classForName( name );
			if ( isEmpty( localTypeParams ) ) {
				final var implicitDefinition =
						new TypeDefinition( name, typeNamedClass, null, null );
				state.typeDefinitionRegistry().register( implicitDefinition );
				return implicitDefinition.resolve(
						localTypeParams,
						explicitMutabilityPlan,
						services,
						state,
						stdIndicators
				);
			}

			return TypeDefinition.createLocalResolution( name, typeNamedClass, localTypeParams, services, state );
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
	 * on {@link BasicValue}.  The binder/materializer supplies an {@link BasicValueResolutionDetails},
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

		@Nullable BasicJavaType<?> determineExplicitJavaType(
				BasicValueResolutionDetails details,
				MappingResolutionServices services,
				MappingResolutionState state) {
			if ( details.explicitJavaType() != null ) {
				return details.explicitJavaType();
			}

			final var member = details.member();
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
							services.getModelsContext()
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
				return services.getManagedBeanRegistry()
						.getBean( javaClass )
						.getBeanInstance();
			}

			if ( this == ANY_KEY ) {
				final var typeAnn = member.locateAnnotationUsage(
						AnyKeyType.class,
						services.getModelsContext()
				);
				if ( typeAnn != null ) {
					final var registeredType = services.getTypeConfiguration()
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

		@Nullable JdbcType determineExplicitJdbcType(
				BasicValueResolutionDetails details,
				MappingResolutionServices services,
				MappingResolutionState state) {
			if ( details.explicitJdbcType() != null ) {
				return details.explicitJdbcType();
			}

			final var member = details.member();
			if ( member == null ) {
				return null;
			}

			final JdbcTypeSelection selection = switch ( this ) {
				case MAP_KEY -> mapKeyJdbcTypeSelection( member );
				case LIST_INDEX -> listIndexJdbcTypeSelection( member );
				case COLLECTION_ID -> collectionIdJdbcTypeSelection( member );
				case ANY_KEY -> {
					final var modelsContext = services.getModelsContext();
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
				details.setConfiguredJdbcTypeCode( selection.jdbcTypeCode() );
				final var jdbcTypeRegistry = services.getTypeConfiguration().getJdbcTypeRegistry();
				return jdbcTypeRegistry.getConstructor( selection.jdbcTypeCode() ) == null
						? jdbcTypeRegistry.getDescriptor( selection.jdbcTypeCode() )
						: null;
			}

			return null;
		}

		@Nullable MutabilityPlan<?> determineExplicitMutabilityPlan(
				BasicValueResolutionDetails details,
				MappingResolutionServices services,
				MappingResolutionState state) {
			if ( details.explicitMutabilityPlan() != null ) {
				return details.explicitMutabilityPlan();
			}
			if ( details.attributeMutabilityPlanClass() != null ) {
				return instantiateMutabilityPlan( details.member(), details.attributeMutabilityPlanClass() );
			}
			if ( details.attributeImmutable() ) {
				return org.hibernate.type.descriptor.java.ImmutableMutabilityPlan.instance();
			}

			final var member = details.member();
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

	private record BasicValueConverterCreationContext(
			MappingResolutionServices services) implements JpaAttributeConverterCreationContext {

		@Override
		public ManagedBeanRegistry getManagedBeanRegistry() {
			return services.getManagedBeanRegistry();
		}

		@Override
		public TypeConfiguration getTypeConfiguration() {
			return services.getTypeConfiguration();
		}
	}

	private record BasicValueJdbcTypeIndicators(
			BasicValueResolutionDetails details,
			MappingResolutionServices services,
			MappingResolutionState state) implements JdbcTypeIndicators {

		@Override
		public TypeConfiguration getTypeConfiguration() {
			return services.getTypeConfiguration();
		}

		@Override
		public Dialect getDialect() {
			return state.database().getDialect();
		}

		@Override
		public EnumType getEnumeratedType() {
			return details.getEnumeratedType();
		}

		@Override
		public TemporalType getTemporalPrecision() {
			return details.getTemporalPrecision();
		}

		@Override
		public boolean isNationalized() {
			return details.isNationalized();
		}

		@Override
		public boolean isLob() {
			return details.isLob();
		}

		@Override
		public long getColumnLength() {
			return details.getColumnLength();
		}

		@Override
		public int getColumnPrecision() {
			return details.getColumnPrecision();
		}

		@Override
		public int getColumnScale() {
			return details.getColumnScale();
		}

		@Override
		public boolean isPreferJavaTimeJdbcTypesEnabled() {
			return state.options().isPreferJavaTimeJdbcTypesEnabled();
		}

		@Override
		public boolean isPreferNativeEnumTypesEnabled() {
			return state.options().isPreferNativeEnumTypesEnabled();
		}

		@Override
		public int getPreferredSqlTypeCodeForBoolean() {
			return resolveJdbcTypeCode( state.options().getPreferredSqlTypeCodeForBoolean() );
		}

		@Override
		public int getPreferredSqlTypeCodeForDuration() {
			return resolveJdbcTypeCode( state.options().getPreferredSqlTypeCodeForDuration() );
		}

		@Override
		public int getPreferredSqlTypeCodeForUuid() {
			return resolveJdbcTypeCode( state.options().getPreferredSqlTypeCodeForUuid() );
		}

		@Override
		public int getPreferredSqlTypeCodeForInstant() {
			return resolveJdbcTypeCode( state.options().getPreferredSqlTypeCodeForInstant() );
		}

		@Override
		public int getPreferredSqlTypeCodeForArray() {
			return resolveJdbcTypeCode( state.options().getPreferredSqlTypeCodeForArray() );
		}

		@Override
		public int resolveJdbcTypeCode(int jdbcTypeCode) {
			final var aggregateColumn = details.value().getAggregateColumn();
			return aggregateColumn == null
					? jdbcTypeCode
					: getDialect().getAggregateSupport()
							.aggregateComponentSqlTypeCode(
									aggregateColumn.getJdbcType( state.mappingContext() )
											.getDefaultSqlTypeCode(),
									jdbcTypeCode
							);
		}

		@Override
		public TimeZoneStorageStrategy getDefaultTimeZoneStorageStrategy() {
			return details.getTimeZoneStorageType() == null
					? state.options().getDefaultTimeZoneStorage()
					: switch ( details.getTimeZoneStorageType() ) {
						case COLUMN -> TimeZoneStorageStrategy.COLUMN;
						case NATIVE -> TimeZoneStorageStrategy.NATIVE;
						case NORMALIZE -> TimeZoneStorageStrategy.NORMALIZE;
						case NORMALIZE_UTC -> TimeZoneStorageStrategy.NORMALIZE_UTC;
						case AUTO, DEFAULT -> state.options().getDefaultTimeZoneStorage();
					};
		}

		@Override
		public Integer getExplicitJdbcTypeCode() {
			return details.getConfiguredJdbcTypeCode() == null ? getPreferredSqlTypeCodeForArray()
					: details.getConfiguredJdbcTypeCode();
		}
	}
}
