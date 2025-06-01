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
import org.hibernate.AssertionFailure;
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
import org.hibernate.dialect.aggregate.AggregateSupport;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.models.spi.TypeDetails;
import org.hibernate.resource.beans.internal.FallbackBeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.java.Immutability;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.internal.ParameterizedTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.DynamicParameterizedType;
import org.hibernate.usertype.UserType;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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
	private Map<String,String> explicitLocalTypeParams;

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
			final JdbcType type = explicitJdbcTypeAccess.apply( getTypeConfiguration() );
			if ( type != null ) {
				return type.isLob();
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
				: getAggregateSupport()
						.aggregateComponentSqlTypeCode( aggregateComponent.getAggregateColumn().getSqlTypeCode(),
								jdbcTypeCode );
	}

	private AggregateSupport getAggregateSupport() {
		return getMetadataCollector().getDatabase().getDialect().getAggregateSupport();
	}

	@Override
	public boolean isNationalized() {
		if ( isNationalized ) {
			return true;
		}
		else if ( explicitJdbcTypeAccess != null ) {
			final JdbcType type = explicitJdbcTypeAccess.apply( getTypeConfiguration() );
			return type != null && type.isNationalized();
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

	public void setType(
			MemberDetails value,
			TypeDetails typeDetails,
			String declaringClassName,
			@Nullable ConverterDescriptor converterDescriptor) {
		this.memberDetails = value;
		final boolean isArray = value.isArray();
		if ( typeDetails == null && !isArray ) {
			// we cannot guess anything
			return;
		}

		if ( columns == null ) {
			throw new AssertionFailure( "`BasicValueBinder#setColumns` should be called before `BasicValueBinder#setType`" );
		}

//		if ( columns.length != 1 ) {
//			throw new AssertionFailure( "Expecting just one column, but found `" + Arrays.toString( columns ) + "`" );
//		}

		final TypeDetails modelClassDetails = isArray ? value.getElementType() : typeDetails;

		if ( kind != Kind.LIST_INDEX && kind != Kind.MAP_KEY  ) {
			isLob = value.hasDirectAnnotationUsage( Lob.class );
		}

		if ( getDialect().getNationalizationSupport() == NationalizationSupport.EXPLICIT ) {
			isNationalized = buildingContext.getBuildingOptions().useNationalizedCharacterData()
					|| value.locateAnnotationUsage( Nationalized.class, getSourceModelContext() ) != null;
		}

		if ( converterDescriptor != null ) {
			applyJpaConverter( value, converterDescriptor );
		}

		final Class<? extends UserType<?>> userTypeImpl =
				kind.mappingAccess.customType( value, getSourceModelContext() );
		if ( userTypeImpl != null ) {
			applyExplicitType( userTypeImpl,
					kind.mappingAccess.customTypeParameters( value, getSourceModelContext() ) );
			// An explicit custom UserType has top precedence when we get to BasicValue resolution.
			return;
		}
		else if ( modelClassDetails != null ) {
			final ClassDetails rawClassDetails = modelClassDetails.determineRawClass();
			final Class<?> basicClass = rawClassDetails.toJavaClass();
			final Class<? extends UserType<?>> registeredUserTypeImpl =
					getMetadataCollector().findRegisteredUserType( basicClass );
			if ( registeredUserTypeImpl != null ) {
				applyExplicitType( registeredUserTypeImpl, emptyMap() );
				return;
			}
		}

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

	private void applyExplicitType(Class<? extends UserType<?>> impl, Map<String,String> params) {
		this.explicitCustomType = impl;
		this.explicitLocalTypeParams = params;
	}

	private void prepareCollectionId(MemberDetails attribute) {
		final CollectionId collectionIdAnn = attribute.getDirectAnnotationUsage( CollectionId.class );
		if ( collectionIdAnn == null ) {
			throw new MappingException( "idbag mapping missing @CollectionId" );
		}

		final boolean useDeferredBeanContainerAccess = useDeferredBeanContainerAccess();
		final ManagedBeanRegistry beanRegistry = getManagedBeanRegistry();

		implicitJavaTypeAccess = typeConfiguration -> null;

		explicitJavaTypeAccess = typeConfiguration -> {
			final CollectionIdJavaClass javaClassAnn = attribute.locateAnnotationUsage(
					CollectionIdJavaClass.class,
					getSourceModelContext()
			);
			if ( javaClassAnn != null ) {
				return (BasicJavaType<?>) buildingContext
						.getBootstrapContext()
						.getTypeConfiguration()
						.getJavaTypeRegistry()
						.getDescriptor( javaClassAnn.idType() );
			}
			final CollectionIdJavaType javaTypeAnn = attribute.locateAnnotationUsage(
					CollectionIdJavaType.class,
					getSourceModelContext()
			);
			if ( javaTypeAnn != null ) {
				final Class<? extends BasicJavaType<?>> javaTypeClass = javaTypeAnn.value();
				if ( javaTypeClass != null ) {
					if ( useDeferredBeanContainerAccess ) {
						return FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( javaTypeClass );
					}
					else {
						return beanRegistry.getBean( javaTypeClass ).getBeanInstance();
					}
				}
			}

			return null;
		};

		explicitJdbcTypeAccess = typeConfiguration -> {
			final CollectionIdJdbcType jdbcTypeAnn =
					attribute.locateAnnotationUsage( CollectionIdJdbcType.class, getSourceModelContext() );
			if ( jdbcTypeAnn != null ) {
				final Class<? extends JdbcType> jdbcTypeClass = jdbcTypeAnn.value();
				if ( jdbcTypeClass != null ) {
					if ( useDeferredBeanContainerAccess ) {
						return FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( jdbcTypeClass );
					}
					else {
						return beanRegistry.getBean( jdbcTypeClass ).getBeanInstance();
					}
				}
			}

			final CollectionIdJdbcTypeCode jdbcTypeCodeAnn =
					attribute.locateAnnotationUsage( CollectionIdJdbcTypeCode.class, getSourceModelContext() );
			if ( jdbcTypeCodeAnn != null ) {
				final int code = jdbcTypeCodeAnn.value();
				if ( code != Integer.MIN_VALUE ) {
					return getDescriptor( typeConfiguration, code );
				}
			}

			return null;
		};

		explicitMutabilityAccess = typeConfiguration -> {
			final CollectionIdMutability mutabilityAnn =
					attribute.locateAnnotationUsage( CollectionIdMutability.class, getSourceModelContext() );
			if ( mutabilityAnn != null ) {
				final Class<? extends MutabilityPlan<?>> mutabilityClass = mutabilityAnn.value();
				if ( mutabilityClass != null ) {
					return resolveMutability( mutabilityClass );
				}
			}

			// see if the value's type Class is annotated with mutability-related annotations
			if ( implicitJavaTypeAccess != null ) {
				final Class<?> attributeType =
						ReflectHelper.getClass( implicitJavaTypeAccess.apply( typeConfiguration ) );
				if ( attributeType != null ) {
					final Mutability attributeTypeMutabilityAnn = attributeType.getAnnotation( Mutability.class );
					if ( attributeTypeMutabilityAnn != null ) {
						return resolveMutability( attributeTypeMutabilityAnn.value() );
					}

					if ( attributeType.isAnnotationPresent( Immutable.class ) ) {
						return ImmutableMutabilityPlan.instance();
					}
				}
			}

			// if there is a converter, check it for mutability-related annotations
			if ( converterDescriptor != null ) {
				final Mutability converterMutabilityAnn =
						converterDescriptor.getAttributeConverterClass().getAnnotation( Mutability.class );
				if ( converterMutabilityAnn != null ) {
					return resolveMutability( converterMutabilityAnn.value() );
				}

				if ( converterDescriptor.getAttributeConverterClass().isAnnotationPresent( Immutable.class ) ) {
					return ImmutableMutabilityPlan.instance();
				}
			}

			// if there is a UserType, see if its Class is annotated with mutability-related annotations
			final Class<? extends UserType<?>> customTypeImpl =
					Kind.ATTRIBUTE.mappingAccess.customType( attribute, getSourceModelContext() );
			if ( customTypeImpl != null ) {
				final Mutability customTypeMutabilityAnn = customTypeImpl.getAnnotation( Mutability.class );
				if ( customTypeMutabilityAnn != null ) {
					return resolveMutability( customTypeMutabilityAnn.value() );
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
		final TypeDetails mapKeyClass =
				explicitMapKeyTypeDetails == null
						? attribute.getMapKeyType()
						: explicitMapKeyTypeDetails;
		implicitJavaTypeAccess = typeConfiguration -> {
			final ClassDetails rawKeyClassDetails = mapKeyClass.determineRawClass();
			return rawKeyClassDetails.toJavaClass();
		};

		final MapKeyEnumerated mapKeyEnumeratedAnn =
				attribute.getDirectAnnotationUsage( MapKeyEnumerated.class );
		if ( mapKeyEnumeratedAnn != null ) {
			enumType = mapKeyEnumeratedAnn.value();
		}

		//noinspection deprecation
		final MapKeyTemporal mapKeyTemporalAnn =
				attribute.getDirectAnnotationUsage( MapKeyTemporal.class );
		if ( mapKeyTemporalAnn != null ) {
			temporalPrecision = mapKeyTemporalAnn.value();
		}

		final boolean useDeferredBeanContainerAccess = useDeferredBeanContainerAccess();

		explicitJdbcTypeAccess = typeConfiguration -> {
			final MapKeyJdbcType jdbcTypeAnn =
					attribute.locateAnnotationUsage( MapKeyJdbcType.class, getSourceModelContext() );
			if ( jdbcTypeAnn != null ) {
				final Class<? extends JdbcType> jdbcTypeClass = jdbcTypeAnn.value();
				if ( jdbcTypeClass != null ) {
					if ( useDeferredBeanContainerAccess ) {
						return FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( jdbcTypeClass );
					}
					else {
						return getManagedBeanRegistry().getBean( jdbcTypeClass ).getBeanInstance();
					}
				}
			}

			final MapKeyJdbcTypeCode jdbcTypeCodeAnn =
					attribute.locateAnnotationUsage( MapKeyJdbcTypeCode.class, getSourceModelContext() );
			if ( jdbcTypeCodeAnn != null ) {
				final int jdbcTypeCode = jdbcTypeCodeAnn.value();
				if ( jdbcTypeCode != Integer.MIN_VALUE ) {
					return getDescriptor( typeConfiguration, jdbcTypeCode );
				}
			}

			return null;
		};

		explicitJavaTypeAccess = typeConfiguration -> {
			final MapKeyJavaType javaTypeAnn =
					attribute.locateAnnotationUsage( MapKeyJavaType.class, getSourceModelContext() );
			if ( javaTypeAnn != null ) {
				final Class<? extends BasicJavaType<?>> javaTypeClass = javaTypeAnn.value();
				if ( javaTypeClass != null ) {
					if ( useDeferredBeanContainerAccess ) {
						return FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( javaTypeClass );
					}
					else {
						return getManagedBeanRegistry().getBean( javaTypeClass ).getBeanInstance();
					}
				}
			}

			final MapKeyClass mapKeyClassAnn = attribute.getDirectAnnotationUsage( MapKeyClass.class );
			if ( mapKeyClassAnn != null ) {
				return (BasicJavaType<?>)
						typeConfiguration.getJavaTypeRegistry()
								.getDescriptor( mapKeyClassAnn.value() );
			}
			else {
				return null;
			}
		};

		explicitMutabilityAccess = typeConfiguration -> {
			final MapKeyMutability mutabilityAnn =
					attribute.locateAnnotationUsage( MapKeyMutability.class, getSourceModelContext() );
			if ( mutabilityAnn != null ) {
				final Class<? extends MutabilityPlan<?>> mutabilityClass = mutabilityAnn.value();
				if ( mutabilityClass != null ) {
					return resolveMutability( mutabilityClass );
				}
			}

			// see if the value's Java Class is annotated with mutability-related annotations
			if ( implicitJavaTypeAccess != null ) {
				final Class<?> attributeType =
						ReflectHelper.getClass( implicitJavaTypeAccess.apply( typeConfiguration ) );
				if ( attributeType != null ) {
					final Mutability attributeTypeMutabilityAnn = attributeType.getAnnotation( Mutability.class );
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
				final Mutability converterMutabilityAnn =
						converterDescriptor.getAttributeConverterClass().getAnnotation( Mutability.class );
				if ( converterMutabilityAnn != null ) {
					return resolveMutability( converterMutabilityAnn.value() );
				}

				if ( converterDescriptor.getAttributeConverterClass().isAnnotationPresent( Immutable.class ) ) {
					return ImmutableMutabilityPlan.instance();
				}
			}

			// if there is a UserType, see if its Class is annotated with mutability-related annotations
			final Class<? extends UserType<?>> customTypeImpl =
					Kind.MAP_KEY.mappingAccess.customType( attribute, getSourceModelContext() );
			if ( customTypeImpl != null ) {
				final Mutability customTypeMutabilityAnn = customTypeImpl.getAnnotation( Mutability.class );
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
		final ManagedBeanRegistry beanRegistry = getManagedBeanRegistry();

		explicitJavaTypeAccess = typeConfiguration -> {
			final ListIndexJavaType javaTypeAnn =
					attribute.locateAnnotationUsage( ListIndexJavaType.class, getSourceModelContext() );
			if ( javaTypeAnn != null ) {
				final Class<? extends BasicJavaType<?>> javaTypeClass = javaTypeAnn.value();
				if ( javaTypeClass != null ) {
					if ( useDeferredBeanContainerAccess ) {
						return FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( javaTypeClass );
					}
					else {
						return beanRegistry.getBean( javaTypeClass ).getBeanInstance();
					}
				}
			}

			return null;
		};

		explicitJdbcTypeAccess = typeConfiguration -> {
			final ListIndexJdbcType jdbcTypeAnn =
					attribute.locateAnnotationUsage( ListIndexJdbcType.class, getSourceModelContext() );
			if ( jdbcTypeAnn != null ) {
				final Class<? extends JdbcType> jdbcTypeClass = jdbcTypeAnn.value();
				if ( jdbcTypeClass != null ) {
					if ( useDeferredBeanContainerAccess ) {
						return FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( jdbcTypeClass );
					}
					else {
						return beanRegistry.getBean( jdbcTypeClass ).getBeanInstance();
					}
				}
			}

			final ListIndexJdbcTypeCode jdbcTypeCodeAnn =
					attribute.locateAnnotationUsage( ListIndexJdbcTypeCode.class, getSourceModelContext() );
			if ( jdbcTypeCodeAnn != null ) {
				return getDescriptor( typeConfiguration, jdbcTypeCodeAnn.value() );
			}
			else {
				return null;
			}
		};
	}

	private void prepareCollectionElement(
			MemberDetails attribute,
			TypeDetails explicitElementTypeDetails) {
		final TypeDetails elementTypeDetails =
				explicitElementTypeDetails == null && attribute.isArray()
						? attribute.getElementType()
						: explicitElementTypeDetails;
		final ClassDetails rawElementType = elementTypeDetails.determineRawClass();
		final java.lang.reflect.Type javaType = rawElementType.toJavaClass();
		final Class<?> javaTypeClass = ReflectHelper.getClass( javaType );

		implicitJavaTypeAccess = typeConfiguration -> javaType;

		//noinspection deprecation
		final Temporal temporalAnn = attribute.getDirectAnnotationUsage( Temporal.class );
		if ( temporalAnn != null ) {
			//noinspection deprecation
			DEPRECATION_LOGGER.deprecatedAnnotation( Temporal.class, attribute.getName() );
			temporalPrecision = temporalAnn.value();
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

		if ( javaTypeClass.isEnum() ) {
			final Enumerated enumeratedAnn = attribute.getDirectAnnotationUsage( Enumerated.class );
			if ( enumeratedAnn != null ) {
				enumType = enumeratedAnn.value();
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

		// layer in support for JPA's approach for specifying a specific Java type for the collection elements...
		final ElementCollection elementCollectionAnn =
				attribute.getDirectAnnotationUsage( ElementCollection.class );
		if ( elementCollectionAnn != null ) {
			final Class<?> targetClassDetails = elementCollectionAnn.targetClass();
			if ( targetClassDetails != void.class) {
				final Function<TypeConfiguration, BasicJavaType<?>> original = explicitJavaTypeAccess;
				explicitJavaTypeAccess = typeConfiguration -> {
					final BasicJavaType<?> originalResult = original.apply( typeConfiguration );
					if ( originalResult != null ) {
						return originalResult;
					}
					else {
						return (BasicJavaType<?>)
								typeConfiguration.getJavaTypeRegistry()
										.getDescriptor( targetClassDetails );
					}
				};
			}
		}
	}

	private void prepareBasicAttribute(
			String declaringClassName,
			MemberDetails attribute,
			TypeDetails attributeType) {
		final Class<?> javaTypeClass = attributeType.determineRawClass().toJavaClass();
		implicitJavaTypeAccess = typeConfiguration -> {
			if ( attributeType.getTypeKind() == TypeDetails.Kind.PARAMETERIZED_TYPE ) {
				return ParameterizedTypeImpl.from( attributeType.asParameterizedType() );
			}
			else {
				return attributeType.determineRawClass().toJavaClass();
			}
		};

		//noinspection deprecation
		final Temporal temporalAnn = attribute.getDirectAnnotationUsage( Temporal.class );
		if ( temporalAnn != null ) {
			//noinspection deprecation
			DEPRECATION_LOGGER.deprecatedAnnotation( Temporal.class,
					declaringClassName + "." + attribute.getName() );
			temporalPrecision = temporalAnn.value();
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

		final Enumerated enumeratedAnn = attribute.getDirectAnnotationUsage( Enumerated.class );
		if ( enumeratedAnn != null ) {
			enumType = enumeratedAnn.value();
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
			final List<TypeDetails> typeArguments = javaType.asParameterizedType().getArguments();
			return !typeArguments.isEmpty() && typeArguments.get( 0 ).isImplementor( Enum.class );
		}
		else {
			return false;
		}
	}

	private void prepareAnyDiscriminator(MemberDetails memberDetails) {
		final AnyDiscriminator anyDiscriminatorAnn =
				memberDetails.locateAnnotationUsage( AnyDiscriminator.class, getSourceModelContext() );
		implicitJavaTypeAccess = typeConfiguration -> {
			if ( anyDiscriminatorAnn != null ) {
				return switch ( anyDiscriminatorAnn.value() ) {
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
		final Function<TypeConfiguration, JdbcType> originalJdbcTypeResolution = explicitJdbcTypeAccess;
		this.explicitJdbcTypeAccess = typeConfiguration -> {
			final JdbcType originalResolution = originalJdbcTypeResolution.apply( typeConfiguration );
			if ( originalResolution != null ) {
				return originalResolution;
			}
			else {
				final Class<?> hintedJavaType = (Class<?>) implicitJavaTypeAccess.apply( typeConfiguration );
				final JavaType<Object> hintedDescriptor =
						typeConfiguration.getJavaTypeRegistry()
								.getDescriptor( hintedJavaType );
				return hintedDescriptor.getRecommendedJdbcType( typeConfiguration.getCurrentBaseSqlTypeIndicators() );
			}
		};
	}

	private void prepareAnyKey(MemberDetails member) {
		implicitJavaTypeAccess = typeConfiguration -> null;

		final boolean useDeferredBeanContainerAccess = useDeferredBeanContainerAccess();

		explicitJavaTypeAccess = typeConfiguration -> {
			final AnyKeyJavaType javaTypeAnn =
					member.locateAnnotationUsage( AnyKeyJavaType.class, getSourceModelContext() );
			if ( javaTypeAnn != null ) {
				final Class<? extends BasicJavaType<?>> implClass = javaTypeAnn.value();
				if ( implClass != null ) {
					if ( useDeferredBeanContainerAccess ) {
						return FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( implClass );
					}
					else {
						return getManagedBeanRegistry().getBean( implClass ).getBeanInstance();
					}
				}
			}

			final AnyKeyJavaClass javaClassAnn =
					member.locateAnnotationUsage( AnyKeyJavaClass.class, getSourceModelContext() );
			if ( javaClassAnn != null ) {
				return (BasicJavaType<?>)
						typeConfiguration.getJavaTypeRegistry()
								.getDescriptor( javaClassAnn.value() );
			}

			// mainly used in XML interpretation
			final AnyKeyType anyKeyTypeAnn =
					member.locateAnnotationUsage( AnyKeyType.class, getSourceModelContext() );
			if ( anyKeyTypeAnn != null ) {
				final String namedType = anyKeyTypeAnn.value();
				final BasicType<Object> registeredType =
						typeConfiguration.getBasicTypeRegistry().getRegisteredType( namedType );
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
			final AnyKeyJdbcType jdbcTypeAnn =
					member.locateAnnotationUsage( AnyKeyJdbcType.class, getSourceModelContext() );
			if ( jdbcTypeAnn != null ) {
				final Class<? extends JdbcType> jdbcTypeClass = jdbcTypeAnn.value();
				if ( jdbcTypeClass != null ) {
					if ( useDeferredBeanContainerAccess ) {
						return FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( jdbcTypeClass );
					}
					else {
						return getManagedBeanRegistry().getBean( jdbcTypeClass ).getBeanInstance();
					}
				}
			}

			final AnyKeyJdbcTypeCode jdbcTypeCodeAnn =
					member.locateAnnotationUsage( AnyKeyJdbcTypeCode.class, getSourceModelContext() );
			if ( jdbcTypeCodeAnn != null ) {
				final int code = jdbcTypeCodeAnn.value();
				if ( code != Integer.MIN_VALUE ) {
					return getDescriptor( typeConfiguration, code );
				}
			}

			return null;
		};
	}

	private void normalJdbcTypeDetails(MemberDetails attribute) {
		explicitJdbcTypeAccess = typeConfiguration -> {
			final org.hibernate.annotations.JdbcType jdbcTypeAnn =
					attribute.locateAnnotationUsage( org.hibernate.annotations.JdbcType.class, getSourceModelContext() );
			if ( jdbcTypeAnn != null ) {
				final Class<? extends JdbcType> jdbcTypeClass = jdbcTypeAnn.value();
				if ( jdbcTypeClass != null ) {
					if ( useDeferredBeanContainerAccess() ) {
						return FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( jdbcTypeClass );
					}
					else {
						return getManagedBeanRegistry().getBean( jdbcTypeClass ).getBeanInstance();
					}
				}
			}

			final JdbcTypeCode jdbcTypeCodeAnn =
					attribute.locateAnnotationUsage( JdbcTypeCode.class, getSourceModelContext() );
			if ( jdbcTypeCodeAnn != null ) {
				final int jdbcTypeCode = jdbcTypeCodeAnn.value();
				if ( jdbcTypeCode != Integer.MIN_VALUE ) {
					final JdbcTypeRegistry jdbcTypeRegistry = typeConfiguration.getJdbcTypeRegistry();
					if ( jdbcTypeRegistry.getConstructor( jdbcTypeCode ) != null ) {
						return null;
					}
					else {
						return jdbcTypeRegistry.getDescriptor( jdbcTypeCode );
					}
				}
			}

			return null;
		};
	}

	private void normalMutabilityDetails(MemberDetails attribute) {
		explicitMutabilityAccess = typeConfiguration -> {
			// Look for `@Mutability` on the attribute
			final Mutability mutabilityAnn =
					attribute.locateAnnotationUsage( Mutability.class, getSourceModelContext() );
			if ( mutabilityAnn != null ) {
				final Class<? extends MutabilityPlan<?>> mutability = mutabilityAnn.value();
				if ( mutability != null ) {
					return resolveMutability( mutability );
				}
			}

			// Look for `@Immutable` on the attribute
			if ( attribute.hasDirectAnnotationUsage( Immutable.class ) ) {
				return ImmutableMutabilityPlan.instance();
			}

			// Look for `@Mutability` on the attribute's type
			if ( explicitJavaTypeAccess != null || implicitJavaTypeAccess != null ) {
				Class<?> attributeType = null;
				if ( explicitJavaTypeAccess != null ) {
					final BasicJavaType<?> jtd = explicitJavaTypeAccess.apply( typeConfiguration );
					if ( jtd != null ) {
						attributeType = jtd.getJavaTypeClass();
					}
				}
				if ( attributeType == null ) {
					final java.lang.reflect.Type javaType = implicitJavaTypeAccess.apply( typeConfiguration );
					if ( javaType != null ) {
						attributeType = ReflectHelper.getClass( javaType );
					}
				}

				if ( attributeType != null ) {
					final Mutability classMutability = attributeType.getAnnotation( Mutability.class );
					if ( classMutability != null ) {
						final Class<? extends MutabilityPlan<?>> mutability = classMutability.value();
						if ( mutability != null ) {
							return resolveMutability( mutability );
						}
					}

					final Immutable classImmutable = attributeType.getAnnotation( Immutable.class );
					if ( classImmutable != null ) {
						return ImmutableMutabilityPlan.instance();
					}
				}
			}

			// if the value is converted, see if the converter Class is annotated `@Mutability`
			if ( converterDescriptor != null ) {
				final Mutability converterMutabilityAnn =
						converterDescriptor.getAttributeConverterClass().getAnnotation( Mutability.class );
				if ( converterMutabilityAnn != null ) {
					return resolveMutability( converterMutabilityAnn.value() );
				}

				final Immutable converterImmutableAnn =
						converterDescriptor.getAttributeConverterClass().getAnnotation( Immutable.class );
				if ( converterImmutableAnn != null ) {
					return ImmutableMutabilityPlan.instance();
				}
			}

			// if a custom UserType is specified, see if the UserType Class is annotated `@Mutability`
			final Class<? extends UserType<?>> customTypeImpl =
					Kind.ATTRIBUTE.mappingAccess.customType( attribute, getSourceModelContext() );
			if ( customTypeImpl != null ) {
				final Mutability customTypeMutabilityAnn = customTypeImpl.getAnnotation( Mutability.class );
				if ( customTypeMutabilityAnn != null ) {
					return resolveMutability( customTypeMutabilityAnn.value() );
				}

				final Immutable customTypeImmutableAnn = customTypeImpl.getAnnotation( Immutable.class );
				if ( customTypeImmutableAnn != null ) {
					return ImmutableMutabilityPlan.instance();
				}
			}

			// generally, this will trigger usage of the `JavaType#getMutabilityPlan`
			return null;
		};
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
		explicitJavaTypeAccess = typeConfiguration -> {
			final org.hibernate.annotations.JavaType javaType =
					attribute.locateAnnotationUsage( org.hibernate.annotations.JavaType.class, getSourceModelContext() );
			if ( javaType != null ) {
				final Class<? extends BasicJavaType<?>> javaTypeClass = javaType.value();
				if ( javaTypeClass != null ) {
					if ( useDeferredBeanContainerAccess() ) {
						return FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( javaTypeClass );
					}
					else {
						return getManagedBeanRegistry().getBean( javaTypeClass ).getBeanInstance();
					}
				}
			}

			return null;
		};

		final JdbcTypeCode jdbcType =
				attribute.locateAnnotationUsage( JdbcTypeCode.class, getSourceModelContext() );
		if ( jdbcType != null ) {
			jdbcTypeCode = jdbcType.value();
		}

		normalJdbcTypeDetails( attribute);
		normalMutabilityDetails( attribute );

		final Enumerated enumerated = attribute.getDirectAnnotationUsage( Enumerated.class );
		if ( enumerated != null ) {
			enumType = enumerated.value();
		}

		//noinspection deprecation
		final Temporal temporal = attribute.getDirectAnnotationUsage( Temporal.class );
		if ( temporal != null ) {
			temporalPrecision = temporal.value();
		}

		final TimeZoneStorage timeZoneStorage =
				attribute.getDirectAnnotationUsage( TimeZoneStorage.class );
		if ( timeZoneStorage != null ) {
			timeZoneStorageType = timeZoneStorage.value();
			final TimeZoneColumn timeZoneColumnAnn =
					attribute.getDirectAnnotationUsage( TimeZoneColumn.class );
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

		this.partitionKey = attribute.hasDirectAnnotationUsage( PartitionKey.class );
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
		this.converterDescriptor = attributeConverterDescriptor;
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
			final ComponentPropertyHolder propertyHolder =
					(ComponentPropertyHolder) columns.getPropertyHolder();
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
		final InFlightMetadataCollector collector = getMetadataCollector();
		final AnnotatedColumn firstColumn = columns.getColumns().get(0);
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
		basicValue.setExplicitTypeParams( explicitLocalTypeParams );

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

		if ( explicitLocalTypeParams != null ) {
			parameters.putAll( explicitLocalTypeParams );
		}

		return parameters;
	}

	/**
	 * Access to detail of basic value mappings based on {@link Kind}
	 */
	private interface BasicMappingAccess {
		Class<? extends UserType<?>> customType(MemberDetails attribute, ModelsContext context);
		Map<String,String> customTypeParameters(MemberDetails attribute, ModelsContext context);
	}

	private static class ValueMappingAccess implements BasicMappingAccess {
		private static final ValueMappingAccess INSTANCE = new ValueMappingAccess();

		@Override
		public Class<? extends UserType<?>> customType(MemberDetails attribute, ModelsContext context) {
			final Type customType = attribute.locateAnnotationUsage( Type.class, context );
			return customType == null ? null : customType.value();
		}

		@Override
		public Map<String,String> customTypeParameters(MemberDetails attribute, ModelsContext context) {
			final Type customType = attribute.locateAnnotationUsage( Type.class, context );
			return customType == null ? null : extractParameterMap( customType.parameters() );
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
	}

	private static class MapKeyMappingAccess implements BasicMappingAccess {
		private static final MapKeyMappingAccess INSTANCE = new MapKeyMappingAccess();

		@Override
		public Class<? extends UserType<?>> customType(MemberDetails attribute, ModelsContext context) {
			final MapKeyType customType = attribute.locateAnnotationUsage( MapKeyType.class, context );
			return customType == null ? null : customType.value();

		}

		@Override
		public Map<String,String> customTypeParameters(MemberDetails attribute, ModelsContext context) {
			final MapKeyType customType = attribute.locateAnnotationUsage( MapKeyType.class, context );
			return customType == null ? null : extractParameterMap( customType.parameters() );

		}
	}

	private static class CollectionIdMappingAccess implements BasicMappingAccess {
		private static final CollectionIdMappingAccess INSTANCE = new CollectionIdMappingAccess();

		@Override
		public Class<? extends UserType<?>> customType(MemberDetails attribute, ModelsContext context) {
			final CollectionIdType customType = attribute.locateAnnotationUsage( CollectionIdType.class, context );
			return customType == null ? null : customType.value();

		}

		@Override
		public Map<String,String> customTypeParameters(MemberDetails attribute, ModelsContext context) {
			final CollectionIdType customType = attribute.locateAnnotationUsage( CollectionIdType.class, context );
			return customType == null ? null : extractParameterMap( customType.parameters() );

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
	}

	private static AnnotatedJoinColumns convertToJoinColumns(AnnotatedColumns columns, MetadataBuildingContext context) {
		final AnnotatedJoinColumns joinColumns = new AnnotatedJoinColumns();
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
			final PersistentClass referencedEntity = persistentClasses.get( referencedEntityName );
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
