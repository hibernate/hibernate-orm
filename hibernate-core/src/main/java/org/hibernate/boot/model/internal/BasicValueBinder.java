/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
import org.hibernate.annotations.PartitionKey;
import org.hibernate.annotations.Target;
import org.hibernate.annotations.TimeZoneColumn;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.annotations.Type;
import org.hibernate.boot.internal.AnyKeyType;
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
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.ParameterizedTypeDetails;
import org.hibernate.models.spi.SourceModelBuildingContext;
import org.hibernate.models.spi.TypeDetails;
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
import org.hibernate.type.internal.ParameterizedTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.DynamicParameterizedType;
import org.hibernate.usertype.UserType;

import org.jboss.logging.Logger;

import jakarta.persistence.DiscriminatorType;
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

	private static final CoreMessageLogger LOG = Logger.getMessageLogger( MethodHandles.lookup(), CoreMessageLogger.class, BasicValueBinder.class.getName() );

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

	private MemberDetails xproperty;
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

	protected SourceModelBuildingContext getSourceModelContext() {
		return buildingContext.getMetadataCollector().getSourceModelBuildingContext();
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
			MemberDetails valueMember,
			TypeDetails typeDetails,
			String declaringClassName,
			ConverterDescriptor converterDescriptor) {
		this.xproperty = valueMember;
		boolean isArray = valueMember.isArray();
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

		final TypeDetails modelClassDetails = isArray
				? valueMember.getElementType()
				: typeDetails;

		if ( kind != Kind.LIST_INDEX && kind != Kind.MAP_KEY  ) {
			isLob = valueMember.hasDirectAnnotationUsage( Lob.class );
		}

		if ( getDialect().getNationalizationSupport() == NationalizationSupport.EXPLICIT ) {
			isNationalized = buildingContext.getBuildingOptions().useNationalizedCharacterData()
					|| valueMember.locateAnnotationUsage( Nationalized.class, getSourceModelContext() ) != null;
		}

		applyJpaConverter( valueMember, converterDescriptor );

		final Class<? extends UserType<?>> userTypeImpl = kind.mappingAccess.customType( valueMember, getSourceModelContext() );
		if ( userTypeImpl != null ) {
			applyExplicitType( userTypeImpl, kind.mappingAccess.customTypeParameters( valueMember, getSourceModelContext() ) );

			// An explicit custom UserType has top precedence when we get to BasicValue resolution.
			return;
		}
		else if ( modelClassDetails != null ) {
			final ClassDetails rawClassDetails = modelClassDetails.determineRawClass();
			final Class<?> basicClass = rawClassDetails.toJavaClass();
			final Class<? extends UserType<?>> registeredUserTypeImpl =
					buildingContext.getMetadataCollector().findRegisteredUserType( basicClass );
			if ( registeredUserTypeImpl != null ) {
				applyExplicitType( registeredUserTypeImpl, Collections.emptyMap() );
				return;
			}
		}

		switch ( kind ) {
			case ATTRIBUTE: {
				prepareBasicAttribute( declaringClassName, valueMember, typeDetails );
				break;
			}
			case ANY_DISCRIMINATOR: {
				prepareAnyDiscriminator( valueMember );
				break;
			}
			case ANY_KEY: {
				prepareAnyKey( valueMember );
				break;
			}
			case COLLECTION_ID: {
				prepareCollectionId( valueMember );
				break;
			}
			case LIST_INDEX: {
				prepareListIndex( valueMember );
				break;
			}
			case MAP_KEY: {
				prepareMapKey( valueMember, typeDetails );
				break;
			}
			case COLLECTION_ELEMENT: {
				prepareCollectionElement( valueMember, typeDetails );
				break;
			}
			default: {
				throw new IllegalArgumentException( "Unexpected binder type : " + kind );
			}
		}

	}

	private void applyExplicitType(Class<? extends UserType<?>> impl, Map<String,String> params) {
		this.explicitCustomType = impl;
		this.explicitLocalTypeParams = params;
	}

	private void prepareCollectionId(MemberDetails attributeMember) {
		final CollectionId collectionIdAnn = attributeMember.getDirectAnnotationUsage( CollectionId.class );
		if ( collectionIdAnn == null ) {
			throw new MappingException( "idbag mapping missing @CollectionId" );
		}

		final boolean useDeferredBeanContainerAccess = !buildingContext.getBuildingOptions().isAllowExtensionsInCdi();
		final ManagedBeanRegistry beanRegistry = getManagedBeanRegistry();

		explicitBasicTypeName = null;
		implicitJavaTypeAccess = (typeConfiguration) -> null;

		explicitJavaTypeAccess = (typeConfiguration) -> {
			final CollectionIdJavaType javaTypeAnn = attributeMember.locateAnnotationUsage( CollectionIdJavaType.class, getSourceModelContext() );
			if ( javaTypeAnn != null ) {
				final Class<? extends BasicJavaType<?>> javaTypeClass = javaTypeAnn.value();
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
			final CollectionIdJdbcType jdbcTypeAnn = attributeMember.locateAnnotationUsage( CollectionIdJdbcType.class, getSourceModelContext() );
			if ( jdbcTypeAnn != null ) {
				final Class<? extends JdbcType> jdbcTypeClass = jdbcTypeAnn.value();
				if ( jdbcTypeClass != null ) {
					if ( useDeferredBeanContainerAccess ) {
						return FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( jdbcTypeClass );
					}
					final ManagedBean<? extends JdbcType> managedBean = beanRegistry.getBean( jdbcTypeClass );
					return managedBean.getBeanInstance();
				}
			}

			final CollectionIdJdbcTypeCode jdbcTypeCodeAnn = attributeMember.locateAnnotationUsage( CollectionIdJdbcTypeCode.class, getSourceModelContext() );
			if ( jdbcTypeCodeAnn != null ) {
				final int code = jdbcTypeCodeAnn.value();
				if ( code != Integer.MIN_VALUE ) {
					return typeConfiguration.getJdbcTypeRegistry().getDescriptor( code );
				}
			}

			return null;
		};

		explicitMutabilityAccess = (typeConfiguration) -> {
			final CollectionIdMutability mutabilityAnn = attributeMember.locateAnnotationUsage( CollectionIdMutability.class, getSourceModelContext() );
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
			final Class<? extends UserType<?>> customTypeImpl = Kind.ATTRIBUTE.mappingAccess.customType( attributeMember, getSourceModelContext() );
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
			MemberDetails attributeMember,
			TypeDetails explicitMapKeyTypeDetails) {
		final TypeDetails mapKeyClass = explicitMapKeyTypeDetails == null
				? attributeMember.getMapKeyType()
				: explicitMapKeyTypeDetails;
		implicitJavaTypeAccess = typeConfiguration -> {
			final ClassDetails rawKeyClassDetails = mapKeyClass.determineRawClass();
			return rawKeyClassDetails.toJavaClass();
		};

		final MapKeyEnumerated mapKeyEnumeratedAnn = attributeMember.getDirectAnnotationUsage( MapKeyEnumerated.class );
		if ( mapKeyEnumeratedAnn != null ) {
			enumType = mapKeyEnumeratedAnn.value();
		}

		final MapKeyTemporal mapKeyTemporalAnn = attributeMember.getDirectAnnotationUsage( MapKeyTemporal.class );
		if ( mapKeyTemporalAnn != null ) {
			temporalPrecision = mapKeyTemporalAnn.value();
		}

		final boolean useDeferredBeanContainerAccess = !buildingContext.getBuildingOptions().isAllowExtensionsInCdi();

		explicitJdbcTypeAccess = typeConfiguration -> {
			final MapKeyJdbcType jdbcTypeAnn = attributeMember.locateAnnotationUsage( MapKeyJdbcType.class, getSourceModelContext() );
			if ( jdbcTypeAnn != null ) {
				final Class<? extends JdbcType> jdbcTypeClass = jdbcTypeAnn.value();
				if ( jdbcTypeClass != null ) {
					if ( useDeferredBeanContainerAccess ) {
						return FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( jdbcTypeClass );
					}
					return getManagedBeanRegistry().getBean( jdbcTypeClass ).getBeanInstance();
				}
			}

			final MapKeyJdbcTypeCode jdbcTypeCodeAnn = attributeMember.locateAnnotationUsage( MapKeyJdbcTypeCode.class, getSourceModelContext() );
			if ( jdbcTypeCodeAnn != null ) {
				final int jdbcTypeCode = jdbcTypeCodeAnn.value();
				if ( jdbcTypeCode != Integer.MIN_VALUE ) {
					return typeConfiguration.getJdbcTypeRegistry().getDescriptor( jdbcTypeCode );
				}
			}

			return null;
		};

		explicitJavaTypeAccess = typeConfiguration -> {
			final MapKeyJavaType javaTypeAnn = attributeMember.locateAnnotationUsage( MapKeyJavaType.class, getSourceModelContext() );
			if ( javaTypeAnn != null ) {
				final Class<? extends BasicJavaType<?>> javaTypeClass = javaTypeAnn.value();
				if ( javaTypeClass != null ) {
					if ( useDeferredBeanContainerAccess ) {
						return FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( javaTypeClass );
					}
					return getManagedBeanRegistry().getBean( javaTypeClass ).getBeanInstance();
				}
			}

			final MapKeyClass mapKeyClassAnn = attributeMember.getDirectAnnotationUsage( MapKeyClass.class );
			if ( mapKeyClassAnn != null ) {
				return (BasicJavaType<?>) typeConfiguration.getJavaTypeRegistry().getDescriptor( mapKeyClassAnn.value() );
			}

			return null;
		};

		explicitMutabilityAccess = typeConfiguration -> {
			final MapKeyMutability mutabilityAnn = attributeMember.locateAnnotationUsage( MapKeyMutability.class, getSourceModelContext() );
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
			final Class<? extends UserType<?>> customTypeImpl = Kind.MAP_KEY.mappingAccess.customType( attributeMember, getSourceModelContext() );
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

	private void prepareListIndex(MemberDetails attributeMember) {
		implicitJavaTypeAccess = typeConfiguration -> Integer.class;

		final boolean useDeferredBeanContainerAccess = !buildingContext.getBuildingOptions().isAllowExtensionsInCdi();
		final ManagedBeanRegistry beanRegistry =
				buildingContext.getBootstrapContext().getServiceRegistry()
						.requireService( ManagedBeanRegistry.class );

		explicitJavaTypeAccess = (typeConfiguration) -> {
			final ListIndexJavaType javaTypeAnn = attributeMember.locateAnnotationUsage( ListIndexJavaType.class, getSourceModelContext() );
			if ( javaTypeAnn != null ) {
				final Class<? extends BasicJavaType<?>> javaTypeClass = javaTypeAnn.value();
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
			final ListIndexJdbcType jdbcTypeAnn = attributeMember.locateAnnotationUsage( ListIndexJdbcType.class, getSourceModelContext() );
			if ( jdbcTypeAnn != null ) {
				final Class<? extends JdbcType> jdbcTypeClass = jdbcTypeAnn.value();
				if ( jdbcTypeClass != null ) {
					if ( useDeferredBeanContainerAccess ) {
						return FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( jdbcTypeClass );
					}
					final ManagedBean<? extends JdbcType> bean = beanRegistry.getBean( jdbcTypeClass );
					return bean.getBeanInstance();
				}
			}

			final ListIndexJdbcTypeCode jdbcTypeCodeAnn = attributeMember.locateAnnotationUsage( ListIndexJdbcTypeCode.class, getSourceModelContext() );
			if ( jdbcTypeCodeAnn != null ) {
				return typeConfiguration.getJdbcTypeRegistry().getDescriptor( jdbcTypeCodeAnn.value() );
			}

			return null;
		};
	}

	private void prepareCollectionElement(
			MemberDetails attributeMember,
			TypeDetails explicitElementTypeDetails) {
		final TypeDetails elementTypeDetails = explicitElementTypeDetails == null && attributeMember.isArray()
				? attributeMember.getElementType()
				: explicitElementTypeDetails;
		final ClassDetails rawElementType = elementTypeDetails.determineRawClass();
		final java.lang.reflect.Type javaType = rawElementType.toJavaClass();
		final Class<Object> javaTypeClass = ReflectHelper.getClass( javaType );

		implicitJavaTypeAccess = typeConfiguration -> javaType;

		final Temporal temporalAnn = attributeMember.getDirectAnnotationUsage( Temporal.class );
		if ( temporalAnn != null ) {
			DEPRECATION_LOGGER.deprecatedAnnotation( Temporal.class, attributeMember.getName() );
			temporalPrecision = temporalAnn.value();
			if ( temporalPrecision == null ) {
				throw new IllegalStateException(
						"No jakarta.persistence.TemporalType defined for @jakarta.persistence.Temporal " +
								"associated with attribute " + attributeMember.getName()
				);
			}
		}
		else {
			temporalPrecision = null;
		}

		if ( javaTypeClass.isEnum() ) {
			final Enumerated enumeratedAnn = attributeMember.getDirectAnnotationUsage( Enumerated.class );
			if ( enumeratedAnn != null ) {
				enumType = enumeratedAnn.value();
				if ( enumType == null ) {
					throw new IllegalStateException(
							"jakarta.persistence.EnumType was null on @jakarta.persistence.Enumerated " +
									" associated with attribute " + attributeMember.getName()
					);
				}
			}
		}
		else {
			enumType = null;
		}

		normalSupplementalDetails( attributeMember);

		// layer in support for JPA's approach for specifying a specific Java type for the collection elements...
		final ElementCollection elementCollectionAnn = attributeMember.getDirectAnnotationUsage( ElementCollection.class );
		if ( elementCollectionAnn != null ) {
			final Class<?> targetClassDetails = elementCollectionAnn.targetClass();
			if ( targetClassDetails != void.class) {
				//noinspection rawtypes
				final Function<TypeConfiguration, BasicJavaType> original = explicitJavaTypeAccess;
				explicitJavaTypeAccess = (typeConfiguration) -> {
					final BasicJavaType<?> originalResult = original.apply( typeConfiguration );
					if ( originalResult != null ) {
						return originalResult;
					}

					return (BasicJavaType<?>) typeConfiguration
							.getJavaTypeRegistry()
							.getDescriptor( targetClassDetails );
				};
			}
		}
	}

	private void prepareBasicAttribute(
			String declaringClassName,
			MemberDetails attributeMember,
			TypeDetails attributeType) {
		final Class<Object> javaTypeClass = attributeType.determineRawClass().toJavaClass();
		implicitJavaTypeAccess = ( typeConfiguration -> {
			if ( attributeType.getTypeKind() == TypeDetails.Kind.PARAMETERIZED_TYPE ) {
				return ParameterizedTypeImpl.from( attributeType.asParameterizedType() );
			}
			return attributeType.determineRawClass().toJavaClass();
		} );

		//noinspection deprecation
		final var temporalAnn = attributeMember.getDirectAnnotationUsage( Temporal.class );
		if ( temporalAnn != null ) {
			//noinspection deprecation
			DEPRECATION_LOGGER.deprecatedAnnotation( Temporal.class, declaringClassName + "." + attributeMember.getName() );
			this.temporalPrecision = temporalAnn.value();
			if ( this.temporalPrecision == null ) {
				throw new IllegalStateException(
						"No jakarta.persistence.TemporalType defined for @jakarta.persistence.Temporal " +
								"associated with attribute " + declaringClassName + "." + attributeMember.getName()
				);
			}
		}
		else {
			this.temporalPrecision = null;
		}

		final Enumerated enumeratedAnn = attributeMember.getDirectAnnotationUsage( Enumerated.class );
		if ( enumeratedAnn != null ) {
			this.enumType = enumeratedAnn.value();
			if ( canUseEnumerated( attributeType, javaTypeClass ) ) {
				if ( this.enumType == null ) {
					throw new IllegalStateException(
							"jakarta.persistence.EnumType was null on @jakarta.persistence.Enumerated " +
									" associated with attribute " + declaringClassName + "." + attributeMember.getName()
					);
				}
			}
			else {
				throw new AnnotationException(
						String.format(
								"Property '%s.%s' is annotated '@Enumerated' but its type '%s' is not an enum",
								declaringClassName,
								attributeMember.getName(),
								attributeType.getName()
						)
				);
			}
		}
		else {
			this.enumType = null;
		}

		normalSupplementalDetails( attributeMember );
	}

	private boolean canUseEnumerated(TypeDetails javaType, Class<Object> javaTypeClass) {
		if ( javaTypeClass.isEnum() || javaTypeClass.isArray() && javaTypeClass.getComponentType().isEnum() ) {
			return true;
		}
		if ( javaType.isImplementor( Collection.class ) ) {
			final ParameterizedTypeDetails parameterizedType = javaType.asParameterizedType();
			final List<TypeDetails> typeArguments = parameterizedType.getArguments();
			if ( !typeArguments.isEmpty() ) {
				return typeArguments.get( 0 ).isImplementor( Enum.class );
			}
		}
		return false;
	}

	private void prepareAnyDiscriminator(MemberDetails memberDetails) {
		final AnyDiscriminator anyDiscriminatorAnn = memberDetails.locateAnnotationUsage( AnyDiscriminator.class, getSourceModelContext() );

		implicitJavaTypeAccess = (typeConfiguration) -> {
			if ( anyDiscriminatorAnn != null ) {
				final DiscriminatorType anyDiscriminatorType = anyDiscriminatorAnn.value();
				return switch ( anyDiscriminatorType ) {
					case CHAR -> Character.class;
					case INTEGER -> Integer.class;
					default -> String.class;
				};
			}

			return String.class;
		};

		normalJdbcTypeDetails( memberDetails);
		normalMutabilityDetails( memberDetails );

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

	private void prepareAnyKey(MemberDetails memberDetails) {
		implicitJavaTypeAccess = (typeConfiguration) -> null;

		final boolean useDeferredBeanContainerAccess = !buildingContext.getBuildingOptions().isAllowExtensionsInCdi();

		explicitJavaTypeAccess = (typeConfiguration) -> {
			final AnyKeyJavaType javaTypeAnn = memberDetails.locateAnnotationUsage( AnyKeyJavaType.class, getSourceModelContext() );
			if ( javaTypeAnn != null ) {
				final Class<? extends BasicJavaType<?>> implClass = javaTypeAnn.value();

				if ( implClass != null ) {
					if ( useDeferredBeanContainerAccess ) {
						return FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( implClass );
					}
					return getManagedBeanRegistry().getBean( implClass ).getBeanInstance();
				}
			}

			final AnyKeyJavaClass javaClassAnn = memberDetails.locateAnnotationUsage( AnyKeyJavaClass.class, getSourceModelContext() );
			if ( javaClassAnn != null ) {
				final Class<?> impl = javaClassAnn.value();
				//noinspection rawtypes
				return (BasicJavaType) typeConfiguration
						.getJavaTypeRegistry()
						.getDescriptor( impl );
			}

			// mainly used in XML interpretation
			final AnyKeyType anyKeyTypeAnn = memberDetails.locateAnnotationUsage( AnyKeyType.class, getSourceModelContext() );
			if ( anyKeyTypeAnn != null ) {
				final String namedType = anyKeyTypeAnn.value();
				final BasicType<Object> registeredType = typeConfiguration.getBasicTypeRegistry().getRegisteredType( namedType );
				if ( registeredType == null ) {
					throw new MappingException( "Unrecognized @AnyKeyType value - " + namedType );
				}
				//noinspection rawtypes
				return (BasicJavaType) registeredType.getJavaTypeDescriptor();
			}

			throw new MappingException("Could not determine key type for '@Any' mapping (specify '@AnyKeyJavaType' or '@AnyKeyJavaClass')");
		};

		explicitJdbcTypeAccess = (typeConfiguration) -> {
			final AnyKeyJdbcType jdbcTypeAnn = memberDetails.locateAnnotationUsage( AnyKeyJdbcType.class, getSourceModelContext() );
			if ( jdbcTypeAnn != null ) {
				final Class<? extends JdbcType> jdbcTypeClass = jdbcTypeAnn.value();
				if ( jdbcTypeClass != null ) {
					if ( useDeferredBeanContainerAccess ) {
						return FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( jdbcTypeClass );
					}
					final ManagedBean<? extends JdbcType> jtdBean = getManagedBeanRegistry().getBean( jdbcTypeClass );
					return jtdBean.getBeanInstance();
				}
			}

			final AnyKeyJdbcTypeCode jdbcTypeCodeAnn = memberDetails.locateAnnotationUsage( AnyKeyJdbcTypeCode.class, getSourceModelContext() );
			if ( jdbcTypeCodeAnn != null ) {
				final int code = jdbcTypeCodeAnn.value();
				if ( code != Integer.MIN_VALUE ) {
					return typeConfiguration.getJdbcTypeRegistry().getDescriptor( code );
				}
			}

			return null;
		};
	}

	private void normalJdbcTypeDetails(MemberDetails attributeMember) {
		explicitJdbcTypeAccess = typeConfiguration -> {
			final org.hibernate.annotations.JdbcType jdbcTypeAnn = attributeMember.locateAnnotationUsage( org.hibernate.annotations.JdbcType.class, getSourceModelContext() );
			if ( jdbcTypeAnn != null ) {
				final Class<? extends JdbcType> jdbcTypeClass = jdbcTypeAnn.value();
				if ( jdbcTypeClass != null ) {
					if ( !buildingContext.getBuildingOptions().isAllowExtensionsInCdi() ) {
						return FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( jdbcTypeClass );
					}
					return getManagedBeanRegistry().getBean( jdbcTypeClass ).getBeanInstance();
				}
			}

			final JdbcTypeCode jdbcTypeCodeAnn = attributeMember.locateAnnotationUsage( JdbcTypeCode.class, getSourceModelContext() );
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

	private void normalMutabilityDetails(MemberDetails attributeMember) {
		explicitMutabilityAccess = typeConfiguration -> {
			// Look for `@Mutability` on the attribute
			final Mutability mutabilityAnn = attributeMember.locateAnnotationUsage( Mutability.class, getSourceModelContext() );
			if ( mutabilityAnn != null ) {
				final Class<? extends MutabilityPlan<?>> mutability = mutabilityAnn.value();
				if ( mutability != null ) {
					return resolveMutability( mutability );
				}
			}

			// Look for `@Immutable` on the attribute
			if ( attributeMember.hasDirectAnnotationUsage( Immutable.class ) ) {
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
			final Class<? extends UserType<?>> customTypeImpl = Kind.ATTRIBUTE.mappingAccess.customType( attributeMember, getSourceModelContext() );
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

	private void normalSupplementalDetails(MemberDetails attributeMember) {
		explicitJavaTypeAccess = typeConfiguration -> {
			final org.hibernate.annotations.JavaType javaType = attributeMember.locateAnnotationUsage( org.hibernate.annotations.JavaType.class, getSourceModelContext() );
			if ( javaType != null ) {
				final Class<? extends BasicJavaType<?>> javaTypeClass = normalizeJavaType( javaType.value() );
				if ( javaTypeClass != null ) {
					if ( !buildingContext.getBuildingOptions().isAllowExtensionsInCdi() ) {
						return FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( javaTypeClass );
					}
					return getManagedBeanRegistry().getBean( javaTypeClass ).getBeanInstance();
				}
			}

			//noinspection deprecation
			final var targetAnn = attributeMember.locateAnnotationUsage( Target.class, getSourceModelContext() );
			if ( targetAnn != null ) {
				//noinspection deprecation
				DEPRECATION_LOGGER.deprecatedAnnotation( Target.class, attributeMember.getName() );
				return (BasicJavaType<?>) typeConfiguration.getJavaTypeRegistry().getDescriptor( targetAnn.value() );
			}

			return null;
		};

		final JdbcTypeCode jdbcType = attributeMember.locateAnnotationUsage( JdbcTypeCode.class, getSourceModelContext() );
		if ( jdbcType != null ) {
			jdbcTypeCode = jdbcType.value();
		}

		normalJdbcTypeDetails( attributeMember);
		normalMutabilityDetails( attributeMember );

		final Enumerated enumerated = attributeMember.getDirectAnnotationUsage( Enumerated.class );
		if ( enumerated != null ) {
			enumType = enumerated.value();
		}

		final Temporal temporal = attributeMember.getDirectAnnotationUsage( Temporal.class );
		if ( temporal != null ) {
			temporalPrecision = temporal.value();
		}

		final TimeZoneStorage timeZoneStorage = attributeMember.getDirectAnnotationUsage( TimeZoneStorage.class );
		if ( timeZoneStorage != null ) {
			timeZoneStorageType = timeZoneStorage.value();
			final TimeZoneColumn timeZoneColumnAnn = attributeMember.getDirectAnnotationUsage( TimeZoneColumn.class );
			if ( timeZoneColumnAnn != null ) {
				if ( timeZoneStorageType != TimeZoneStorageType.AUTO && timeZoneStorageType != TimeZoneStorageType.COLUMN ) {
					throw new IllegalStateException(
							"@TimeZoneColumn can not be used in conjunction with @TimeZoneStorage( " + timeZoneStorageType +
									" ) with attribute " + attributeMember.getDeclaringType().getName() +
									'.' + attributeMember.getName()
					);
				}
			}
		}

		this.partitionKey = attributeMember.hasDirectAnnotationUsage( PartitionKey.class );
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

	@Override
	public Dialect getDialect() {
		return buildingContext.getMetadataCollector().getDatabase().getDialect();
	}

	private void applyJpaConverter(MemberDetails attributeMember, ConverterDescriptor attributeConverterDescriptor) {
		if ( attributeConverterDescriptor == null ) {
			return;
		}

		LOG.debugf( "Applying JPA converter [%s:%s]", persistentClassName, attributeMember.getName() );

		if ( attributeMember.hasDirectAnnotationUsage( Id.class ) ) {
			LOG.debugf( "Skipping AttributeConverter checks for Id attribute [%s]", attributeMember.getName() );
			return;
		}

		if ( attributeMember.hasDirectAnnotationUsage( Version.class ) ) {
			LOG.debugf( "Skipping AttributeConverter checks for version attribute [%s]", attributeMember.getName() );
			return;
		}

		if ( kind == Kind.MAP_KEY ) {
			if ( attributeMember.hasDirectAnnotationUsage( MapKeyTemporal.class ) ) {
				LOG.debugf( "Skipping AttributeConverter checks for map-key annotated as MapKeyTemporal [%s]", attributeMember.getName() );
				return;
			}

			if ( attributeMember.hasDirectAnnotationUsage( MapKeyEnumerated.class ) ) {
				LOG.debugf( "Skipping AttributeConverter checks for map-key annotated as MapKeyEnumerated [%s]", attributeMember.getName() );
				return;
			}
		}
		else {
			if ( attributeMember.hasDirectAnnotationUsage( Temporal.class ) ) {
				LOG.debugf( "Skipping AttributeConverter checks for Temporal attribute [%s]", attributeMember.getName() );
				return;
			}

			if ( attributeMember.hasDirectAnnotationUsage( Enumerated.class ) ) {
				LOG.debugf( "Skipping AttributeConverter checks for Enumerated attribute [%s]", attributeMember.getName() );
				return;
			}
		}

		if ( isAssociation() ) {
			LOG.debugf( "Skipping AttributeConverter checks for association attribute [%s]", attributeMember.getName() );
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
		Class<? extends UserType<?>> customType(MemberDetails attributeMember, SourceModelBuildingContext sourceModelContext);
		Map<String,String> customTypeParameters(MemberDetails attributeMember, SourceModelBuildingContext sourceModelContext);
	}

	private static class ValueMappingAccess implements BasicMappingAccess {
		public static final ValueMappingAccess INSTANCE = new ValueMappingAccess();

		@Override
		public Class<? extends UserType<?>> customType(MemberDetails attributeMember, SourceModelBuildingContext sourceModelContext) {
			final Type customType = attributeMember.locateAnnotationUsage( Type.class, sourceModelContext );
			if ( customType == null ) {
				return null;
			}

			return normalizeUserType( customType.value() );
		}

		@Override
		public Map<String,String> customTypeParameters(MemberDetails attributeMember, SourceModelBuildingContext sourceModelContext) {
			final Type customType = attributeMember.locateAnnotationUsage( Type.class, sourceModelContext );
			if ( customType == null ) {
				return null;
			}

			return AnnotationHelper.extractParameterMap( customType.parameters() );
		}
	}

	private static class AnyDiscriminatorMappingAccess implements BasicMappingAccess {
		public static final AnyDiscriminatorMappingAccess INSTANCE = new AnyDiscriminatorMappingAccess();

		@Override
		public Class<? extends UserType<?>> customType(MemberDetails attributeMember, SourceModelBuildingContext sourceModelContext) {
			return null;
		}

		@Override
		public Map<String,String> customTypeParameters(MemberDetails attributeMember, SourceModelBuildingContext sourceModelContext) {
			return Collections.emptyMap();
		}
	}

	private static class AnyKeyMappingAccess implements BasicMappingAccess {
		public static final AnyKeyMappingAccess INSTANCE = new AnyKeyMappingAccess();

		@Override
		public Class<? extends UserType<?>> customType(MemberDetails attributeMember, SourceModelBuildingContext sourceModelContext) {
			return null;
		}

		@Override
		public Map<String,String> customTypeParameters(MemberDetails attributeMember, SourceModelBuildingContext sourceModelContext) {
			return Collections.emptyMap();
		}
	}

	private static class MapKeyMappingAccess implements BasicMappingAccess {
		public static final MapKeyMappingAccess INSTANCE = new MapKeyMappingAccess();

		@Override
		public Class<? extends UserType<?>> customType(MemberDetails attributeMember, SourceModelBuildingContext sourceModelContext) {
			final MapKeyType customType = attributeMember.locateAnnotationUsage( MapKeyType.class, sourceModelContext );
			if ( customType == null ) {
				return null;
			}

			return normalizeUserType( customType.value() );
		}

		@Override
		public Map<String,String> customTypeParameters(MemberDetails attributeMember, SourceModelBuildingContext sourceModelContext) {
			final MapKeyType customType = attributeMember.locateAnnotationUsage( MapKeyType.class, sourceModelContext );
			if ( customType == null ) {
				return null;
			}

			return AnnotationHelper.extractParameterMap( customType.parameters() );
		}
	}

	private static class CollectionIdMappingAccess implements BasicMappingAccess {
		public static final CollectionIdMappingAccess INSTANCE = new CollectionIdMappingAccess();

		@Override
		public Class<? extends UserType<?>> customType(MemberDetails attributeMember, SourceModelBuildingContext sourceModelBuildingContext) {
			final CollectionIdType customType = attributeMember.locateAnnotationUsage( CollectionIdType.class, sourceModelBuildingContext );
			if ( customType == null ) {
				return null;
			}

			return normalizeUserType( customType.value() );
		}

		@Override
		public Map<String,String> customTypeParameters(MemberDetails attributeMember, SourceModelBuildingContext sourceModelContext) {
			final CollectionIdType customType = attributeMember.locateAnnotationUsage( CollectionIdType.class, sourceModelContext );
			if ( customType == null ) {
				return null;
			}

			return AnnotationHelper.extractParameterMap( customType.parameters() );
		}
	}

	private static class ListIndexMappingAccess implements BasicMappingAccess {
		public static final ListIndexMappingAccess INSTANCE = new ListIndexMappingAccess();

		@Override
		public Class<? extends UserType<?>> customType(MemberDetails attributeMember, SourceModelBuildingContext sourceModelContext) {
			return null;
		}

		@Override
		public Map<String,String> customTypeParameters(MemberDetails attributeMember, SourceModelBuildingContext sourceModelContext) {
			return Collections.emptyMap();
		}
	}
}
