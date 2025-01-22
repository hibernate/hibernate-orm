/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyClass;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.MapKeyTemporal;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Version;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.AnnotationException;
import org.hibernate.MappingException;
import org.hibernate.type.TimeZoneStorageStrategy;
import org.hibernate.annotations.*;
import org.hibernate.boot.internal.AnyKeyType;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.spi.AccessType;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.NationalizationSupport;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.models.spi.TypeDetails;
import org.hibernate.resource.beans.internal.FallbackBeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.java.Immutability;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.internal.ParameterizedTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.DynamicParameterizedType;
import org.hibernate.usertype.UserType;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static java.util.Collections.emptyMap;
import static org.hibernate.boot.model.internal.AnnotationHelper.extractParameterMap;
import static org.hibernate.boot.model.internal.TableBinder.linkJoinColumnWithValueOverridingNameIfImplicit;
import static org.hibernate.internal.log.DeprecationLogger.DEPRECATION_LOGGER;

/**
 * A stateful binder responsible for creating instances of {@link BasicValue}.
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 */
public class BasicValueBinder implements JdbcTypeIndicators {

	// todo (6.0) : In light of how we want to build Types (specifically BasicTypes) moving
	//      forward this class should undergo major changes: see the comments in #setType
	//		but as always the "design" of these classes make it unclear exactly how to change it properly.

	public enum Kind {
		ATTRIBUTE( ValueMappingAccess.INSTANCE ),
		ANY_DISCRIMINATOR( AnyDiscriminatorMappingAccess.INSTANCE ),
		ANY_KEY( AnyKeyMappingAccess.INSTANCE ),
		MAP_KEY( MapKeyMappingAccess.INSTANCE ),
		COLLECTION_ELEMENT( ValueMappingAccess.INSTANCE ),
		COLLECTION_ID( CollectionIdMappingAccess.INSTANCE ),
		LIST_INDEX( ListIndexMappingAccess.INSTANCE );

		private final BasicMappingAccess mappingAccess;

		Kind(BasicMappingAccess mappingAccess) {
			this.mappingAccess = mappingAccess;
		}
	}

	private final Kind kind;
	private final Component aggregateComponent;
	private final MetadataBuildingContext buildingContext;


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// in-flight info

	private Class<? extends UserType<?>> explicitCustomType;
	private Map<String,String> explicitLocalCustomTypeParams;
	private Annotation explicitCustomTypeAnnotation;

	private Function<TypeConfiguration, JdbcType> explicitJdbcTypeAccess;
	private Function<TypeConfiguration, BasicJavaType<?>> explicitJavaTypeAccess;
	private Function<TypeConfiguration, MutabilityPlan<?>> explicitMutabilityAccess;
	private Function<TypeConfiguration, java.lang.reflect.Type> implicitJavaTypeAccess;

	private MemberDetails memberDetails;
	private AccessType accessType;

	private ConverterDescriptor<?,?> converterDescriptor;

	private boolean isNationalized;
	private boolean isLob;
	private EnumType enumType;
	private TemporalType temporalPrecision;
	private TimeZoneStorageType timeZoneStorageType;
	private boolean partitionKey;
	private Integer jdbcTypeCode;

	private Table table;
	private AnnotatedColumns columns;

	private BasicValue basicValue;

	private String persistentClassName;
	private String returnedClassName;
	private String referencedEntityName; // only used for @MapsId or @IdClass

	public BasicValueBinder(Kind kind, MetadataBuildingContext buildingContext) {
		this( kind, null, buildingContext );
	}

	public BasicValueBinder(Kind kind, Component aggregateComponent, MetadataBuildingContext buildingContext) {
		assert kind != null;
		assert buildingContext != null;

		this.kind = kind;
		this.aggregateComponent = aggregateComponent;
		this.buildingContext = buildingContext;
	}

	protected ModelsContext getSourceModelContext() {
		return buildingContext.getBootstrapContext().getModelsContext();
	}

	private InFlightMetadataCollector getMetadataCollector() {
		return buildingContext.getMetadataCollector();
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return buildingContext.getBootstrapContext().getTypeConfiguration();
	}

	@Override
	public TimeZoneStorageStrategy getDefaultTimeZoneStorageStrategy() {
		return BasicValue.timeZoneStorageStrategy( timeZoneStorageType, buildingContext );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqlTypeDescriptorIndicators

	@Override
	public EnumType getEnumeratedType() {
		return enumType;
	}

	@Override
	public boolean isLob() {
		if ( isLob ) {
			return true;
		}

		if ( explicitJdbcTypeAccess != null ) {
			final var jdbcType = explicitJdbcTypeAccess.apply( getTypeConfiguration() );
			if ( jdbcType != null ) {
				return jdbcType.isLob();
			}
		}
		return false;
	}

	@Override
	public TemporalType getTemporalPrecision() {
		return temporalPrecision;
	}

	@Override
	public boolean isPreferJavaTimeJdbcTypesEnabled() {
		return buildingContext.isPreferJavaTimeJdbcTypesEnabled();
	}

	@Override
	public boolean isPreferNativeEnumTypesEnabled() {
		return buildingContext.isPreferNativeEnumTypesEnabled();
	}

	@Override
	public int getPreferredSqlTypeCodeForBoolean() {
		return resolveJdbcTypeCode( buildingContext.getPreferredSqlTypeCodeForBoolean() );
	}

	@Override
	public int getPreferredSqlTypeCodeForDuration() {
		return resolveJdbcTypeCode( buildingContext.getPreferredSqlTypeCodeForDuration() );
	}

	@Override
	public int getPreferredSqlTypeCodeForUuid() {
		return resolveJdbcTypeCode( buildingContext.getPreferredSqlTypeCodeForUuid() );
	}

	@Override
	public int getPreferredSqlTypeCodeForInstant() {
		return resolveJdbcTypeCode( buildingContext.getPreferredSqlTypeCodeForInstant() );
	}

	@Override
	public int getPreferredSqlTypeCodeForArray() {
		return resolveJdbcTypeCode( buildingContext.getPreferredSqlTypeCodeForArray() );
	}

	@Override
	public int resolveJdbcTypeCode(int jdbcTypeCode) {
		return aggregateComponent == null
				? jdbcTypeCode
				: getAggregateComponentTypeCode( jdbcTypeCode );
	}

	private int getAggregateComponentTypeCode(int jdbcTypeCode) {
		final Integer aggregateColumnSqlTypeCode =
				aggregateComponent.getAggregateColumn().getSqlTypeCode();
		return getDialect().getAggregateSupport()
				.aggregateComponentSqlTypeCode( aggregateColumnSqlTypeCode, jdbcTypeCode );
	}

	@Override
	public boolean isNationalized() {
		if ( isNationalized ) {
			return true;
		}
		else if ( explicitJdbcTypeAccess != null ) {
			final var jdbcType = explicitJdbcTypeAccess.apply( getTypeConfiguration() );
			return jdbcType != null && jdbcType.isNationalized();
		}
		else {
			return false;
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// in-flight handling

	public void setVersion(boolean isVersion) {
		if ( isVersion && basicValue != null ) {
			basicValue.makeVersion();
		}
	}

	void setReferencedEntityName(String referencedEntityName) {
		this.referencedEntityName = referencedEntityName;
	}

	public void setReturnedClassName(String returnedClassName) {
		this.returnedClassName = returnedClassName;
	}

	public void setTable(Table table) {
		this.table = table;
	}

	public void setColumns(AnnotatedColumns columns) {
		this.columns = columns;
	}

	public void setPersistentClassName(String persistentClassName) {
		this.persistentClassName = persistentClassName;
	}

	public void setAccessType(AccessType accessType) {
		this.accessType = accessType;
	}

	private static JdbcType getDescriptor(TypeConfiguration typeConfiguration, int code) {
		return typeConfiguration.getJdbcTypeRegistry().getDescriptor( code );
	}

	public void setType(MemberDetails memberDetails, TypeDetails typeDetails) {
		setType( memberDetails, typeDetails, null, null );
	}

	public void setType(
			MemberDetails memberDetails,
			TypeDetails typeDetails,
			@Nullable String declaringClassName,
			@Nullable ConverterDescriptor<?,?> converterDescriptor) {
		this.memberDetails = memberDetails;
		if ( typeDetails != null || memberDetails.isArray() ) {
			assert columns != null
				: "BasicValueBinder.setColumns must be called before BasicValueBinder.setType";

			if ( kind != Kind.LIST_INDEX && kind != Kind.MAP_KEY ) {
				isLob = memberDetails.hasDirectAnnotationUsage( Lob.class );
			}

			if ( getDialect().getNationalizationSupport() == NationalizationSupport.EXPLICIT ) {
				isNationalized = isNationalized( memberDetails );
			}

			final boolean customType;
			if ( converterDescriptor != null ) {
				applyJpaConverter( memberDetails, converterDescriptor );
				customType = false;
			}
			else {
				customType = applyCustomType( memberDetails, typeDetails );
			}
			if ( !customType ) {
				prepareValue( memberDetails, typeDetails, declaringClassName );
			}
		}
		// else we cannot guess anything
	}

	private boolean isNationalized(MemberDetails memberDetails) {
		return buildingContext.getBuildingOptions().useNationalizedCharacterData()
			|| memberDetails.locateAnnotationUsage( Nationalized.class, getSourceModelContext() ) != null;
	}

	private boolean applyCustomType(MemberDetails memberDetails, TypeDetails typeDetails) {
		final var modelContext = getSourceModelContext();
		final var userTypeImpl = kind.mappingAccess.customType( memberDetails, modelContext );
		if ( userTypeImpl != null ) {
			this.explicitCustomType = userTypeImpl;
			this.explicitLocalCustomTypeParams = kind.mappingAccess.customTypeParameters( memberDetails, modelContext );
			this.explicitCustomTypeAnnotation = kind.mappingAccess.customTypeAnnotation( memberDetails, modelContext );
			// An explicit custom UserType has top precedence when we get to BasicValue resolution.
			return true;
		}
		final var modelClassDetails = memberDetails.isArray() ? memberDetails.getElementType() : typeDetails;
		if ( modelClassDetails != null ) {
			final var basicClass = modelClassDetails.determineRawClass().toJavaClass();
			final var registeredUserTypeImpl =
					getMetadataCollector().findRegisteredUserType( basicClass );
			if ( registeredUserTypeImpl != null ) {
				this.explicitCustomType = registeredUserTypeImpl;
				this.explicitLocalCustomTypeParams = emptyMap();
				return true;
			}
		}
		return false;
	}

	private void prepareValue(MemberDetails value, TypeDetails typeDetails, @Nullable String declaringClassName) {
		switch ( kind ) {
			case ATTRIBUTE:
				prepareBasicAttribute( declaringClassName, value, typeDetails );
				break;
			case ANY_DISCRIMINATOR:
				prepareAnyDiscriminator( value );
				break;
			case ANY_KEY:
				prepareAnyKey( value );
				break;
			case COLLECTION_ID:
				prepareCollectionId( value );
				break;
			case LIST_INDEX:
				prepareListIndex( value );
				break;
			case MAP_KEY:
				prepareMapKey( value, typeDetails );
				break;
			case COLLECTION_ELEMENT:
				prepareCollectionElement( value, typeDetails );
				break;
			default:
				throw new IllegalArgumentException( "Unexpected binder type : " + kind );
		}
	}

	private void prepareCollectionId(MemberDetails attribute) {
		final var collectionId = attribute.getDirectAnnotationUsage( CollectionId.class );
		if ( collectionId == null ) {
			throw new MappingException( "idbag mapping missing @CollectionId" );
		}

		final boolean useDeferredBeanContainerAccess = useDeferredBeanContainerAccess();
		final var beanRegistry = getManagedBeanRegistry();

		implicitJavaTypeAccess = typeConfiguration -> null;

		final var modelContext = getSourceModelContext();

		explicitJavaTypeAccess = typeConfiguration -> {
			final var idJavaClass = attribute.locateAnnotationUsage( CollectionIdJavaClass.class, modelContext );
			if ( idJavaClass != null ) {
				return (BasicJavaType<?>)
						buildingContext.getBootstrapContext().getTypeConfiguration()
								.getJavaTypeRegistry().resolveDescriptor( idJavaClass.idType() );
			}
			final var idJavaType = attribute.locateAnnotationUsage( CollectionIdJavaType.class, modelContext );
			if ( idJavaType != null ) {
				final var basicJavaTypeClass = idJavaType.value();
				if ( basicJavaTypeClass != null ) {
					return useDeferredBeanContainerAccess
							? FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( basicJavaTypeClass )
							: beanRegistry.getBean( basicJavaTypeClass ).getBeanInstance();
				}
			}

			return null;
		};

		explicitJdbcTypeAccess = typeConfiguration -> {
			final var idJdbcType = attribute.locateAnnotationUsage( CollectionIdJdbcType.class, modelContext );
			if ( idJdbcType != null ) {
				final var jdbcTypeClass = idJdbcType.value();
				if ( jdbcTypeClass != null ) {
					return useDeferredBeanContainerAccess
							? FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( jdbcTypeClass )
							: beanRegistry.getBean( jdbcTypeClass ).getBeanInstance();
				}
			}

			final var idJdbcTypeCode = attribute.locateAnnotationUsage( CollectionIdJdbcTypeCode.class, modelContext );
			if ( idJdbcTypeCode != null ) {
				final int code = idJdbcTypeCode.value();
				if ( code != Integer.MIN_VALUE ) {
					return getDescriptor( typeConfiguration, code );
				}
			}

			return null;
		};

		explicitMutabilityAccess = typeConfiguration -> {
			final var idMutability = attribute.locateAnnotationUsage( CollectionIdMutability.class, modelContext );
			if ( idMutability != null ) {
				final var mutabilityClass = idMutability.value();
				if ( mutabilityClass != null ) {
					return resolveMutability( mutabilityClass );
				}
			}

			// see if the value's type Class is annotated with mutability-related annotations
			if ( implicitJavaTypeAccess != null ) {
				final var attributeType = ReflectHelper.getClass( implicitJavaTypeAccess.apply( typeConfiguration ) );
				if ( attributeType != null ) {
					final var attributeTypeMutability = attributeType.getAnnotation( Mutability.class );
					if ( attributeTypeMutability != null ) {
						return resolveMutability( attributeTypeMutability.value() );
					}
					if ( attributeType.isAnnotationPresent( Immutable.class ) ) {
						return ImmutableMutabilityPlan.instance();
					}
				}
			}

			// if there is a converter, check it for mutability-related annotations
			if ( converterDescriptor != null ) {
				final var attributeConverterClass = converterDescriptor.getAttributeConverterClass();
				final var converterMutability = attributeConverterClass.getAnnotation( Mutability.class );
				if ( converterMutability != null ) {
					return resolveMutability( converterMutability.value() );
				}
				if ( attributeConverterClass.isAnnotationPresent( Immutable.class ) ) {
					return ImmutableMutabilityPlan.instance();
				}
			}

			// if there is a UserType, see if its Class is annotated with mutability-related annotations
			final var customTypeImpl = Kind.ATTRIBUTE.mappingAccess.customType( attribute, modelContext );
			if ( customTypeImpl != null ) {
				final var customTypeMutability = customTypeImpl.getAnnotation( Mutability.class );
				if ( customTypeMutability != null ) {
					return resolveMutability( customTypeMutability.value() );
				}
				if ( customTypeImpl.isAnnotationPresent( Immutable.class ) ) {
					return ImmutableMutabilityPlan.instance();
				}
			}

			// generally, this will trigger usage of the `JavaType#getMutabilityPlan`
			return null;
		};
	}

	private boolean useDeferredBeanContainerAccess() {
		return !buildingContext.getBuildingOptions().isAllowExtensionsInCdi();
	}

	private ManagedBeanRegistry getManagedBeanRegistry() {
		return buildingContext.getBootstrapContext().getManagedBeanRegistry();
	}

	private void prepareMapKey(
			MemberDetails attribute,
			TypeDetails explicitMapKeyTypeDetails) {
		final var mapKeyClass =
				explicitMapKeyTypeDetails == null
						? attribute.getMapKeyType()
						: explicitMapKeyTypeDetails;
		implicitJavaTypeAccess = typeConfiguration -> mapKeyClass.determineRawClass().toJavaClass();

		final var mapKeyEnumerated = attribute.getDirectAnnotationUsage( MapKeyEnumerated.class );
		if ( mapKeyEnumerated != null ) {
			enumType = mapKeyEnumerated.value();
		}

		//noinspection deprecation
		final var mapKeyTemporal = attribute.getDirectAnnotationUsage( MapKeyTemporal.class );
		if ( mapKeyTemporal != null ) {
			temporalPrecision = mapKeyTemporal.value();
		}

		final boolean useDeferredBeanContainerAccess = useDeferredBeanContainerAccess();

		final var modelContext = getSourceModelContext();

		explicitJdbcTypeAccess = typeConfiguration -> {
			final var mapKeyJdbcType = attribute.locateAnnotationUsage( MapKeyJdbcType.class, modelContext );
			if ( mapKeyJdbcType != null ) {
				final var jdbcTypeClass = mapKeyJdbcType.value();
				if ( jdbcTypeClass != null ) {
					return useDeferredBeanContainerAccess
							? FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( jdbcTypeClass )
							: getManagedBeanRegistry().getBean( jdbcTypeClass ).getBeanInstance();
				}
			}

			final var jdbcTypeCodeAnn = attribute.locateAnnotationUsage( MapKeyJdbcTypeCode.class, modelContext );
			if ( jdbcTypeCodeAnn != null ) {
				final int jdbcTypeCode = jdbcTypeCodeAnn.value();
				if ( jdbcTypeCode != Integer.MIN_VALUE ) {
					return getDescriptor( typeConfiguration, jdbcTypeCode );
				}
			}

			return null;
		};

		explicitJavaTypeAccess = typeConfiguration -> {
			final var mapKeyJavaType = attribute.locateAnnotationUsage( MapKeyJavaType.class, modelContext );
			if ( mapKeyJavaType != null ) {
				final var basicJavaTypeClass = mapKeyJavaType.value();
				if ( basicJavaTypeClass != null ) {
					return useDeferredBeanContainerAccess
							? FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( basicJavaTypeClass )
							: getManagedBeanRegistry().getBean( basicJavaTypeClass ).getBeanInstance();
				}
			}

			final var mapKeyClassAnn = attribute.getDirectAnnotationUsage( MapKeyClass.class );
			if ( mapKeyClassAnn != null ) {
				return (BasicJavaType<?>)
						typeConfiguration.getJavaTypeRegistry()
								.resolveDescriptor( mapKeyClassAnn.value() );
			}
			else {
				return null;
			}
		};

		explicitMutabilityAccess = typeConfiguration -> {
			final var mutabilityAnn = attribute.locateAnnotationUsage( MapKeyMutability.class, modelContext );
			if ( mutabilityAnn != null ) {
				final var mutabilityClass = mutabilityAnn.value();
				if ( mutabilityClass != null ) {
					return resolveMutability( mutabilityClass );
				}
			}

			// see if the value's Java Class is annotated with mutability-related annotations
			if ( implicitJavaTypeAccess != null ) {
				final var attributeType = ReflectHelper.getClass( implicitJavaTypeAccess.apply( typeConfiguration ) );
				if ( attributeType != null ) {
					final var attributeTypeMutabilityAnn = attributeType.getAnnotation( Mutability.class );
					if ( attributeTypeMutabilityAnn != null ) {
						return resolveMutability( attributeTypeMutabilityAnn.value() );
					}
					if ( attributeType.isAnnotationPresent( Immutable.class ) ) {
						return ImmutableMutabilityPlan.instance();
					}
				}
			}

			// if the value is converted, see if converter Class is annotated with mutability-related annotations
			if ( converterDescriptor != null ) {
				final var attributeConverterClass = converterDescriptor.getAttributeConverterClass();
				final var converterMutability = attributeConverterClass.getAnnotation( Mutability.class );
				if ( converterMutability != null ) {
					return resolveMutability( converterMutability.value() );
				}
				if ( attributeConverterClass.isAnnotationPresent( Immutable.class ) ) {
					return ImmutableMutabilityPlan.instance();
				}
			}

			// if there is a UserType, see if its Class is annotated with mutability-related annotations
			final var customTypeImpl = Kind.MAP_KEY.mappingAccess.customType( attribute, modelContext );
			if ( customTypeImpl != null ) {
				final var customTypeMutabilityAnn = customTypeImpl.getAnnotation( Mutability.class );
				if ( customTypeMutabilityAnn != null ) {
					return resolveMutability( customTypeMutabilityAnn.value() );
				}
				if ( customTypeImpl.isAnnotationPresent( Immutable.class ) ) {
					return ImmutableMutabilityPlan.instance();
				}
			}

			// generally, this will trigger usage of the JavaType.getMutabilityPlan
			return null;
		};
	}

	private void prepareListIndex(MemberDetails attribute) {
		implicitJavaTypeAccess = typeConfiguration -> Integer.class;

		final boolean useDeferredBeanContainerAccess = useDeferredBeanContainerAccess();
		final var beanRegistry = getManagedBeanRegistry();

		final var modelContext = getSourceModelContext();

		explicitJavaTypeAccess = typeConfiguration -> {
			final var javaType = attribute.locateAnnotationUsage( ListIndexJavaType.class, modelContext );
			if ( javaType != null ) {
				final var javaTypeClass = javaType.value();
				if ( javaTypeClass != null ) {
					return useDeferredBeanContainerAccess
							? FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( javaTypeClass )
							: beanRegistry.getBean( javaTypeClass ).getBeanInstance();
				}
			}

			return null;
		};

		explicitJdbcTypeAccess = typeConfiguration -> {
			final var jdbcType = attribute.locateAnnotationUsage( ListIndexJdbcType.class, modelContext );
			if ( jdbcType != null ) {
				final var jdbcTypeClass = jdbcType.value();
				if ( jdbcTypeClass != null ) {
					return useDeferredBeanContainerAccess
							? FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( jdbcTypeClass )
							: beanRegistry.getBean( jdbcTypeClass ).getBeanInstance();
				}
			}

			final var jdbcTypeCode = attribute.locateAnnotationUsage( ListIndexJdbcTypeCode.class, modelContext );
			if ( jdbcTypeCode != null ) {
				return getDescriptor( typeConfiguration, jdbcTypeCode.value() );
			}
			else {
				return null;
			}
		};
	}

	private void prepareCollectionElement(
			MemberDetails attribute,
			TypeDetails explicitElementTypeDetails) {
		final var elementTypeDetails =
				explicitElementTypeDetails == null && attribute.isArray()
						? attribute.getElementType()
						: explicitElementTypeDetails;
		final var javaType = elementTypeDetails.determineRawClass().toJavaClass();

		implicitJavaTypeAccess = typeConfiguration -> javaType;

		//noinspection deprecation
		final var temporal = attribute.getDirectAnnotationUsage( Temporal.class );
		if ( temporal != null ) {
			//noinspection deprecation
			DEPRECATION_LOGGER.deprecatedAnnotation( Temporal.class, attribute.getName() );
			temporalPrecision = temporal.value();
			if ( temporalPrecision == null ) {
				throw new IllegalStateException(
						"No jakarta.persistence.TemporalType defined for @jakarta.persistence.Temporal " +
								"associated with attribute " + attribute.getName()
				);
			}
		}
		else {
			temporalPrecision = null;
		}

		if ( ReflectHelper.getClass( javaType ).isEnum() ) {
			final var enumerated = attribute.getDirectAnnotationUsage( Enumerated.class );
			if ( enumerated != null ) {
				enumType = enumerated.value();
				if ( enumType == null ) {
					throw new IllegalStateException(
							"jakarta.persistence.EnumType was null on @jakarta.persistence.Enumerated " +
									" associated with attribute " + attribute.getName()
					);
				}
			}
		}
		else {
			enumType = null;
		}

		normalSupplementalDetails( attribute);

		// layer in support for JPA's approach for specifying a specific Java type for the collection elements
		final var elementCollection = attribute.getDirectAnnotationUsage( ElementCollection.class );
		if ( elementCollection != null ) {
			final var targetClassDetails = elementCollection.targetClass();
			if ( targetClassDetails != void.class) {
				final var original = explicitJavaTypeAccess;
				explicitJavaTypeAccess = typeConfiguration -> {
					final var originalResult = original.apply( typeConfiguration );
					if ( originalResult != null ) {
						return originalResult;
					}
					else {
						return (BasicJavaType<?>)
								typeConfiguration.getJavaTypeRegistry()
										.resolveDescriptor( targetClassDetails );
					}
				};
			}
		}
	}

	private void prepareBasicAttribute(
			String declaringClassName,
			MemberDetails attribute,
			TypeDetails attributeType) {
		implicitJavaTypeAccess = typeConfiguration ->
				attributeType.getTypeKind() == TypeDetails.Kind.PARAMETERIZED_TYPE
						? ParameterizedTypeImpl.from( attributeType.asParameterizedType() )
						: attributeType.determineRawClass().toJavaClass();

		//noinspection deprecation
		final var temporal = attribute.getDirectAnnotationUsage( Temporal.class );
		if ( temporal != null ) {
			//noinspection deprecation
			DEPRECATION_LOGGER.deprecatedAnnotation( Temporal.class,
					declaringClassName + "." + attribute.getName() );
			temporalPrecision = temporal.value();
			if ( temporalPrecision == null ) {
				throw new IllegalStateException(
						"No jakarta.persistence.TemporalType defined for @jakarta.persistence.Temporal " +
								"associated with attribute " + declaringClassName + "." + attribute.getName()
				);
			}
		}
		else {
			temporalPrecision = null;
		}

		final var enumerated = attribute.getDirectAnnotationUsage( Enumerated.class );
		if ( enumerated != null ) {
			enumType = enumerated.value();
			final var javaTypeClass = attributeType.determineRawClass().toJavaClass();
			if ( canUseEnumerated( attributeType, javaTypeClass ) ) {
				if ( enumType == null ) {
					throw new IllegalStateException(
							"jakarta.persistence.EnumType was null on @jakarta.persistence.Enumerated " +
									" associated with attribute " + declaringClassName + "." + attribute.getName()
					);
				}
			}
			else {
				throw new AnnotationException(
						String.format(
								"Property '%s.%s' is annotated '@Enumerated' but its type '%s' is not an enum",
								declaringClassName,
								attribute.getName(),
								attributeType.getName()
						)
				);
			}
		}
		else {
			enumType = null;
		}

		normalSupplementalDetails( attribute );
	}

	private boolean canUseEnumerated(TypeDetails javaType, Class<?> javaTypeClass) {
		if ( javaTypeClass.isEnum()
				|| javaTypeClass.isArray() && javaTypeClass.getComponentType().isEnum() ) {
			return true;
		}
		else if ( javaType.isImplementor( Collection.class ) ) {
			final var typeArguments = javaType.asParameterizedType().getArguments();
			return !typeArguments.isEmpty() && typeArguments.get( 0 ).isImplementor( Enum.class );
		}
		else {
			return false;
		}
	}

	private void prepareAnyDiscriminator(MemberDetails memberDetails) {
		final var anyDiscriminator =
				memberDetails.locateAnnotationUsage( AnyDiscriminator.class, getSourceModelContext() );
		implicitJavaTypeAccess = typeConfiguration -> {
			if ( anyDiscriminator != null ) {
				return switch ( anyDiscriminator.value() ) {
					case CHAR -> Character.class;
					case INTEGER -> Integer.class;
					default -> String.class;
				};
			}
			else {
				return String.class;
			}
		};

		normalJdbcTypeDetails( memberDetails);
		normalMutabilityDetails( memberDetails );

		// layer AnyDiscriminator into the JdbcType resolution
		final var originalJdbcTypeResolution = explicitJdbcTypeAccess;
		explicitJdbcTypeAccess = typeConfiguration -> {
			final var originalResolution = originalJdbcTypeResolution.apply( typeConfiguration );
			return originalResolution != null
					? originalResolution
					: typeConfiguration.getJavaTypeRegistry()
							.getDescriptor( implicitJavaTypeAccess.apply( typeConfiguration ) )
							.getRecommendedJdbcType( typeConfiguration.getCurrentBaseSqlTypeIndicators() );
		};
	}

	private void prepareAnyKey(MemberDetails member) {
		implicitJavaTypeAccess = typeConfiguration -> null;

		final boolean useDeferredBeanContainerAccess = useDeferredBeanContainerAccess();

		final var context = getSourceModelContext();

		explicitJavaTypeAccess = typeConfiguration -> {
			final var anyKeyJavaType = member.locateAnnotationUsage( AnyKeyJavaType.class, context );
			if ( anyKeyJavaType != null ) {
				final var implClass = anyKeyJavaType.value();
				if ( implClass != null ) {
					return useDeferredBeanContainerAccess
							? FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( implClass )
							: getManagedBeanRegistry().getBean( implClass ).getBeanInstance();
				}
			}

			final var anyKeyJavaClass = member.locateAnnotationUsage( AnyKeyJavaClass.class, context );
			if ( anyKeyJavaClass != null ) {
				return (BasicJavaType<?>)
						typeConfiguration.getJavaTypeRegistry()
								.resolveDescriptor( anyKeyJavaClass.value() );
			}

			// mainly used in XML interpretation
			final var anyKeyType = member.locateAnnotationUsage( AnyKeyType.class, context );
			if ( anyKeyType != null ) {
				final String namedType = anyKeyType.value();
				final var registeredType =
						typeConfiguration.getBasicTypeRegistry()
								.getRegisteredType( namedType );
				if ( registeredType == null ) {
					throw new MappingException( "Unrecognized @AnyKeyType value - " + namedType );
				}
				else {
					return (BasicJavaType<?>) registeredType.getJavaTypeDescriptor();
				}
			}

			throw new MappingException("Could not determine key type for '@Any' mapping (specify '@AnyKeyJavaType' or '@AnyKeyJavaClass')");
		};

		explicitJdbcTypeAccess = typeConfiguration -> {
			final var anyKeyJdbcType = member.locateAnnotationUsage( AnyKeyJdbcType.class, context );
			if ( anyKeyJdbcType != null ) {
				final var jdbcTypeClass = anyKeyJdbcType.value();
				if ( jdbcTypeClass != null ) {
					return useDeferredBeanContainerAccess
							? FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( jdbcTypeClass )
							: getManagedBeanRegistry().getBean( jdbcTypeClass ).getBeanInstance();
				}
			}

			final var anyKeyJdbcTypeCode = member.locateAnnotationUsage( AnyKeyJdbcTypeCode.class, context );
			if ( anyKeyJdbcTypeCode != null ) {
				final int code = anyKeyJdbcTypeCode.value();
				if ( code != Integer.MIN_VALUE ) {
					return getDescriptor( typeConfiguration, code );
				}
			}

			return null;
		};
	}

	private void normalJdbcTypeDetails(MemberDetails attribute) {
		explicitJdbcTypeAccess = typeConfiguration -> {
			final var context = getSourceModelContext();

			final var jdbcType = attribute.locateAnnotationUsage( org.hibernate.annotations.JdbcType.class, context );
			if ( jdbcType != null ) {
				final var jdbcTypeClass = jdbcType.value();
				if ( jdbcTypeClass != null ) {
					return useDeferredBeanContainerAccess()
							? FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( jdbcTypeClass )
							: getManagedBeanRegistry().getBean( jdbcTypeClass ).getBeanInstance();
				}
			}

			final var jdbcTypeCode = attribute.locateAnnotationUsage( JdbcTypeCode.class, context );
			if ( jdbcTypeCode != null ) {
				final int code = jdbcTypeCode.value();
				if ( code != Integer.MIN_VALUE ) {
					final var jdbcTypeRegistry = typeConfiguration.getJdbcTypeRegistry();
					return jdbcTypeRegistry.getConstructor( code ) == null
							? jdbcTypeRegistry.getDescriptor( code )
							: null;
				}
			}

			return null;
		};
	}

	private void normalMutabilityDetails(MemberDetails attribute) {
		explicitMutabilityAccess = typeConfiguration -> {
			// Look for `@Mutability` on the attribute
			final var mutability =
					attribute.locateAnnotationUsage( Mutability.class, getSourceModelContext() );
			if ( mutability != null && mutability.value() != null ) {
				return resolveMutability( mutability.value() );
			}
			// Look for `@Immutable` on the attribute
			if ( attribute.hasDirectAnnotationUsage( Immutable.class ) ) {
				return ImmutableMutabilityPlan.instance();
			}

			// Look for `@Mutability` on the attribute's type
			if ( explicitJavaTypeAccess != null || implicitJavaTypeAccess != null ) {
				final var attributeType = attributeType( typeConfiguration );
				if ( attributeType != null ) {
					final var classMutability = attributeType.getAnnotation( Mutability.class );
					if ( classMutability != null ) {
						return resolveMutability( classMutability.value() );
					}
					if ( attributeType.isAnnotationPresent( Immutable.class ) ) {
						return ImmutableMutabilityPlan.instance();
					}
				}
			}

			// if the value is converted, see if the converter Class is annotated `@Mutability`
			if ( converterDescriptor != null ) {
				final var attributeConverterClass = converterDescriptor.getAttributeConverterClass();
				final var converterMutability = attributeConverterClass.getAnnotation( Mutability.class );
				if ( converterMutability != null ) {
					return resolveMutability( converterMutability.value() );
				}
				if ( attributeConverterClass.isAnnotationPresent( Immutable.class ) ) {
					return ImmutableMutabilityPlan.instance();
				}
			}

			// if a custom UserType is specified, see if the UserType Class is annotated `@Mutability`
			final var customTypeImpl =
					Kind.ATTRIBUTE.mappingAccess.customType( attribute, getSourceModelContext() );
			if ( customTypeImpl != null ) {
				final var customTypeMutability = customTypeImpl.getAnnotation( Mutability.class );
				if ( customTypeMutability != null ) {
					return resolveMutability( customTypeMutability.value() );
				}
				if ( customTypeImpl.isAnnotationPresent( Immutable.class ) ) {
					return ImmutableMutabilityPlan.instance();
				}
			}

			// generally, this will trigger usage of the `JavaType#getMutabilityPlan`
			return null;
		};
	}

	private Class<?> attributeType(TypeConfiguration typeConfiguration) {
		if ( explicitJavaTypeAccess != null ) {
			final var basicJavaType = explicitJavaTypeAccess.apply( typeConfiguration );
			if ( basicJavaType != null ) {
				final var attributeType = basicJavaType.getJavaTypeClass();
				if ( attributeType != null ) {
					return attributeType;
				}
			}
		}
		final var javaType = implicitJavaTypeAccess.apply( typeConfiguration );
		if ( javaType != null ) {
			return ReflectHelper.getClass( javaType );
		}
		return null;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <T> MutabilityPlan<T> resolveMutability(Class<? extends MutabilityPlan> mutability) {
		if ( mutability.equals( Immutability.class ) ) {
			return Immutability.instance();
		}
		else if ( mutability.equals( ImmutableMutabilityPlan.class ) ) {
			return ImmutableMutabilityPlan.instance();
		}
		else if ( useDeferredBeanContainerAccess() ) {
			return FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( mutability );
		}
		else {
			return getManagedBeanRegistry().getBean( mutability ).getBeanInstance();
		}
	}

	private void normalSupplementalDetails(MemberDetails attribute) {
		final var context = getSourceModelContext();

		explicitJavaTypeAccess = typeConfiguration -> {
			final var javaType =
					attribute.locateAnnotationUsage( org.hibernate.annotations.JavaType.class, context );
			if ( javaType != null ) {
				final var basicJavaTypeClass = javaType.value();
				if ( basicJavaTypeClass != null ) {
					return useDeferredBeanContainerAccess()
							? FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( basicJavaTypeClass )
							: getManagedBeanRegistry().getBean( basicJavaTypeClass ).getBeanInstance();
				}
			}

			return null;
		};

		final var jdbcType = attribute.locateAnnotationUsage( JdbcTypeCode.class, context );
		if ( jdbcType != null ) {
			jdbcTypeCode = jdbcType.value();
		}

		normalJdbcTypeDetails( attribute);
		normalMutabilityDetails( attribute );

		final var enumerated = attribute.getDirectAnnotationUsage( Enumerated.class );
		if ( enumerated != null ) {
			enumType = enumerated.value();
		}

		//noinspection deprecation
		final var temporal = attribute.getDirectAnnotationUsage( Temporal.class );
		if ( temporal != null ) {
			temporalPrecision = temporal.value();
		}

		final var timeZoneStorage = attribute.getDirectAnnotationUsage( TimeZoneStorage.class );
		if ( timeZoneStorage != null ) {
			timeZoneStorageType = timeZoneStorage.value();
			final var timeZoneColumnAnn = attribute.getDirectAnnotationUsage( TimeZoneColumn.class );
			if ( timeZoneColumnAnn != null ) {
				if ( timeZoneStorageType != TimeZoneStorageType.AUTO
						&& timeZoneStorageType != TimeZoneStorageType.COLUMN ) {
					throw new IllegalStateException(
							"'@TimeZoneColumn' can not be used in conjunction with '@TimeZoneStorage( "
							+ timeZoneStorageType + " )' for attribute '"
							+ attribute.getDeclaringType().getName() + '.' + attribute.getName() + "'"
					);
				}
			}
		}

		partitionKey = attribute.hasDirectAnnotationUsage( PartitionKey.class );
	}

	@Override
	public Dialect getDialect() {
		return getMetadataCollector().getDatabase().getDialect();
	}

	private void applyJpaConverter(MemberDetails attribute, ConverterDescriptor<?,?> attributeConverterDescriptor) {
		final boolean autoApply = attributeConverterDescriptor.getAutoApplyDescriptor().isAutoApplicable();
		disallowConverter( attribute, Id.class, autoApply );
		disallowConverter( attribute, Version.class, autoApply );
		if ( kind == Kind.MAP_KEY ) {
			//noinspection deprecation
			disallowConverter( attribute, MapKeyTemporal.class, autoApply );
			disallowConverter( attribute, MapKeyEnumerated.class, autoApply );
		}
		else {
			//noinspection deprecation
			disallowConverter( attribute, Temporal.class, autoApply );
			disallowConverter( attribute, Enumerated.class, autoApply );
			disallowConverter( attribute, ManyToOne.class, autoApply );
			disallowConverter( attribute, OneToOne.class, autoApply );
			disallowConverter( attribute, OneToMany.class, autoApply );
			disallowConverter( attribute, ManyToMany.class, autoApply );
			// Note that @Convert is only allowed in conjunction with
			// @Embedded if it specifies a field using attributeName
			disallowConverter( attribute, Embedded.class, autoApply );
			disallowConverter( attribute, EmbeddedId.class, autoApply );
		}
		// I assume that these do not work with converters (no tests)
		disallowConverter( attribute, Struct.class, autoApply );
		disallowConverter( attribute, Array.class, autoApply );
		disallowConverter( attribute, Any.class, autoApply );
		converterDescriptor = attributeConverterDescriptor;
	}

	void disallowConverter(MemberDetails attribute, Class<? extends Annotation> annotationType, boolean autoApply) {
		if ( attribute.hasDirectAnnotationUsage( annotationType ) ) {
			throw new AnnotationException( "'AttributeConverter' not allowed for attribute '" + attribute.getName()
											+ "' annotated '@" + annotationType.getName() + "'"
											+ ( autoApply ? " (use '@Convert(disableConversion=true)' to suppress this error)" : "" ) );
		}
	}

	public BasicValue make() {
		if ( basicValue != null ) {
			return basicValue;
		}

		columns.checkPropertyConsistency();

		if ( table == null ) {
			table = columns.getTable();
		}

		basicValue = new BasicValue( buildingContext, table );

		if ( columns.getPropertyHolder().isComponent() ) {
			final var propertyHolder = (ComponentPropertyHolder) columns.getPropertyHolder();
			basicValue.setAggregateColumn( propertyHolder.getAggregateColumn() );
		}

		if ( isNationalized() ) {
			basicValue.makeNationalized();
		}

		if ( isLob() ) {
			basicValue.makeLob();
		}

		if ( enumType != null ) {
			basicValue.setEnumerationStyle( enumType );
		}

		if ( timeZoneStorageType != null ) {
			basicValue.setTimeZoneStorageType( timeZoneStorageType );
		}

		basicValue.setPartitionKey( partitionKey );

		if ( temporalPrecision != null ) {
			basicValue.setTemporalPrecision( temporalPrecision );
		}

		if ( jdbcTypeCode != null ) {
			basicValue.setExplicitJdbcTypeCode( jdbcTypeCode );
		}

		linkWithValue();

		if ( !getMetadataCollector().isInSecondPass() ) {
			//Defer this to the second pass
			getMetadataCollector().addSecondPass( new SetBasicValueTypeSecondPass( this ) );
		}
		else {
			//We are already in second pass
			fillSimpleValue();
		}

		return basicValue;
	}

	private void linkWithValue() {
		final var collector = getMetadataCollector();
		final var firstColumn = columns.getColumns().get(0);
		if ( !collector.isInSecondPass() && firstColumn.isNameDeferred() && referencedEntityName != null ) {
			collector.addSecondPass( new OverriddenFkSecondPass( basicValue, referencedEntityName, columns ) );
		}
		else if ( aggregateComponent != null ) {
			assert columns.getColumns().size() == 1;
			firstColumn.linkWithAggregateValue( basicValue, aggregateComponent );
		}
		else {
			for ( AnnotatedColumn column : columns.getColumns() ) {
				column.linkWithValue( basicValue );
			}
		}
	}

	public void fillSimpleValue() {
		basicValue.setExplicitTypeParams( explicitLocalCustomTypeParams );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// todo (6.0) : we are dropping support for @Type and @TypeDef from annotations
		//		so this handling can go away.  most of this (enum, temporal, ect) will be
		//		handled by BasicValue already.  this stuff is all just to drive
		// 		DynamicParameterizedType handling - just pass them (or a Supplier?) into
		//		BasicValue so that it has access to them as needed

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		if ( explicitCustomType != null
				&& DynamicParameterizedType.class.isAssignableFrom( explicitCustomType ) ) {
			basicValue.setTypeParameters( createDynamicParameterizedTypeParameters() );
		}

		if ( explicitCustomType != null ) {
			basicValue.setTypeAnnotation( explicitCustomTypeAnnotation );
		}

		if ( converterDescriptor != null ) {
			basicValue.setJpaAttributeConverterDescriptor( converterDescriptor );
		}

		if ( implicitJavaTypeAccess != null ) {
			basicValue.setImplicitJavaTypeAccess( implicitJavaTypeAccess );
		}

		if ( explicitJavaTypeAccess != null ) {
			basicValue.setExplicitJavaTypeAccess( explicitJavaTypeAccess );
		}

		if ( explicitJdbcTypeAccess != null ) {
			basicValue.setExplicitJdbcTypeAccess( explicitJdbcTypeAccess );
		}

		if ( explicitMutabilityAccess != null ) {
			basicValue.setExplicitMutabilityPlanAccess( explicitMutabilityAccess );
		}

		if ( enumType != null ) {
			basicValue.setEnumerationStyle( enumType );
		}

		if ( timeZoneStorageType != null ) {
			basicValue.setTimeZoneStorageType( timeZoneStorageType );
		}

		if ( temporalPrecision != null ) {
			basicValue.setTemporalPrecision( temporalPrecision );
		}

		if ( isLob() ) {
			basicValue.makeLob();
		}

		if ( isNationalized() ) {
			basicValue.makeNationalized();
		}

		if ( explicitCustomType != null ) {
			basicValue.setExplicitCustomType( explicitCustomType );
		}
	}

	private Map<String, Object> createDynamicParameterizedTypeParameters() {
		final Map<String, Object> parameters = new HashMap<>();

		if ( returnedClassName == null ) {
			throw new MappingException( "Returned class name not specified for basic mapping: " + memberDetails.getName() );
		}

		parameters.put( DynamicParameterizedType.RETURNED_CLASS, returnedClassName );
		parameters.put( DynamicParameterizedType.XPROPERTY, memberDetails );
		parameters.put( DynamicParameterizedType.PROPERTY, memberDetails.getName() );

		parameters.put( DynamicParameterizedType.IS_DYNAMIC, Boolean.toString( true ) );
		parameters.put( DynamicParameterizedType.IS_PRIMARY_KEY, Boolean.toString( kind == Kind.MAP_KEY ) );

		if ( persistentClassName != null ) {
			parameters.put( DynamicParameterizedType.ENTITY, persistentClassName );
		}

		if ( returnedClassName != null ) {
			parameters.put( DynamicParameterizedType.RETURNED_CLASS, returnedClassName );
		}

		if ( accessType != null ) {
			parameters.put( DynamicParameterizedType.ACCESS_TYPE, accessType.getType() );
		}

		if ( explicitLocalCustomTypeParams != null ) {
			parameters.putAll( explicitLocalCustomTypeParams );
		}

		return parameters;
	}

	/**
	 * Access to detail of basic value mappings based on {@link Kind}
	 */
	private interface BasicMappingAccess {
		Class<? extends UserType<?>> customType(MemberDetails attribute, ModelsContext context);
		Map<String,String> customTypeParameters(MemberDetails attribute, ModelsContext context);
		Annotation customTypeAnnotation(MemberDetails attribute, ModelsContext context);
	}

	private static class ValueMappingAccess implements BasicMappingAccess {
		private static final ValueMappingAccess INSTANCE = new ValueMappingAccess();

		@Override
		public Class<? extends UserType<?>> customType(MemberDetails attribute, ModelsContext context) {
			final var customType = attribute.locateAnnotationUsage( Type.class, context );
			return customType == null ? null : customType.value();
		}

		@Override
		public Map<String,String> customTypeParameters(MemberDetails attribute, ModelsContext context) {
			final var customType = attribute.locateAnnotationUsage( Type.class, context );
			return customType == null ? null : extractParameterMap( customType.parameters() );
		}

		@Override
		public Annotation customTypeAnnotation(MemberDetails attribute, ModelsContext context) {
			final var annotations = attribute.getMetaAnnotated( Type.class, context );
			return annotations == null || annotations.isEmpty() ? null : annotations.get( 0 );
		}
	}

	private static class AnyDiscriminatorMappingAccess implements BasicMappingAccess {
		private static final AnyDiscriminatorMappingAccess INSTANCE = new AnyDiscriminatorMappingAccess();

		@Override
		public Class<? extends UserType<?>> customType(MemberDetails attribute, ModelsContext context) {
			return null;
		}

		@Override
		public Map<String,String> customTypeParameters(MemberDetails attribute, ModelsContext context) {
			return emptyMap();
		}

		@Override
		public Annotation customTypeAnnotation(MemberDetails attribute, ModelsContext context) {
			return null;
		}
	}

	private static class AnyKeyMappingAccess implements BasicMappingAccess {
		private static final AnyKeyMappingAccess INSTANCE = new AnyKeyMappingAccess();

		@Override
		public Class<? extends UserType<?>> customType(MemberDetails attribute, ModelsContext context) {
			return null;
		}

		@Override
		public Map<String,String> customTypeParameters(MemberDetails attribute, ModelsContext context) {
			return emptyMap();
		}

		@Override
		public Annotation customTypeAnnotation(MemberDetails attribute, ModelsContext context) {
			return null;
		}
	}

	private static class MapKeyMappingAccess implements BasicMappingAccess {
		private static final MapKeyMappingAccess INSTANCE = new MapKeyMappingAccess();

		@Override
		public Class<? extends UserType<?>> customType(MemberDetails attribute, ModelsContext context) {
			final var customType = attribute.locateAnnotationUsage( MapKeyType.class, context );
			return customType == null ? null : customType.value();

		}

		@Override
		public Map<String,String> customTypeParameters(MemberDetails attribute, ModelsContext context) {
			final var customType = attribute.locateAnnotationUsage( MapKeyType.class, context );
			return customType == null ? null : extractParameterMap( customType.parameters() );

		}

		@Override
		public Annotation customTypeAnnotation(MemberDetails attribute, ModelsContext context) {
			final var annotations = attribute.getMetaAnnotated( MapKeyType.class, context );
			return annotations == null || annotations.isEmpty() ? null : annotations.get( 0 );
		}
	}

	private static class CollectionIdMappingAccess implements BasicMappingAccess {
		private static final CollectionIdMappingAccess INSTANCE = new CollectionIdMappingAccess();

		@Override
		public Class<? extends UserType<?>> customType(MemberDetails attribute, ModelsContext context) {
			final var customType = attribute.locateAnnotationUsage( CollectionIdType.class, context );
			return customType == null ? null : customType.value();

		}

		@Override
		public Map<String,String> customTypeParameters(MemberDetails attribute, ModelsContext context) {
			final var customType = attribute.locateAnnotationUsage( CollectionIdType.class, context );
			return customType == null ? null : extractParameterMap( customType.parameters() );
		}

		@Override
		public Annotation customTypeAnnotation(MemberDetails attribute, ModelsContext context) {
			final var annotations = attribute.getMetaAnnotated( CollectionIdType.class, context );
			return annotations == null || annotations.isEmpty() ? null : annotations.get( 0 );
		}
	}

	private static class ListIndexMappingAccess implements BasicMappingAccess {
		private static final ListIndexMappingAccess INSTANCE = new ListIndexMappingAccess();

		@Override
		public Class<? extends UserType<?>> customType(MemberDetails attribute, ModelsContext context) {
			return null;
		}

		@Override
		public Map<String,String> customTypeParameters(MemberDetails attribute, ModelsContext context) {
			return emptyMap();
		}

		@Override
		public Annotation customTypeAnnotation(MemberDetails attribute, ModelsContext context) {
			return null;
		}
	}

	private static AnnotatedJoinColumns convertToJoinColumns(AnnotatedColumns columns, MetadataBuildingContext context) {
		final var joinColumns = new AnnotatedJoinColumns();
		joinColumns.setBuildingContext( context );
		joinColumns.setPropertyHolder( columns.getPropertyHolder() );
		joinColumns.setPropertyName( columns.getPropertyName() );
		//TODO: resetting the parent here looks like a dangerous thing to do
		//      should we be cloning them first (the legacy code did not)
		for ( AnnotatedColumn column : columns.getColumns() ) {
			column.setParent( joinColumns );
		}
		return joinColumns;
	}

	// used for resolving FK with @MapsId and @IdClass
	private static class OverriddenFkSecondPass implements FkSecondPass {
		private final AnnotatedJoinColumns joinColumns;
		private final BasicValue value;
		private final String referencedEntityName;

		public OverriddenFkSecondPass(
				BasicValue value,
				String referencedEntityName,
				AnnotatedColumns columns) {
			this.value = value;
			this.referencedEntityName = referencedEntityName;
			this.joinColumns = convertToJoinColumns( columns, value.getBuildingContext() );
		}

		@Override
		public BasicValue getValue() {
			return value;
		}

		@Override
		public String getReferencedEntityName() {
			return referencedEntityName;
		}

		@Override
		public boolean isInPrimaryKey() {
			// @MapsId is not itself in the primary key,
			// so it's safe to simply process it after all the primary keys have been processed.
			return true;
		}

		@Override
		public void doSecondPass(Map<String, PersistentClass> persistentClasses) {
			final var referencedEntity = persistentClasses.get( referencedEntityName );
			if ( referencedEntity == null ) {
				// TODO: much better error message if this is something that can really happen!
				throw new AnnotationException( "Unknown entity name '" + referencedEntityName + "'" );
			}
			linkJoinColumnWithValueOverridingNameIfImplicit(
					referencedEntity,
					referencedEntity.getKey(),
					joinColumns,
					value
			);
		}
	}
}
