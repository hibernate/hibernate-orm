/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.TimeZoneStorageStrategy;
import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.annotations.AnyKeyJavaType;
import org.hibernate.annotations.AnyKeyJdbcType;
import org.hibernate.annotations.AnyKeyJdbcTypeCode;
import org.hibernate.annotations.CollectionId;
import org.hibernate.annotations.CollectionIdJavaType;
import org.hibernate.annotations.CollectionIdJdbcType;
import org.hibernate.annotations.CollectionIdJdbcTypeCode;
import org.hibernate.annotations.CollectionIdMutability;
import org.hibernate.annotations.CollectionIdType;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.ListIndexJavaType;
import org.hibernate.annotations.ListIndexJdbcType;
import org.hibernate.annotations.ListIndexJdbcTypeCode;
import org.hibernate.annotations.MapKeyJavaType;
import org.hibernate.annotations.MapKeyJdbcType;
import org.hibernate.annotations.MapKeyJdbcTypeCode;
import org.hibernate.annotations.MapKeyMutability;
import org.hibernate.annotations.MapKeyType;
import org.hibernate.annotations.Mutability;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.PartitionKey;
import org.hibernate.annotations.Target;
import org.hibernate.annotations.TimeZoneColumn;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.model.TypeDefinition;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.spi.AccessType;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.NationalizationSupport;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Table;
import org.hibernate.resource.beans.internal.FallbackBeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.type.BasicType;
import org.hibernate.type.SerializableToBlobType;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.java.Immutability;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.DynamicParameterizedType;
import org.hibernate.usertype.UserType;

import org.jboss.logging.Logger;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.MapKeyClass;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.MapKeyTemporal;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Version;

import static org.hibernate.boot.model.internal.HCANNHelper.findAnnotation;

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

	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class, BasicValueBinder.class.getName() );

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

	private String explicitBasicTypeName;
	private Class<? extends UserType<?>> explicitCustomType;
	private Map<String,String> explicitLocalTypeParams;

	private Function<TypeConfiguration, JdbcType> explicitJdbcTypeAccess;
	private Function<TypeConfiguration, BasicJavaType> explicitJavaTypeAccess;
	private Function<TypeConfiguration, MutabilityPlan> explicitMutabilityAccess;
	private Function<TypeConfiguration, java.lang.reflect.Type> implicitJavaTypeAccess;

	private XProperty xproperty;
	private AccessType accessType;

	private ConverterDescriptor converterDescriptor;

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

	private String timeStampVersionType;
	private String persistentClassName;
	private String propertyName;
	private String returnedClassName;
	private String referencedEntityName;

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
				: buildingContext.getMetadataCollector().getDatabase().getDialect().getAggregateSupport()
				.aggregateComponentSqlTypeCode( aggregateComponent.getAggregateColumn().getSqlTypeCode(), jdbcTypeCode );
	}

	@Override
	public boolean isNationalized() {
		if ( isNationalized ) {
			return true;
		}
		if ( explicitJdbcTypeAccess != null ) {
			final JdbcType type = explicitJdbcTypeAccess.apply( getTypeConfiguration() );
			if ( type != null ) {
				return type.isNationalized();
			}
		}
		return false;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// in-flight handling

	public void setVersion(boolean isVersion) {
		if ( isVersion && basicValue != null ) {
			basicValue.makeVersion();
		}
	}

	public void setTimestampVersionType(String versionType) {
		this.timeStampVersionType = versionType;
	}

	public void setReferencedEntityName(String referencedEntityName) {
		this.referencedEntityName = referencedEntityName;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
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


	public void setType(
			XProperty modelXProperty,
			XClass modelPropertyTypeXClass,
			String declaringClassName,
			ConverterDescriptor converterDescriptor) {
		this.xproperty = modelXProperty;
		boolean isArray = modelXProperty.isArray();
		if ( modelPropertyTypeXClass == null && !isArray ) {
			// we cannot guess anything
			return;
		}

		if ( columns == null ) {
			throw new AssertionFailure( "`BasicValueBinder#setColumns` should be called before `BasicValueBinder#setType`" );
		}

//		if ( columns.length != 1 ) {
//			throw new AssertionFailure( "Expecting just one column, but found `" + Arrays.toString( columns ) + "`" );
//		}

		final XClass modelTypeXClass = isArray
				? modelXProperty.getElementClass()
				: modelPropertyTypeXClass;

		// If we get into this method we know that there is a Java type for the value
		//		and that it is safe to load on the app classloader.
		final java.lang.reflect.Type modelJavaType = resolveJavaType( modelTypeXClass );
		if ( modelJavaType == null ) {
			throw new IllegalStateException( "BasicType requires Java type" );
		}

		if ( kind != Kind.LIST_INDEX && kind != Kind.MAP_KEY  ) {
			isLob = modelXProperty.isAnnotationPresent( Lob.class );
		}

		if ( getDialect().getNationalizationSupport() == NationalizationSupport.EXPLICIT ) {
			isNationalized = HCANNHelper.findAnnotation( modelXProperty, Nationalized.class ) != null
					|| buildingContext.getBuildingOptions().useNationalizedCharacterData();
		}

		applyJpaConverter( modelXProperty, converterDescriptor );

		final Class<? extends UserType<?>> userTypeImpl = kind.mappingAccess.customType( modelXProperty );
		if ( userTypeImpl != null ) {
			applyExplicitType( userTypeImpl, kind.mappingAccess.customTypeParameters( modelXProperty ) );

			// An explicit custom UserType has top precedence when we get to BasicValue resolution.
			return;
		}
		else if ( modelTypeXClass != null ) {
			final Class<?> basicClass = buildingContext.getBootstrapContext()
					.getReflectionManager()
					.toClass( modelTypeXClass );
			final Class<? extends UserType<?>> registeredUserTypeImpl =
					buildingContext.getMetadataCollector().findRegisteredUserType( basicClass );
			if ( registeredUserTypeImpl != null ) {
				applyExplicitType( registeredUserTypeImpl, new Parameter[0] );
				return;
			}
		}

		switch ( kind ) {
			case ATTRIBUTE: {
				prepareBasicAttribute( declaringClassName, modelXProperty, modelPropertyTypeXClass );
				break;
			}
			case ANY_DISCRIMINATOR: {
				prepareAnyDiscriminator( modelXProperty );
				break;
			}
			case ANY_KEY: {
				prepareAnyKey( modelXProperty );
				break;
			}
			case COLLECTION_ID: {
				prepareCollectionId( modelXProperty );
				break;
			}
			case LIST_INDEX: {
				prepareListIndex( modelXProperty );
				break;
			}
			case MAP_KEY: {
				prepareMapKey( modelXProperty, modelPropertyTypeXClass );
				break;
			}
			case COLLECTION_ELEMENT: {
				prepareCollectionElement( modelXProperty, modelPropertyTypeXClass );
				break;
			}
			default: {
				throw new IllegalArgumentException( "Unexpected binder type : " + kind );
			}
		}

	}

	private void applyExplicitType(Class<? extends UserType<?>> impl, Parameter[] params) {
		this.explicitCustomType = impl;
		this.explicitLocalTypeParams = extractTypeParams( params );
	}

	private Map<String,String> extractTypeParams(Parameter[] parameters) {
		if ( parameters == null || parameters.length == 0 ) {
			return Collections.emptyMap();
		}

		if ( parameters.length == 1 ) {
			return Collections.singletonMap( parameters[0].name(), parameters[0].value() );
		}

		final Map<String,String> map = new HashMap<>();
		for ( Parameter parameter: parameters ) {
			map.put( parameter.name(), parameter.value() );
		}

		return map;
	}

	private void prepareCollectionId(XProperty modelXProperty) {
		final CollectionId collectionIdAnn = modelXProperty.getAnnotation( CollectionId.class );
		if ( collectionIdAnn == null ) {
			throw new MappingException( "idbag mapping missing @CollectionId" );
		}

		final boolean useDeferredBeanContainerAccess = !buildingContext.getBuildingOptions().isAllowExtensionsInCdi();
		final ManagedBeanRegistry beanRegistry = getManagedBeanRegistry();

		explicitBasicTypeName = null;
		implicitJavaTypeAccess = (typeConfiguration) -> null;

		explicitJavaTypeAccess = (typeConfiguration) -> {

			final CollectionIdJavaType javaTypeAnn = findAnnotation( modelXProperty, CollectionIdJavaType.class );
			if ( javaTypeAnn != null ) {
				final Class<? extends BasicJavaType<?>> javaTypeClass = normalizeJavaType( javaTypeAnn.value() );
				if ( javaTypeClass != null ) {
					if ( useDeferredBeanContainerAccess ) {
						return FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( javaTypeClass );
					}
					final ManagedBean<? extends BasicJavaType<?>> bean = beanRegistry.getBean( javaTypeClass );
					return bean.getBeanInstance();
				}
			}

			return null;
		};

		explicitJdbcTypeAccess = (typeConfiguration) -> {
			final CollectionIdJdbcType jdbcTypeAnn = findAnnotation( modelXProperty, CollectionIdJdbcType.class );
			if ( jdbcTypeAnn != null ) {
				final Class<? extends JdbcType> jdbcTypeClass = normalizeJdbcType( jdbcTypeAnn.value() );
				if ( jdbcTypeClass != null ) {
					if ( useDeferredBeanContainerAccess ) {
						return FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( jdbcTypeClass );
					}
					final ManagedBean<? extends JdbcType> managedBean = beanRegistry.getBean( jdbcTypeClass );
					return managedBean.getBeanInstance();
				}
			}

			final CollectionIdJdbcTypeCode jdbcTypeCodeAnn = findAnnotation( modelXProperty, CollectionIdJdbcTypeCode.class );
			if ( jdbcTypeCodeAnn != null ) {
				if ( jdbcTypeCodeAnn.value() != Integer.MIN_VALUE ) {
					return typeConfiguration.getJdbcTypeRegistry().getDescriptor( jdbcTypeCodeAnn.value() );
				}
			}

			return null;
		};

		explicitMutabilityAccess = (typeConfiguration) -> {
			final CollectionIdMutability mutabilityAnn = findAnnotation( modelXProperty, CollectionIdMutability.class );
			if ( mutabilityAnn != null ) {
				final Class<? extends MutabilityPlan<?>> mutabilityClass = mutabilityAnn.value();
				if ( mutabilityClass != null ) {
					return resolveMutability( mutabilityClass );
				}
			}

			// see if the value's type Class is annotated with mutability-related annotations
			if ( implicitJavaTypeAccess != null ) {
				final Class<?> attributeType = ReflectHelper.getClass( implicitJavaTypeAccess.apply( typeConfiguration ) );
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
				final Mutability converterMutabilityAnn = converterDescriptor.getAttributeConverterClass().getAnnotation( Mutability.class );
				if ( converterMutabilityAnn != null ) {
					return resolveMutability( converterMutabilityAnn.value() );
				}

				if ( converterDescriptor.getAttributeConverterClass().isAnnotationPresent( Immutable.class ) ) {
					return ImmutableMutabilityPlan.instance();
				}
			}

			// if there is a UserType, see if its Class is annotated with mutability-related annotations
			final Class<? extends UserType<?>> customTypeImpl = Kind.ATTRIBUTE.mappingAccess.customType( modelXProperty );
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

	private ManagedBeanRegistry getManagedBeanRegistry() {
		return buildingContext.getBootstrapContext()
				.getServiceRegistry()
				.requireService(ManagedBeanRegistry.class);
	}

	private void prepareMapKey(
			XProperty mapAttribute,
			XClass modelPropertyTypeXClass) {
		final XClass mapKeyClass = modelPropertyTypeXClass == null ? mapAttribute.getMapKey() : modelPropertyTypeXClass;
		final java.lang.reflect.Type javaType = resolveJavaType( mapKeyClass );
		implicitJavaTypeAccess = typeConfiguration -> javaType;

		final MapKeyEnumerated mapKeyEnumeratedAnn = mapAttribute.getAnnotation( MapKeyEnumerated.class );
		if ( mapKeyEnumeratedAnn != null ) {
			enumType = mapKeyEnumeratedAnn.value();
		}

		final MapKeyTemporal mapKeyTemporalAnn = mapAttribute.getAnnotation( MapKeyTemporal.class );
		if ( mapKeyTemporalAnn != null ) {
			temporalPrecision = mapKeyTemporalAnn.value();
		}

		final boolean useDeferredBeanContainerAccess = !buildingContext.getBuildingOptions().isAllowExtensionsInCdi();

		explicitJdbcTypeAccess = typeConfiguration -> {
			final MapKeyJdbcType jdbcTypeAnn = findAnnotation( mapAttribute, MapKeyJdbcType.class );
			if ( jdbcTypeAnn != null ) {
				final Class<? extends JdbcType> jdbcTypeClass = normalizeJdbcType( jdbcTypeAnn.value() );
				if ( jdbcTypeClass != null ) {
					if ( useDeferredBeanContainerAccess ) {
						return FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( jdbcTypeClass );
					}
					return getManagedBeanRegistry().getBean( jdbcTypeClass ).getBeanInstance();
				}
			}

			final MapKeyJdbcTypeCode jdbcTypeCodeAnn = findAnnotation( mapAttribute, MapKeyJdbcTypeCode.class );
			if ( jdbcTypeCodeAnn != null ) {
				final int jdbcTypeCode = jdbcTypeCodeAnn.value();
				if ( jdbcTypeCode != Integer.MIN_VALUE ) {
					return typeConfiguration.getJdbcTypeRegistry().getDescriptor( jdbcTypeCode );
				}
			}

			return null;
		};

		explicitJavaTypeAccess = typeConfiguration -> {
			final MapKeyJavaType javaTypeAnn = findAnnotation( mapAttribute, MapKeyJavaType.class );
			if ( javaTypeAnn != null ) {
				final Class<? extends BasicJavaType<?>> javaTypeClass = normalizeJavaType( javaTypeAnn.value() );
				if ( javaTypeClass != null ) {
					if ( useDeferredBeanContainerAccess ) {
						return FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( javaTypeClass );
					}
					return getManagedBeanRegistry().getBean( javaTypeClass ).getBeanInstance();
				}
			}

			final MapKeyClass mapKeyClassAnn = mapAttribute.getAnnotation( MapKeyClass.class );
			if ( mapKeyClassAnn != null ) {
				return (BasicJavaType<?>) typeConfiguration.getJavaTypeRegistry()
						.getDescriptor( mapKeyClassAnn.value() );
			}

			return null;
		};

		explicitMutabilityAccess = typeConfiguration -> {
			final MapKeyMutability mutabilityAnn = findAnnotation( mapAttribute, MapKeyMutability.class );
			if ( mutabilityAnn != null ) {
				final Class<? extends MutabilityPlan<?>> mutabilityClass = mutabilityAnn.value();
				if ( mutabilityClass != null ) {
					return resolveMutability( mutabilityClass );
				}
			}

			// see if the value's Java Class is annotated with mutability-related annotations
			if ( implicitJavaTypeAccess != null ) {
				final Class<?> attributeType = ReflectHelper.getClass( implicitJavaTypeAccess.apply( typeConfiguration ) );
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
				final Mutability converterMutabilityAnn = converterDescriptor.getAttributeConverterClass().getAnnotation( Mutability.class );
				if ( converterMutabilityAnn != null ) {
					return resolveMutability( converterMutabilityAnn.value() );
				}

				if ( converterDescriptor.getAttributeConverterClass().isAnnotationPresent( Immutable.class ) ) {
					return ImmutableMutabilityPlan.instance();
				}
			}

			// if there is a UserType, see if its Class is annotated with mutability-related annotations
			final Class<? extends UserType<?>> customTypeImpl = Kind.MAP_KEY.mappingAccess.customType( mapAttribute );
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

	private void prepareListIndex(XProperty listAttribute) {
		implicitJavaTypeAccess = typeConfiguration -> Integer.class;

		final boolean useDeferredBeanContainerAccess = !buildingContext.getBuildingOptions().isAllowExtensionsInCdi();
		final ManagedBeanRegistry beanRegistry =
				buildingContext.getBootstrapContext().getServiceRegistry()
						.requireService( ManagedBeanRegistry.class );

		explicitJavaTypeAccess = (typeConfiguration) -> {
			final ListIndexJavaType javaTypeAnn = findAnnotation( listAttribute, ListIndexJavaType.class );
			if ( javaTypeAnn != null ) {
				final Class<? extends BasicJavaType<?>> javaTypeClass = normalizeJavaType( javaTypeAnn.value() );
				if ( javaTypeClass != null ) {
					if ( useDeferredBeanContainerAccess ) {
						return FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( javaTypeClass );
					}
					final ManagedBean<? extends BasicJavaType<?>> bean = beanRegistry.getBean( javaTypeClass );
					return bean.getBeanInstance();
				}
			}

			return null;
		};

		explicitJdbcTypeAccess = (typeConfiguration) -> {
			final ListIndexJdbcType jdbcTypeAnn = findAnnotation( listAttribute, ListIndexJdbcType.class );
			if ( jdbcTypeAnn != null ) {
				final Class<? extends JdbcType> jdbcTypeClass = normalizeJdbcType( jdbcTypeAnn.value() );
				if ( jdbcTypeClass != null ) {
					if ( useDeferredBeanContainerAccess ) {
						return FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( jdbcTypeClass );
					}
					final ManagedBean<? extends JdbcType> bean = beanRegistry.getBean( jdbcTypeClass );
					return bean.getBeanInstance();
				}
			}

			final ListIndexJdbcTypeCode jdbcTypeCodeAnn = findAnnotation( listAttribute, ListIndexJdbcTypeCode.class );
			if ( jdbcTypeCodeAnn != null ) {
				return typeConfiguration.getJdbcTypeRegistry().getDescriptor( jdbcTypeCodeAnn.value() );
			}

			return null;
		};
	}

	private void prepareCollectionElement(XProperty attributeXProperty, XClass elementTypeXClass) {

		XClass xclass = elementTypeXClass == null && attributeXProperty.isArray() 
				? attributeXProperty.getElementClass() 
				: elementTypeXClass;
		final java.lang.reflect.Type javaType = resolveJavaType( xclass );
		final Class<Object> javaTypeClass = ReflectHelper.getClass( javaType );

		implicitJavaTypeAccess = typeConfiguration -> javaType;

		final Temporal temporalAnn = attributeXProperty.getAnnotation( Temporal.class );
		if ( temporalAnn != null ) {
			temporalPrecision = temporalAnn.value();
			if ( temporalPrecision == null ) {
				throw new IllegalStateException(
						"No jakarta.persistence.TemporalType defined for @jakarta.persistence.Temporal " +
								"associated with attribute " + attributeXProperty.getDeclaringClass().getName() +
								'.' + attributeXProperty.getName()
				);
			}
		}
		else {
			temporalPrecision = null;
		}

		if ( javaTypeClass.isEnum() ) {
			final Enumerated enumeratedAnn = attributeXProperty.getAnnotation( Enumerated.class );
			if ( enumeratedAnn != null ) {
				enumType = enumeratedAnn.value();
				if ( enumType == null ) {
					throw new IllegalStateException(
							"jakarta.persistence.EnumType was null on @jakarta.persistence.Enumerated " +
									" associated with attribute " + attributeXProperty.getDeclaringClass().getName() +
									'.' + attributeXProperty.getName()
					);
				}
			}
		}
		else {
			enumType = null;
		}

		normalSupplementalDetails( attributeXProperty);

		// layer in support for JPA's approach for specifying a specific Java type for the collection elements...
		final ElementCollection elementCollectionAnn = attributeXProperty.getAnnotation( ElementCollection.class );
		if ( elementCollectionAnn != null
				&& elementCollectionAnn.targetClass() != null
				&& elementCollectionAnn.targetClass() != void.class ) {
			final Function<TypeConfiguration, BasicJavaType> original = explicitJavaTypeAccess;
			explicitJavaTypeAccess = (typeConfiguration) -> {
				final BasicJavaType<?> originalResult = original.apply( typeConfiguration );
				if ( originalResult != null ) {
					return originalResult;
				}

				return (BasicJavaType<?>) typeConfiguration
						.getJavaTypeRegistry()
						.getDescriptor( elementCollectionAnn.targetClass() );
			};
		}
	}

	private void prepareBasicAttribute(String declaringClassName, XProperty attributeDescriptor, XClass attributeType) {
		
		final java.lang.reflect.Type javaType = resolveJavaType( attributeType );
		final Class<Object> javaTypeClass = ReflectHelper.getClass( javaType );

		implicitJavaTypeAccess = typeConfiguration -> javaType;

		final Temporal temporalAnn = attributeDescriptor.getAnnotation( Temporal.class );
		if ( temporalAnn != null ) {
			this.temporalPrecision = temporalAnn.value();
			if ( this.temporalPrecision == null ) {
				throw new IllegalStateException(
						"No jakarta.persistence.TemporalType defined for @jakarta.persistence.Temporal " +
								"associated with attribute " + attributeDescriptor.getDeclaringClass().getName() +
								'.' + attributeDescriptor.getName()
				);
			}
		}
		else {
			this.temporalPrecision = null;
		}

		final Enumerated enumeratedAnn = attributeDescriptor.getAnnotation( Enumerated.class );
		if ( enumeratedAnn != null ) {
			this.enumType = enumeratedAnn.value();
			if ( canUseEnumerated( javaType, javaTypeClass ) ) {
				if ( this.enumType == null ) {
					throw new IllegalStateException(
							"jakarta.persistence.EnumType was null on @jakarta.persistence.Enumerated " +
									" associated with attribute " + attributeDescriptor.getDeclaringClass().getName() +
									'.' + attributeDescriptor.getName()
					);
				}
			}
			else {
				throw new AnnotationException(
						String.format(
								"Property '%s.%s' is annotated '@Enumerated' but its type '%s' is not an enum",
								declaringClassName,
								attributeDescriptor.getName(),
								attributeType.getName()
						)
				);
			}
		}
		else {
			this.enumType = null;
		}

		normalSupplementalDetails( attributeDescriptor);
	}

	private boolean canUseEnumerated(java.lang.reflect.Type javaType, Class<Object> javaTypeClass) {
		if ( javaTypeClass.isEnum() || javaTypeClass.isArray() && javaTypeClass.getComponentType().isEnum() ) {
			return true;
		}
		if ( Collection.class.isAssignableFrom( javaTypeClass ) ) {
			final java.lang.reflect.Type[] typeArguments = ( (ParameterizedType) javaType ).getActualTypeArguments();
			if ( typeArguments.length != 0 ) {
				return ReflectHelper.getClass( typeArguments[0] ).isEnum();
			}
		}
		return false;
	}

	private void prepareAnyDiscriminator(XProperty modelXProperty) {
		final AnyDiscriminator anyDiscriminatorAnn = findAnnotation( modelXProperty, AnyDiscriminator.class );

		implicitJavaTypeAccess = (typeConfiguration) -> {
			if ( anyDiscriminatorAnn != null ) {
				switch ( anyDiscriminatorAnn.value() ) {
					case CHAR: {
						return Character.class;
					}
					case INTEGER: {
						return Integer.class;
					}
					default: {
						return String.class;
					}
				}
			}

			return String.class;
		};

		normalJdbcTypeDetails( modelXProperty);
		normalMutabilityDetails( modelXProperty );

		// layer AnyDiscriminator into the JdbcType resolution
		final Function<TypeConfiguration, JdbcType> originalJdbcTypeResolution = explicitJdbcTypeAccess;
		this.explicitJdbcTypeAccess = (typeConfiguration) -> {
			final JdbcType originalResolution = originalJdbcTypeResolution.apply( typeConfiguration );
			if ( originalResolution != null ) {
				return originalResolution;
			}

			final Class<?> hintedJavaType = (Class<?>) implicitJavaTypeAccess.apply( typeConfiguration );
			final JavaType<Object> hintedDescriptor = typeConfiguration
					.getJavaTypeRegistry()
					.getDescriptor( hintedJavaType );
			return hintedDescriptor.getRecommendedJdbcType( typeConfiguration.getCurrentBaseSqlTypeIndicators() );
		};
	}

	private void prepareAnyKey(XProperty modelXProperty) {
		implicitJavaTypeAccess = (typeConfiguration) -> null;

		final boolean useDeferredBeanContainerAccess = !buildingContext.getBuildingOptions().isAllowExtensionsInCdi();

		explicitJavaTypeAccess = (typeConfiguration) -> {
			final AnyKeyJavaType javaTypeAnn = findAnnotation( modelXProperty, AnyKeyJavaType.class );
			if ( javaTypeAnn != null ) {
				final Class<? extends BasicJavaType<?>> javaTypeClass = normalizeJavaType( javaTypeAnn.value() );

				if ( javaTypeClass != null ) {
					if ( useDeferredBeanContainerAccess ) {
						return FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( javaTypeClass );
					}
					return getManagedBeanRegistry().getBean( javaTypeClass ).getBeanInstance();
				}
			}

			final AnyKeyJavaClass javaClassAnn = findAnnotation( modelXProperty, AnyKeyJavaClass.class );
			if ( javaClassAnn != null ) {
				//noinspection rawtypes
				return (BasicJavaType) typeConfiguration
						.getJavaTypeRegistry()
						.getDescriptor( javaClassAnn.value() );
			}

			throw new MappingException("Could not determine key type for '@Any' mapping (specify '@AnyKeyJavaType' or '@AnyKeyJavaClass')");
		};

		explicitJdbcTypeAccess = (typeConfiguration) -> {
			final AnyKeyJdbcType jdbcTypeAnn = findAnnotation( modelXProperty, AnyKeyJdbcType.class );
			if ( jdbcTypeAnn != null ) {
				final Class<? extends JdbcType> jdbcTypeClass = normalizeJdbcType( jdbcTypeAnn.value() );
				if ( jdbcTypeClass != null ) {
					if ( useDeferredBeanContainerAccess ) {
						return FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( jdbcTypeClass );
					}
					final ManagedBean<? extends JdbcType> jtdBean = getManagedBeanRegistry().getBean( jdbcTypeClass );
					return jtdBean.getBeanInstance();
				}
			}

			final AnyKeyJdbcTypeCode jdbcTypeCodeAnn = findAnnotation( modelXProperty, AnyKeyJdbcTypeCode.class );
			if ( jdbcTypeCodeAnn != null ) {
				if ( jdbcTypeCodeAnn.value() != Integer.MIN_VALUE ) {
					return typeConfiguration.getJdbcTypeRegistry().getDescriptor( jdbcTypeCodeAnn.value() );
				}
			}

			return null;
		};
	}

	private void normalJdbcTypeDetails(XProperty attributeXProperty) {
		explicitJdbcTypeAccess = typeConfiguration -> {

			final org.hibernate.annotations.JdbcType jdbcTypeAnn = findAnnotation( attributeXProperty, org.hibernate.annotations.JdbcType.class );
			if ( jdbcTypeAnn != null ) {
				final Class<? extends JdbcType> jdbcTypeClass = normalizeJdbcType( jdbcTypeAnn.value() );
				if ( jdbcTypeClass != null ) {
					if ( !buildingContext.getBuildingOptions().isAllowExtensionsInCdi() ) {
						return FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( jdbcTypeClass );
					}
					return getManagedBeanRegistry().getBean( jdbcTypeClass ).getBeanInstance();
				}
			}

			final JdbcTypeCode jdbcTypeCodeAnn = findAnnotation( attributeXProperty, JdbcTypeCode.class );
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

	private void normalMutabilityDetails(XProperty attributeXProperty) {
		explicitMutabilityAccess = typeConfiguration -> {
			// Look for `@Mutability` on the attribute
			final Mutability mutabilityAnn = findAnnotation( attributeXProperty, Mutability.class );
			if ( mutabilityAnn != null ) {
				final Class<? extends MutabilityPlan<?>> mutability = mutabilityAnn.value();
				if ( mutability != null ) {
					return resolveMutability( mutability );
				}
			}

			// Look for `@Immutable` on the attribute
			final Immutable immutableAnn = attributeXProperty.getAnnotation( Immutable.class );
			if ( immutableAnn != null ) {
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
				final Mutability converterMutabilityAnn = converterDescriptor.getAttributeConverterClass().getAnnotation( Mutability.class );
				if ( converterMutabilityAnn != null ) {
					final Class<? extends MutabilityPlan<?>> mutability = converterMutabilityAnn.value();
					return resolveMutability( mutability );
				}

				final Immutable converterImmutableAnn = converterDescriptor.getAttributeConverterClass().getAnnotation( Immutable.class );
				if ( converterImmutableAnn != null ) {
					return ImmutableMutabilityPlan.instance();
				}
			}

			// if a custom UserType is specified, see if the UserType Class is annotated `@Mutability`
			final Class<? extends UserType<?>> customTypeImpl = Kind.ATTRIBUTE.mappingAccess.customType( attributeXProperty );
			if ( customTypeImpl != null ) {
				final Mutability customTypeMutabilityAnn = customTypeImpl.getAnnotation( Mutability.class );
				if ( customTypeMutabilityAnn != null ) {
					final Class<? extends MutabilityPlan<?>> mutability = customTypeMutabilityAnn.value();
					return resolveMutability( mutability );
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

		if ( mutability.equals( ImmutableMutabilityPlan.class ) ) {
			return ImmutableMutabilityPlan.instance();
		}

		if ( !buildingContext.getBuildingOptions().isAllowExtensionsInCdi() ) {
			return FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( mutability );
		}

		return getManagedBeanRegistry().getBean( mutability ).getBeanInstance();
	}

	private void normalSupplementalDetails(XProperty attributeXProperty) {

		explicitJavaTypeAccess = typeConfiguration -> {
			final org.hibernate.annotations.JavaType javaType =
					findAnnotation( attributeXProperty, org.hibernate.annotations.JavaType.class );
			if ( javaType != null ) {
				final Class<? extends BasicJavaType<?>> javaTypeClass = normalizeJavaType( javaType.value() );
				if ( javaTypeClass != null ) {
					if ( !buildingContext.getBuildingOptions().isAllowExtensionsInCdi() ) {
						return FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( javaTypeClass );
					}
					return getManagedBeanRegistry().getBean( javaTypeClass ).getBeanInstance();
				}
			}

			final Target targetAnn = findAnnotation( attributeXProperty, Target.class );
			if ( targetAnn != null ) {
				return (BasicJavaType<?>) typeConfiguration.getJavaTypeRegistry().getDescriptor( targetAnn.value() );
			}

			return null;
		};

		final org.hibernate.annotations.JdbcTypeCode jdbcType =
				findAnnotation( attributeXProperty, org.hibernate.annotations.JdbcTypeCode.class );
		if ( jdbcType != null ) {
			jdbcTypeCode = jdbcType.value();
		}

		normalJdbcTypeDetails( attributeXProperty);
		normalMutabilityDetails( attributeXProperty );

		final Enumerated enumerated = attributeXProperty.getAnnotation( Enumerated.class );
		if ( enumerated != null ) {
			enumType = enumerated.value();
		}

		final Temporal temporal = attributeXProperty.getAnnotation( Temporal.class );
		if ( temporal != null ) {
			temporalPrecision = temporal.value();
		}

		final TimeZoneStorage timeZoneStorage = attributeXProperty.getAnnotation( TimeZoneStorage.class );
		if ( timeZoneStorage != null ) {
			timeZoneStorageType = timeZoneStorage.value();
			final TimeZoneColumn timeZoneColumnAnn = attributeXProperty.getAnnotation( TimeZoneColumn.class );
			if ( timeZoneColumnAnn != null ) {
				if ( timeZoneStorageType != TimeZoneStorageType.AUTO && timeZoneStorageType != TimeZoneStorageType.COLUMN ) {
					throw new IllegalStateException(
							"@TimeZoneColumn can not be used in conjunction with @TimeZoneStorage( " + timeZoneStorageType +
									" ) with attribute " + attributeXProperty.getDeclaringClass().getName() +
									'.' + attributeXProperty.getName()
					);
				}
			}
		}
		final PartitionKey partitionKey = attributeXProperty.getAnnotation( PartitionKey.class );
		if ( partitionKey != null ) {
			this.partitionKey = true;
		}
	}

	private static Class<? extends UserType<?>> normalizeUserType(Class<? extends UserType<?>> userType) {
		return userType;
	}

	private Class<? extends JdbcType> normalizeJdbcType(Class<? extends JdbcType> jdbcType) {
		return jdbcType;
	}

	private static Class<? extends BasicJavaType<?>> normalizeJavaType(Class<? extends BasicJavaType<?>> javaType) {
		return javaType;
	}

	private java.lang.reflect.Type resolveJavaType(XClass returnedClassOrElement) {
		return buildingContext.getBootstrapContext()
				.getReflectionManager()
				.toType( returnedClassOrElement );
	}

	@Override
	public Dialect getDialect() {
		return buildingContext.getMetadataCollector().getDatabase().getDialect();
	}

	private void applyJpaConverter(XProperty property, ConverterDescriptor attributeConverterDescriptor) {
		if ( attributeConverterDescriptor == null ) {
			return;
		}

		LOG.debugf( "Applying JPA converter [%s:%s]", persistentClassName, property.getName() );

		if ( property.isAnnotationPresent( Id.class ) ) {
			LOG.debugf( "Skipping AttributeConverter checks for Id attribute [%s]", property.getName() );
			return;
		}

		if ( property.isAnnotationPresent( Version.class ) ) {
			LOG.debugf( "Skipping AttributeConverter checks for version attribute [%s]", property.getName() );
			return;
		}

		if ( kind == Kind.MAP_KEY ) {
			if ( property.isAnnotationPresent( MapKeyTemporal.class ) ) {
				LOG.debugf( "Skipping AttributeConverter checks for map-key annotated as MapKeyTemporal [%s]", property.getName() );
				return;
			}

			if ( property.isAnnotationPresent( MapKeyEnumerated.class ) ) {
				LOG.debugf( "Skipping AttributeConverter checks for map-key annotated as MapKeyEnumerated [%s]", property.getName() );
				return;
			}
		}
		else {
			if ( property.isAnnotationPresent( Temporal.class ) ) {
				LOG.debugf( "Skipping AttributeConverter checks for Temporal attribute [%s]", property.getName() );
				return;
			}

			if ( property.isAnnotationPresent( Enumerated.class ) ) {
				LOG.debugf( "Skipping AttributeConverter checks for Enumerated attribute [%s]", property.getName() );
				return;
			}
		}

		if ( isAssociation() ) {
			LOG.debugf( "Skipping AttributeConverter checks for association attribute [%s]", property.getName() );
			return;
		}

		this.converterDescriptor = attributeConverterDescriptor;
	}

	private boolean isAssociation() {
		// todo : this information is only known to caller(s), need to pass that information in somehow.
		// or, is this enough?
		return referencedEntityName != null;
	}

	public void setExplicitType(String explicitType) {
		this.explicitBasicTypeName = explicitType;
	}

	public BasicValue make() {
		if ( basicValue != null ) {
			return basicValue;
		}

		columns.checkPropertyConsistency();

		LOG.debugf( "building BasicValue for %s", propertyName );

		if ( table == null ) {
			table = columns.getTable();
		}

		basicValue = new BasicValue( buildingContext, table );

		if ( columns.getPropertyHolder().isComponent() ) {
			final ComponentPropertyHolder propertyHolder = (ComponentPropertyHolder) columns.getPropertyHolder();
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

		boolean isInSecondPass = buildingContext.getMetadataCollector().isInSecondPass();
		if ( !isInSecondPass ) {
			//Defer this to the second pass
			buildingContext.getMetadataCollector().addSecondPass( new SetBasicValueTypeSecondPass( this ) );
		}
		else {
			//We are already in second pass
			fillSimpleValue();
		}

		return basicValue;
	}

	public void linkWithValue() {
		final InFlightMetadataCollector collector = buildingContext.getMetadataCollector();
		final AnnotatedColumn firstColumn = columns.getColumns().get(0);
		if ( !collector.isInSecondPass() && firstColumn.isNameDeferred() && referencedEntityName != null ) {
			final AnnotatedJoinColumns joinColumns = new AnnotatedJoinColumns();
			joinColumns.setBuildingContext( buildingContext );
			joinColumns.setPropertyHolder( columns.getPropertyHolder() );
			joinColumns.setPropertyName( columns.getPropertyName() );
			//TODO: resetting the parent here looks like a dangerous thing to do
			//      should we be cloning them first (the legacy code did not)
			for ( AnnotatedColumn column : columns.getColumns() ) {
				column.setParent( joinColumns );
			}
			collector.addSecondPass(
					new PkDrivenByDefaultMapsIdSecondPass( referencedEntityName, joinColumns, basicValue )
			);
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
		LOG.debugf( "Starting `BasicValueBinder#fillSimpleValue` for %s", propertyName );

		final String explicitBasicTypeName = this.explicitBasicTypeName != null
				? this.explicitBasicTypeName
				: this.timeStampVersionType;
		basicValue.setExplicitTypeName( explicitBasicTypeName );
		basicValue.setExplicitTypeParams( explicitLocalTypeParams );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// todo (6.0) : we are dropping support for @Type and @TypeDef from annotations
		//		so this handling can go away.  most of this (enum, temporal, ect) will be
		//		handled by BasicValue already.  this stuff is all just to drive
		// 		DynamicParameterizedType handling - just pass them (or a Supplier?) into
		//		BasicValue so that it has access to them as needed

		Class<?> typeClass = null;

		if ( explicitBasicTypeName != null ) {
			final TypeDefinition typeDefinition = buildingContext
					.getTypeDefinitionRegistry()
					.resolve( explicitBasicTypeName );
			if ( typeDefinition == null ) {
				final BasicType<?> registeredType = getTypeConfiguration()
						.getBasicTypeRegistry()
						.getRegisteredType( explicitBasicTypeName );
				if ( registeredType == null ) {
					typeClass = buildingContext
							.getBootstrapContext()
							.getClassLoaderAccess()
							.classForName( explicitBasicTypeName );
				}
			}
			else {
				typeClass = typeDefinition.getTypeImplementorClass();
			}
		}
		// Enum type is parameterized and prior to Hibernate 6 we always resolved the type class
		else if ( enumType != null || isEnum() ) {
			typeClass = org.hibernate.type.EnumType.class;
		}
		// The Lob type is parameterized and prior to Hibernate 6 we always resolved the type class
		else if ( isLob || isSerializable() ) {
			typeClass = SerializableToBlobType.class;
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		if ( explicitCustomType != null && DynamicParameterizedType.class.isAssignableFrom( explicitCustomType )
				|| typeClass != null && DynamicParameterizedType.class.isAssignableFrom( typeClass ) ) {
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
			throw new MappingException( "Returned class name not specified for basic mapping: " + xproperty.getName() );
		}

		parameters.put( DynamicParameterizedType.RETURNED_CLASS, returnedClassName );
		parameters.put( DynamicParameterizedType.XPROPERTY, xproperty );
		parameters.put( DynamicParameterizedType.PROPERTY, xproperty.getName() );

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

	private boolean isEnum() {
		Class<?> clazz = null;
		if ( implicitJavaTypeAccess != null ) {
			java.lang.reflect.Type type = implicitJavaTypeAccess.apply( getTypeConfiguration() );
			if ( type instanceof ParameterizedType ) {
				type = ( (ParameterizedType) type ).getRawType();
			}
			if ( type instanceof Class<?> ) {
				clazz = (Class<?>) type;
			}
		}
		return clazz != null && clazz.isEnum();
	}

	private boolean isSerializable() {
		Class<?> clazz = null;
		if ( implicitJavaTypeAccess != null ) {
			java.lang.reflect.Type type = implicitJavaTypeAccess.apply( getTypeConfiguration() );
			if ( type instanceof ParameterizedType ) {
				type = ( (ParameterizedType) type ).getRawType();
			}
			if ( type instanceof Class<?> ) {
				clazz = (Class<?>) type;
			}
		}
		return clazz != null && Serializable.class.isAssignableFrom( clazz );
	}




	/**
	 * Access to detail of basic value mappings based on {@link Kind}
	 */
	private interface BasicMappingAccess {
		Class<? extends UserType<?>> customType(XProperty xProperty);
		Parameter[] customTypeParameters(XProperty xProperty);
	}

	private static class ValueMappingAccess implements BasicMappingAccess {
		public static final ValueMappingAccess INSTANCE = new ValueMappingAccess();

		@Override
		public Class<? extends UserType<?>> customType(XProperty xProperty) {
			final Type customType = findAnnotation( xProperty, Type.class );
			if ( customType == null ) {
				return null;
			}

			return normalizeUserType( customType.value() );
		}

		@Override
		public Parameter[] customTypeParameters(XProperty xProperty) {
			final Type customType = findAnnotation( xProperty, Type.class );
			if ( customType == null ) {
				return null;
			}
			return customType.parameters();
		}
	}

	private static class AnyDiscriminatorMappingAccess implements BasicMappingAccess {
		public static final AnyDiscriminatorMappingAccess INSTANCE = new AnyDiscriminatorMappingAccess();

		@Override
		public Class<? extends UserType<?>> customType(XProperty xProperty) {
			return null;
		}

		private static final Parameter[] EMPTY_PARAMS = new Parameter[0];

		@Override
		public Parameter[] customTypeParameters(XProperty xProperty) {
			return EMPTY_PARAMS;
		}
	}

	private static class AnyKeyMappingAccess implements BasicMappingAccess {
		public static final AnyKeyMappingAccess INSTANCE = new AnyKeyMappingAccess();

		@Override
		public Class<? extends UserType<?>> customType(XProperty xProperty) {
			return null;
		}

		private static final Parameter[] EMPTY_PARAMS = new Parameter[0];

		@Override
		public Parameter[] customTypeParameters(XProperty xProperty) {
			return EMPTY_PARAMS;
		}
	}

	private static class MapKeyMappingAccess implements BasicMappingAccess {
		public static final MapKeyMappingAccess INSTANCE = new MapKeyMappingAccess();

		@Override
		public Class<? extends UserType<?>> customType(XProperty xProperty) {
			final MapKeyType customType = findAnnotation( xProperty, MapKeyType.class );
			if ( customType == null ) {
				return null;
			}

			return normalizeUserType( customType.value() );
		}

		@Override
		public Parameter[] customTypeParameters(XProperty xProperty) {
			final MapKeyType customType = findAnnotation( xProperty, MapKeyType.class );
			if ( customType == null ) {
				return null;
			}
			return customType.parameters();
		}
	}

	private static class CollectionIdMappingAccess implements BasicMappingAccess {
		public static final CollectionIdMappingAccess INSTANCE = new CollectionIdMappingAccess();

		@Override
		public Class<? extends UserType<?>> customType(XProperty xProperty) {
			final CollectionIdType customType = findAnnotation( xProperty, CollectionIdType.class );
			if ( customType == null ) {
				return null;
			}

			return normalizeUserType( customType.value() );
		}

		@Override
		public Parameter[] customTypeParameters(XProperty xProperty) {
			final CollectionIdType customType = findAnnotation( xProperty, CollectionIdType.class );
			if ( customType == null ) {
				return null;
			}
			return customType.parameters();
		}
	}

	private static class ListIndexMappingAccess implements BasicMappingAccess {
		public static final ListIndexMappingAccess INSTANCE = new ListIndexMappingAccess();

		@Override
		public Class<? extends UserType<?>> customType(XProperty xProperty) {
			return null;
		}

		@Override
		public Parameter[] customTypeParameters(XProperty xProperty) {
			return null;
		}
	}
}
