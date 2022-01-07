/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.annotations;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.TimeZoneStorageStrategy;
import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.annotations.AnyKeyJavaType;
import org.hibernate.annotations.AnyKeyJdbcType;
import org.hibernate.annotations.AnyKeyJdbcTypeCode;
import org.hibernate.annotations.CollectionId;
import org.hibernate.annotations.CollectionIdCustomType;
import org.hibernate.annotations.CollectionIdJavaType;
import org.hibernate.annotations.CollectionIdJdbcType;
import org.hibernate.annotations.CollectionIdJdbcTypeCode;
import org.hibernate.annotations.CollectionIdMutability;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.ListIndexJavaType;
import org.hibernate.annotations.ListIndexJdbcType;
import org.hibernate.annotations.ListIndexJdbcTypeCode;
import org.hibernate.annotations.MapKeyCustomType;
import org.hibernate.annotations.MapKeyJavaType;
import org.hibernate.annotations.MapKeyJdbcType;
import org.hibernate.annotations.MapKeyJdbcTypeCode;
import org.hibernate.annotations.MapKeyMutability;
import org.hibernate.annotations.Mutability;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Target;
import org.hibernate.annotations.TimeZoneColumn;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.annotations.TimeZoneType;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.model.TypeDefinition;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.AccessType;
import org.hibernate.cfg.AnnotatedColumn;
import org.hibernate.cfg.AnnotatedJoinColumn;
import org.hibernate.cfg.PkDrivenByDefaultMapsIdSecondPass;
import org.hibernate.cfg.SetBasicValueTypeSecondPass;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.NationalizationSupport;
import org.hibernate.dialect.TimeZoneSupport;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Table;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.type.BasicType;
import org.hibernate.type.SerializableToBlobType;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptorIndicators;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.DynamicParameterizedType;
import org.hibernate.usertype.UserType;

import org.jboss.logging.Logger;

import jakarta.persistence.Column;
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

import static org.hibernate.cfg.annotations.HCANNHelper.findAnnotation;

/**
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 */
public class BasicValueBinder<T> implements JdbcTypeDescriptorIndicators {

	// todo (6.0) : In light of how we want to build Types (specifically BasicTypes) moving forward this class should undergo major changes
	//		see the comments in #setType
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
	private final MetadataBuildingContext buildingContext;

	private final ClassLoaderService classLoaderService;
	private final StrategySelector strategySelector;



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// in-flight info

	private String explicitBasicTypeName;
	private Class<? extends UserType<?>> explicitCustomType;
	private Map explicitLocalTypeParams;

	private Function<TypeConfiguration, JdbcType> explicitJdbcTypeAccess;
	private Function<TypeConfiguration, BasicJavaType> explicitJavaTypeAccess;
	private Function<TypeConfiguration, MutabilityPlan> explicitMutabilityAccess;
	private Function<TypeConfiguration, java.lang.reflect.Type> implicitJavaTypeAccess;

	private XProperty xproperty;
	private AccessType accessType;

	private ConverterDescriptor converterDescriptor;

	private boolean isVersion;
	private boolean isNationalized;
	private boolean isLob;
	private EnumType enumType;
	private TemporalType temporalPrecision;
	private TimeZoneStorageType timeZoneStorageType;

	private Table table;
	private AnnotatedColumn[] columns;

	private BasicValue basicValue;

	private String timeStampVersionType;
	private String persistentClassName;
	private String propertyName;
	private String returnedClassName;
	private String referencedEntityName;


	public BasicValueBinder(Kind kind, MetadataBuildingContext buildingContext) {
		assert kind != null;
		assert  buildingContext != null;

		this.kind = kind;
		this.buildingContext = buildingContext;

		this.classLoaderService = buildingContext.getBootstrapContext()
				.getServiceRegistry()
				.getService( ClassLoaderService.class );

		this.strategySelector = buildingContext.getBootstrapContext()
				.getServiceRegistry()
				.getService( StrategySelector.class );
	}


	@Override
	public TypeConfiguration getTypeConfiguration() {
		return buildingContext.getBootstrapContext().getTypeConfiguration();
	}
	@Override
	public TimeZoneStorageStrategy getDefaultTimeZoneStorageStrategy() {
		if ( timeZoneStorageType != null ) {
			switch ( timeZoneStorageType ) {
				case COLUMN:
					return TimeZoneStorageStrategy.COLUMN;
				case NATIVE:
					return TimeZoneStorageStrategy.NATIVE;
				case NORMALIZE:
					return TimeZoneStorageStrategy.NORMALIZE;
			}
		}
		return buildingContext.getBuildingOptions().getDefaultTimeZoneStorage();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqlTypeDescriptorIndicators

	@Override
	public EnumType getEnumeratedType() {
		return enumType;
	}

	@Override
	public boolean isLob() {
		return isLob;
	}

	@Override
	public TemporalType getTemporalPrecision() {
		return temporalPrecision;
	}

	@Override
	public int getPreferredSqlTypeCodeForBoolean() {
		return buildingContext.getPreferredSqlTypeCodeForBoolean();
	}

	@Override
	public boolean isNationalized() {
		return isNationalized;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// in-flight handling

	public void setVersion(boolean isVersion) {
		this.isVersion = isVersion;
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

	public void setColumns(AnnotatedColumn[] columns) {
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

		if ( columns.length != 1 ) {
			throw new AssertionFailure( "Expecting just one column, but found `" + Arrays.toString( columns ) + "`" );
		}

		final XClass modelTypeXClass = isArray
				? modelXProperty.getElementClass()
				: modelPropertyTypeXClass;

		// If we get into this method we know that there is a Java type for the value
		//		and that it is safe to load on the app classloader.
		final Class modelJavaType = resolveJavaType( modelTypeXClass, buildingContext );
		if ( modelJavaType == null ) {
			throw new IllegalStateException( "BasicType requires Java type" );
		}

		final Class modelPropertyJavaType = buildingContext.getBootstrapContext()
				.getReflectionManager()
				.toClass( modelXProperty.getType() );

		final boolean isMap = Map.class.isAssignableFrom( modelPropertyJavaType );

		if ( kind != Kind.LIST_INDEX && kind != Kind.MAP_KEY  ) {
			isLob = modelXProperty.isAnnotationPresent( Lob.class );
		}

		if ( getDialect().getNationalizationSupport() == NationalizationSupport.EXPLICIT ) {
			isNationalized = modelXProperty.isAnnotationPresent( Nationalized.class )
					|| buildingContext.getBuildingOptions().useNationalizedCharacterData();
		}

		applyJpaConverter( modelXProperty, converterDescriptor );

		final Class<? extends UserType> userTypeImpl = kind.mappingAccess.customType( modelXProperty );
		if ( userTypeImpl != null ) {
			applyExplicitType( userTypeImpl, kind.mappingAccess.customTypeParameters( modelXProperty ) );

			// An explicit custom UserType has top precedence when we get to BasicValue resolution.
			return;
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

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void applyExplicitType(Class<? extends UserType> impl, Parameter[] params) {
		this.explicitCustomType = (Class) impl;
		this.explicitLocalTypeParams = extractTypeParams( params );
	}

	@SuppressWarnings("unchecked")
	private Map extractTypeParams(Parameter[] parameters) {
		if ( parameters == null || parameters.length == 0 ) {
			return Collections.emptyMap();
		}

		if ( parameters.length == 1 ) {
			return Collections.singletonMap( parameters[0].name(), parameters[0].value() );
		}

		final Map map = new HashMap();
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

		final ManagedBeanRegistry beanRegistry = buildingContext.getBootstrapContext()
				.getServiceRegistry()
				.getService( ManagedBeanRegistry.class );

		explicitBasicTypeName = null;
		implicitJavaTypeAccess = (typeConfiguration) -> null;

		explicitJavaTypeAccess = (typeConfiguration) -> {
			final CollectionIdJavaType javaTypeAnn = findAnnotation( modelXProperty, CollectionIdJavaType.class );
			if ( javaTypeAnn != null ) {
				final Class<? extends BasicJavaType<?>> javaType = normalizeJavaType( javaTypeAnn.value() );
				if ( javaType != null ) {
					final ManagedBean<? extends BasicJavaType<?>> bean = beanRegistry.getBean( javaType );
					return bean.getBeanInstance();
				}
			}

			return null;
		};

		explicitJdbcTypeAccess = (typeConfiguration) -> {
			final CollectionIdJdbcType jdbcTypeAnn = findAnnotation( modelXProperty, CollectionIdJdbcType.class );
			if ( jdbcTypeAnn != null ) {
				final Class<? extends JdbcType> jdbcType = normalizeJdbcType( jdbcTypeAnn.value() );
				if ( jdbcType != null ) {
					final ManagedBean<? extends JdbcType> managedBean = beanRegistry.getBean( jdbcType );
					return managedBean.getBeanInstance();
				}
			}

			final CollectionIdJdbcTypeCode jdbcTypeCodeAnn = findAnnotation( modelXProperty, CollectionIdJdbcTypeCode.class );
			if ( jdbcTypeCodeAnn != null ) {
				if ( jdbcTypeCodeAnn.value() != Integer.MIN_VALUE ) {
					return typeConfiguration.getJdbcTypeDescriptorRegistry().getDescriptor( jdbcTypeCodeAnn.value() );
				}
			}

			return null;
		};

		explicitMutabilityAccess = (typeConfiguration) -> {
			final CollectionIdMutability mutabilityAnn = findAnnotation( modelXProperty, CollectionIdMutability.class );
			if ( mutabilityAnn != null ) {
				final Class<? extends MutabilityPlan<?>> mutability = normalizeMutability( mutabilityAnn.value() );
				if ( mutability != null ) {
					final ManagedBean<? extends MutabilityPlan<?>> jtdBean = beanRegistry.getBean( mutability );
					return jtdBean.getBeanInstance();
				}
			}

			// see if the value's type Class is annotated `@Immutable`
			if ( implicitJavaTypeAccess != null ) {
				final Class<?> attributeType = ReflectHelper.getClass( implicitJavaTypeAccess.apply( typeConfiguration ) );
				if ( attributeType != null ) {
					if ( attributeType.isAnnotationPresent( Immutable.class ) ) {
						return ImmutableMutabilityPlan.instance();
					}
				}
			}

			// if there is a converter, check it for mutability-related annotations
			if ( converterDescriptor != null ) {
				final Mutability converterMutabilityAnn = converterDescriptor.getAttributeConverterClass().getAnnotation( Mutability.class );
				if ( converterMutabilityAnn != null ) {
					final ManagedBean<? extends MutabilityPlan<?>> jtdBean = beanRegistry.getBean( converterMutabilityAnn.value() );
					return jtdBean.getBeanInstance();
				}

				if ( converterDescriptor.getAttributeConverterClass().isAnnotationPresent( Immutable.class ) ) {
					return ImmutableMutabilityPlan.instance();
				}
			}

			final Class<? extends UserType> customTypeImpl = Kind.ATTRIBUTE.mappingAccess.customType( modelXProperty );
			if ( customTypeImpl.isAnnotationPresent( Immutable.class ) ) {
				return ImmutableMutabilityPlan.instance();
			}

			// generally, this will trigger usage of the `JavaTypeDescriptor#getMutabilityPlan`
			return null;
		};

		// todo (6.0) - handle generator
		final String generator = collectionIdAnn.generator();
	}

	private void prepareMapKey(
			XProperty mapAttribute,
			XClass modelPropertyTypeXClass) {
		final XClass mapKeyClass;
		if ( modelPropertyTypeXClass == null ) {
			mapKeyClass = mapAttribute.getMapKey();
		}
		else {
			mapKeyClass = modelPropertyTypeXClass;
		}
		final Class<?> implicitJavaType = buildingContext.getBootstrapContext()
				.getReflectionManager()
				.toClass( mapKeyClass );

		implicitJavaTypeAccess = (typeConfiguration) -> implicitJavaType;

		final MapKeyEnumerated mapKeyEnumeratedAnn = mapAttribute.getAnnotation( MapKeyEnumerated.class );
		if ( mapKeyEnumeratedAnn != null ) {
			enumType = mapKeyEnumeratedAnn.value();
		}

		final MapKeyTemporal mapKeyTemporalAnn = mapAttribute.getAnnotation( MapKeyTemporal.class );
		if ( mapKeyTemporalAnn != null ) {
			temporalPrecision = mapKeyTemporalAnn.value();
		}

		final ManagedBeanRegistry managedBeanRegistry = buildingContext.getBootstrapContext()
				.getServiceRegistry()
				.getService( ManagedBeanRegistry.class );

		explicitJdbcTypeAccess = typeConfiguration -> {
			final MapKeyJdbcType jdbcTypeAnn = findAnnotation( mapAttribute, MapKeyJdbcType.class );
			if ( jdbcTypeAnn != null ) {
				final Class<? extends JdbcType> jdbcTypeImpl = normalizeJdbcType( jdbcTypeAnn.value() );
				if ( jdbcTypeImpl != null ) {
					final ManagedBean<? extends JdbcType> jdbcTypeBean = managedBeanRegistry.getBean( jdbcTypeImpl );
					return jdbcTypeBean.getBeanInstance();
				}
			}

			final MapKeyJdbcTypeCode jdbcTypeCodeAnn = findAnnotation( mapAttribute, MapKeyJdbcTypeCode.class );
			if ( jdbcTypeCodeAnn != null ) {
				final int jdbcTypeCode = jdbcTypeCodeAnn.value();
				if ( jdbcTypeCode != Integer.MIN_VALUE ) {
					return typeConfiguration.getJdbcTypeDescriptorRegistry().getDescriptor( jdbcTypeCode );
				}
			}

			return null;
		};

		explicitJavaTypeAccess = typeConfiguration -> {
			final MapKeyJavaType javaTypeAnn = findAnnotation( mapAttribute, MapKeyJavaType.class );
			if ( javaTypeAnn != null ) {
				final Class<? extends BasicJavaType<?>> jdbcTypeImpl = normalizeJavaType( javaTypeAnn.value() );
				if ( jdbcTypeImpl != null ) {
					final ManagedBean<? extends BasicJavaType> jdbcTypeBean = managedBeanRegistry.getBean( jdbcTypeImpl );
					return jdbcTypeBean.getBeanInstance();
				}
			}

			final MapKeyClass mapKeyClassAnn = mapAttribute.getAnnotation( MapKeyClass.class );
			if ( mapKeyClassAnn != null ) {
				return (BasicJavaType) typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( mapKeyClassAnn.value() );
			}

			return null;
		};

		explicitMutabilityAccess = typeConfiguration -> {
			final MapKeyMutability mutabilityAnn = findAnnotation( mapAttribute, MapKeyMutability.class );
			if ( mutabilityAnn != null ) {
				final Class<? extends MutabilityPlan<?>> mutability = normalizeMutability( mutabilityAnn.value() );
				if ( mutability != null ) {
					final ManagedBean<? extends MutabilityPlan<?>> jtdBean = managedBeanRegistry.getBean( mutability );
					return jtdBean.getBeanInstance();
				}
			}

			// see if the value's type Class is annotated `@Immutable`
			if ( implicitJavaTypeAccess != null ) {
				final Class<?> attributeType = ReflectHelper.getClass( implicitJavaTypeAccess.apply( typeConfiguration ) );
				if ( attributeType != null ) {
					if ( attributeType.isAnnotationPresent( Immutable.class ) ) {
						return ImmutableMutabilityPlan.instance();
					}
				}
			}

			// if the value is converted, see if the converter Class is annotated `@Immutable`
			if ( converterDescriptor != null ) {
				final Mutability converterMutabilityAnn = converterDescriptor.getAttributeConverterClass().getAnnotation( Mutability.class );
				if ( converterMutabilityAnn != null ) {
					final ManagedBean<? extends MutabilityPlan<?>> jtdBean = managedBeanRegistry.getBean( converterMutabilityAnn.value() );
					return jtdBean.getBeanInstance();
				}

				if ( converterDescriptor.getAttributeConverterClass().isAnnotationPresent( Immutable.class ) ) {
					return ImmutableMutabilityPlan.instance();
				}
			}

			final Class<? extends UserType> customTypeImpl = Kind.MAP_KEY.mappingAccess.customType( mapAttribute );
			if ( customTypeImpl != null ) {
				if ( customTypeImpl.isAnnotationPresent( Immutable.class ) ) {
					return ImmutableMutabilityPlan.instance();
				}
			}

			// generally, this will trigger usage of the `JavaTypeDescriptor#getMutabilityPlan`
			return null;
		};
	}

	private void prepareListIndex(XProperty listAttribute) {
		implicitJavaTypeAccess = typeConfiguration -> Integer.class;

		final ManagedBeanRegistry beanRegistry = buildingContext
				.getBootstrapContext()
				.getServiceRegistry()
				.getService( ManagedBeanRegistry.class );

		explicitJavaTypeAccess = (typeConfiguration) -> {
			final ListIndexJavaType javaTypeAnn = findAnnotation( listAttribute, ListIndexJavaType.class );
			if ( javaTypeAnn != null ) {
				final Class<? extends BasicJavaType<?>> javaType = normalizeJavaType( javaTypeAnn.value() );
				if ( javaType != null ) {
					final ManagedBean<? extends BasicJavaType<?>> bean = beanRegistry.getBean( javaType );
					return bean.getBeanInstance();
				}
			}

			return null;
		};

		explicitJdbcTypeAccess = (typeConfiguration) -> {
			final ListIndexJdbcType jdbcTypeAnn = findAnnotation( listAttribute, ListIndexJdbcType.class );
			if ( jdbcTypeAnn != null ) {
				final Class<? extends JdbcType> jdbcType = normalizeJdbcType( jdbcTypeAnn.value() );
				if ( jdbcType != null ) {
					final ManagedBean<? extends JdbcType> bean = beanRegistry.getBean( jdbcType );
					return bean.getBeanInstance();
				}
			}

			final ListIndexJdbcTypeCode jdbcTypeCodeAnn = findAnnotation( listAttribute, ListIndexJdbcTypeCode.class );
			if ( jdbcTypeCodeAnn != null ) {
				return typeConfiguration.getJdbcTypeDescriptorRegistry().getDescriptor( jdbcTypeCodeAnn.value() );
			}

			return null;
		};
	}

	private void prepareCollectionElement(XProperty attributeXProperty, XClass elementTypeXClass) {

		Class<T> javaType;
		//noinspection unchecked
		if ( elementTypeXClass == null && attributeXProperty.isArray() ) {
			javaType = buildingContext.getBootstrapContext()
					.getReflectionManager()
					.toClass( attributeXProperty.getElementClass() );
		}
		else {
			javaType = buildingContext.getBootstrapContext()
					.getReflectionManager()
					.toClass( elementTypeXClass );
		}

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

		if ( javaType.isEnum() ) {
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

		final TimeZoneStorage timeZoneStorageAnn = attributeXProperty.getAnnotation( TimeZoneStorage.class );
		if ( timeZoneStorageAnn != null ) {
			timeZoneStorageType = timeZoneStorageAnn.value();
			final TimeZoneColumn timeZoneColumnAnn = attributeXProperty.getAnnotation( TimeZoneColumn.class );
			final Column column;
			final TimeZoneType type;
			if ( timeZoneColumnAnn != null ) {
				column = timeZoneColumnAnn.column();
				type = timeZoneColumnAnn.type();
			}
			else {
				switch ( timeZoneStorageType ) {
					case AUTO:
						final Dialect dialect = buildingContext.getBootstrapContext().getServiceRegistry()
								.getService( JdbcServices.class ).getDialect();
						if ( dialect.getTimeZoneSupport() == TimeZoneSupport.NATIVE ) {
							column = null;
							type = null;
							break;
						}
					case COLUMN:
						final String timeZoneColumnName = columns[0].getName() + "_tz";
						column = new Column() {
							@Override
							public String name() {
								return timeZoneColumnName;
							}

							@Override
							public boolean unique() {
								return false;
							}

							@Override
							public boolean nullable() {
								return columns[0].isNullable();
							}

							@Override
							public boolean insertable() {
								return columns[0].isInsertable();
							}

							@Override
							public boolean updatable() {
								return columns[0].isUpdatable();
							}

							@Override
							public String columnDefinition() {
								return "";
							}

							@Override
							public String table() {
								return columns[0].getExplicitTableName();
							}

							@Override
							public int length() {
								return 255;
							}

							@Override
							public int precision() {
								return 0;
							}

							@Override
							public int scale() {
								return 0;
							}

							@Override
							public Class<? extends Annotation> annotationType() {
								return Column.class;
							}
						};
						type = TimeZoneType.OFFSET;
						break;
					default:
						column = null;
						type = null;
						break;
				}
			}
			if ( column != null ) {
				// todo (6.0): do something with the column
				//  maybe move this to AnnotationBinder#2266 and make it treat the property as composite for the COLUMN strategy
				throw new NotYetImplementedFor6Exception("TimeZoneColumn support is not yet implemented!");
			}
			else if ( timeZoneColumnAnn != null ) {
				throw new IllegalStateException(
						"@TimeZoneColumn can not be used in conjunction with @TimeZoneStorage( " + timeZoneStorageType +
								" ) with attribute " + attributeXProperty.getDeclaringClass().getName() +
								'.' + attributeXProperty.getName()
				);
			}
		}
		else {
			timeZoneStorageType = null;
		}

		normalSupplementalDetails( attributeXProperty, buildingContext );

		// layer in support for JPA's approach for specifying a specific Java type for the collection elements...
		final ElementCollection elementCollectionAnn = attributeXProperty.getAnnotation( ElementCollection.class );
		if ( elementCollectionAnn != null
				&& elementCollectionAnn.targetClass() != null
				&& elementCollectionAnn.targetClass() != void.class ) {
			final Function<TypeConfiguration, BasicJavaType> original = explicitJavaTypeAccess;
			explicitJavaTypeAccess = (typeConfiguration) -> {
				final BasicJavaType originalResult = original.apply( typeConfiguration );
				if ( originalResult != null ) {
					return originalResult;
				}

				return (BasicJavaType) typeConfiguration
						.getJavaTypeDescriptorRegistry()
						.getDescriptor( elementCollectionAnn.targetClass() );
			};
		}
	}

	@SuppressWarnings("unchecked")
	private void prepareBasicAttribute(String declaringClassName, XProperty attributeDescriptor, XClass attributeType) {
		final Class<T> javaType = buildingContext.getBootstrapContext()
				.getReflectionManager()
				.toClass( attributeType );

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

		if ( javaType.isEnum() ) {
			final Enumerated enumeratedAnn = attributeDescriptor.getAnnotation( Enumerated.class );
			if ( enumeratedAnn != null ) {
				this.enumType = enumeratedAnn.value();
				if ( this.enumType == null ) {
					throw new IllegalStateException(
							"jakarta.persistence.EnumType was null on @jakarta.persistence.Enumerated " +
									" associated with attribute " + attributeDescriptor.getDeclaringClass().getName() +
									'.' + attributeDescriptor.getName()
					);
				}
			}
		}
		else {
			if ( attributeDescriptor.isAnnotationPresent( Enumerated.class ) ) {
				throw new AnnotationException(
						String.format(
								"Attribute [%s.%s] was annotated as enumerated, but its java type is not an enum [%s]",
								declaringClassName,
								attributeDescriptor.getName(),
								attributeType.getName()
						)
				);
			}
			this.enumType = null;
		}

		normalSupplementalDetails( attributeDescriptor, buildingContext );
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

		normalJdbcTypeDetails( modelXProperty, buildingContext );
		normalMutabilityDetails( modelXProperty, buildingContext );

		// layer AnyDiscriminator into the JdbcType resolution
		final Function<TypeConfiguration, JdbcType> originalJdbcTypeResolution = explicitJdbcTypeAccess;
		this.explicitJdbcTypeAccess = (typeConfiguration) -> {
			final JdbcType originalResolution = originalJdbcTypeResolution.apply( typeConfiguration );
			if ( originalResolution != null ) {
				return originalResolution;
			}

			final Class<?> hintedJavaType = (Class<?>) implicitJavaTypeAccess.apply( typeConfiguration );
			final JavaType<Object> hintedDescriptor = typeConfiguration
					.getJavaTypeDescriptorRegistry()
					.getDescriptor( hintedJavaType );
			return hintedDescriptor.getRecommendedJdbcType( typeConfiguration.getCurrentBaseSqlTypeIndicators() );
		};
	}

	private void prepareAnyKey(XProperty modelXProperty) {
		implicitJavaTypeAccess = (typeConfiguration) -> null;

		final ManagedBeanRegistry managedBeanRegistry = buildingContext.getBootstrapContext()
				.getServiceRegistry()
				.getService( ManagedBeanRegistry.class );

		explicitJavaTypeAccess = (typeConfiguration) -> {
			final AnyKeyJavaType javaTypeAnn = findAnnotation( modelXProperty, AnyKeyJavaType.class );
			if ( javaTypeAnn != null ) {
				final Class<? extends BasicJavaType<?>> javaType = normalizeJavaType( javaTypeAnn.value() );

				if ( javaType != null ) {
					final ManagedBean<? extends BasicJavaType<?>> jtdBean = managedBeanRegistry.getBean( javaType );
					return jtdBean.getBeanInstance();
				}
			}

			final AnyKeyJavaClass javaClassAnn = findAnnotation( modelXProperty, AnyKeyJavaClass.class );
			if ( javaClassAnn != null ) {
				//noinspection rawtypes
				return (BasicJavaType) typeConfiguration
						.getJavaTypeDescriptorRegistry()
						.getDescriptor( javaClassAnn.value() );
			}

			return null;
		};

		explicitJdbcTypeAccess = (typeConfiguration) -> {
			final AnyKeyJdbcType jdbcTypeAnn = findAnnotation( modelXProperty, AnyKeyJdbcType.class );
			if ( jdbcTypeAnn != null ) {
				final Class<? extends JdbcType> jdbcType = normalizeJdbcType( jdbcTypeAnn.value() );
				if ( jdbcType != null ) {
					final ManagedBean<? extends JdbcType> jtdBean = managedBeanRegistry.getBean( jdbcType );
					return jtdBean.getBeanInstance();
				}
			}

			final AnyKeyJdbcTypeCode jdbcTypeCodeAnn = findAnnotation( modelXProperty, AnyKeyJdbcTypeCode.class );
			if ( jdbcTypeCodeAnn != null ) {
				if ( jdbcTypeCodeAnn.value() != Integer.MIN_VALUE ) {
					return typeConfiguration.getJdbcTypeDescriptorRegistry().getDescriptor( jdbcTypeCodeAnn.value() );
				}
			}

			return null;
		};
	}

	private void normalJdbcTypeDetails(
			XProperty attributeXProperty,
			MetadataBuildingContext buildingContext) {
		explicitJdbcTypeAccess = (typeConfiguration) -> {
			final ManagedBeanRegistry managedBeanRegistry = buildingContext.getBootstrapContext()
					.getServiceRegistry()
					.getService( ManagedBeanRegistry.class );

			final org.hibernate.annotations.JdbcType jdbcTypeAnn = findAnnotation( attributeXProperty, org.hibernate.annotations.JdbcType.class );
			if ( jdbcTypeAnn != null ) {
				final Class<? extends JdbcType> jdbcType = normalizeJdbcType( jdbcTypeAnn.value() );
				if ( jdbcType != null ) {
					final ManagedBean<? extends JdbcType> jdbcTypeBean = managedBeanRegistry.getBean( jdbcType );
					return jdbcTypeBean.getBeanInstance();
				}
			}

			final JdbcTypeCode jdbcTypeCodeAnn = findAnnotation( attributeXProperty, JdbcTypeCode.class );
			if ( jdbcTypeCodeAnn != null ) {
				final int jdbcTypeCode = jdbcTypeCodeAnn.value();
				if ( jdbcTypeCode != Integer.MIN_VALUE ) {
					return typeConfiguration.getJdbcTypeDescriptorRegistry().getDescriptor( jdbcTypeCode );
				}
			}

			return null;
		};
	}

	private void normalMutabilityDetails(
			XProperty attributeXProperty,
			MetadataBuildingContext buildingContext) {
		final ManagedBeanRegistry managedBeanRegistry = buildingContext.getBootstrapContext()
				.getServiceRegistry()
				.getService( ManagedBeanRegistry.class );

		explicitMutabilityAccess = typeConfiguration -> {
			final Mutability mutabilityAnn = findAnnotation( attributeXProperty, Mutability.class );
			if ( mutabilityAnn != null ) {
				final Class<? extends MutabilityPlan<?>> mutability = normalizeMutability( mutabilityAnn.value() );
				if ( mutability != null ) {
					final ManagedBean<? extends MutabilityPlan<?>> jtdBean = managedBeanRegistry.getBean( mutability );
					return jtdBean.getBeanInstance();
				}
			}

			final Immutable immutableAnn = attributeXProperty.getAnnotation( Immutable.class );
			if ( immutableAnn != null ) {
				return ImmutableMutabilityPlan.instance();
			}

			// see if the value's type Class is annotated `@Immutable`
			if ( implicitJavaTypeAccess != null ) {
				final Class<?> attributeType = ReflectHelper.getClass( implicitJavaTypeAccess.apply( typeConfiguration ) );
				if ( attributeType != null ) {
					if ( attributeType.isAnnotationPresent( Immutable.class ) ) {
						return ImmutableMutabilityPlan.instance();
					}
				}
			}

			// if the value is converted, see if the converter Class is annotated `@Immutable`
			if ( converterDescriptor != null ) {
				final Mutability converterMutabilityAnn = converterDescriptor.getAttributeConverterClass().getAnnotation( Mutability.class );
				if ( converterMutabilityAnn != null ) {
					final ManagedBean<? extends MutabilityPlan<?>> jtdBean = managedBeanRegistry.getBean( converterMutabilityAnn.value() );
					return jtdBean.getBeanInstance();
				}

				if ( converterDescriptor.getAttributeConverterClass().isAnnotationPresent( Immutable.class ) ) {
					return ImmutableMutabilityPlan.instance();
				}
			}

			final Class<? extends UserType> customTypeImpl = Kind.ATTRIBUTE.mappingAccess.customType( attributeXProperty );
			if ( customTypeImpl != null ) {
				if ( customTypeImpl.isAnnotationPresent( Immutable.class ) ) {
					return ImmutableMutabilityPlan.instance();
				}
			}

			// generally, this will trigger usage of the `JavaTypeDescriptor#getMutabilityPlan`
			return null;
		};
	}

	private void normalSupplementalDetails(
			XProperty attributeXProperty,
			MetadataBuildingContext buildingContext) {
		final ManagedBeanRegistry managedBeanRegistry = buildingContext.getBootstrapContext()
				.getServiceRegistry()
				.getService( ManagedBeanRegistry.class );

		explicitJavaTypeAccess = typeConfiguration -> {
			final org.hibernate.annotations.JavaType javaTypeAnn = findAnnotation( attributeXProperty, org.hibernate.annotations.JavaType.class );
			if ( javaTypeAnn != null ) {
				final Class<? extends BasicJavaType<?>> javaType = normalizeJavaType( javaTypeAnn.value() );

				if ( javaType != null ) {
					final ManagedBean<? extends BasicJavaType<?>> jtdBean = managedBeanRegistry.getBean( javaType );
					return jtdBean.getBeanInstance();
				}
			}

			final Target targetAnn = findAnnotation( attributeXProperty, Target.class );
			if ( targetAnn != null ) {
				return (BasicJavaType) typeConfiguration
						.getJavaTypeDescriptorRegistry()
						.getDescriptor( targetAnn.value() );
			}

			return null;
		};

		normalJdbcTypeDetails( attributeXProperty, buildingContext );
		normalMutabilityDetails( attributeXProperty, buildingContext );

		final Enumerated enumeratedAnn = attributeXProperty.getAnnotation( Enumerated.class );
		if ( enumeratedAnn != null ) {
			enumType = enumeratedAnn.value();
		}

		final Temporal temporalAnn = attributeXProperty.getAnnotation( Temporal.class );
		if ( temporalAnn != null ) {
			temporalPrecision = temporalAnn.value();
		}
	}

	private static Class<? extends UserType<?>> normalizeUserType(Class<? extends UserType<?>> userType) {
		if ( userType == null ) {
			return null;
		}

		return userType;
	}

	private Class<? extends JdbcType> normalizeJdbcType(Class<? extends JdbcType> jdbcType) {
		if ( jdbcType == null ) {
			return null;
		}

		return jdbcType;
	}

	private static Class<? extends BasicJavaType<?>> normalizeJavaType(Class<? extends BasicJavaType<?>> javaType) {
		if ( javaType == null ) {
			return null;
		}

		return javaType;
	}

	private Class<? extends MutabilityPlan<?>> normalizeMutability(Class<? extends MutabilityPlan<?>> mutability) {
		if ( mutability == null ) {
			return null;
		}

		return mutability;
	}

	private static Class resolveJavaType(XClass returnedClassOrElement, MetadataBuildingContext buildingContext) {
		return buildingContext.getBootstrapContext()
					.getReflectionManager()
					.toClass( returnedClassOrElement );
	}

	private Dialect getDialect() {
		return buildingContext.getBuildingOptions()
				.getServiceRegistry()
				.getService( JdbcServices.class )
				.getJdbcEnvironment()
				.getDialect();
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

	private void validate() {
		AnnotatedColumn.checkPropertyConsistency( columns, propertyName );
	}

	public BasicValue make() {
		if ( basicValue != null ) {
			return basicValue;
		}

		validate();

		LOG.debugf( "building BasicValue for %s", propertyName );

		if ( table == null ) {
			table = columns[0].getTable();
		}

		basicValue = new BasicValue( buildingContext, table );

		if ( isNationalized ) {
			basicValue.makeNationalized();
		}

		if ( isLob ) {
			basicValue.makeLob();
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
		if ( columns[0].isNameDeferred() && !buildingContext.getMetadataCollector().isInSecondPass() && referencedEntityName != null ) {
			buildingContext.getMetadataCollector().addSecondPass(
					new PkDrivenByDefaultMapsIdSecondPass(
							referencedEntityName, (AnnotatedJoinColumn[]) columns, basicValue
					)
			);
		}
		else {
			for ( AnnotatedColumn column : columns ) {
				column.linkWithValue( basicValue );
			}
		}
	}

	public void fillSimpleValue() {
		LOG.debugf( "Starting `BasicValueBinder#fillSimpleValue` for %s", propertyName );

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

		if ( ( explicitCustomType != null && DynamicParameterizedType.class.isAssignableFrom( explicitCustomType ) )
				|| ( typeClass != null && DynamicParameterizedType.class.isAssignableFrom( typeClass ) ) ) {
			final Map<String, Object> parameters = createDynamicParameterizedTypeParameters();
			basicValue.setTypeParameters( (Map) parameters );
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

		if ( isLob ) {
			basicValue.makeLob();
		}

		if ( isNationalized ) {
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
		Class<? extends UserType> customType(XProperty xProperty);
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

			return normalizeUserType( (Class) customType.value() );
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
			final MapKeyCustomType customType = findAnnotation( xProperty, MapKeyCustomType.class );
			if ( customType == null ) {
				return null;
			}

			return normalizeUserType( customType.value() );
		}

		@Override
		public Parameter[] customTypeParameters(XProperty xProperty) {
			final MapKeyCustomType customType = findAnnotation( xProperty, MapKeyCustomType.class );
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
			final CollectionIdCustomType customType = findAnnotation( xProperty, CollectionIdCustomType.class );
			if ( customType == null ) {
				return null;
			}

			return normalizeUserType( customType.value() );
		}

		@Override
		public Parameter[] customTypeParameters(XProperty xProperty) {
			final CollectionIdCustomType customType = findAnnotation( xProperty, CollectionIdCustomType.class );
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
