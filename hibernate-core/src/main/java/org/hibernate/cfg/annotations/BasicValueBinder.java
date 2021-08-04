/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.annotations;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.MapKeyEnumerated;
import javax.persistence.MapKeyTemporal;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.annotations.CollectionId;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JavaType;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.MapKeyJavaType;
import org.hibernate.annotations.MapKeyJdbcType;
import org.hibernate.annotations.MapKeyJdbcTypeCode;
import org.hibernate.annotations.MapKeyType;
import org.hibernate.annotations.Mutability;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.AccessType;
import org.hibernate.cfg.Ejb3Column;
import org.hibernate.cfg.Ejb3JoinColumn;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.cfg.PkDrivenByDefaultMapsIdSecondPass;
import org.hibernate.cfg.SetBasicValueTypeSecondPass;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.NationalizationSupport;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Table;
import org.hibernate.type.descriptor.java.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptorIndicators;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.DynamicParameterizedType;

import org.jboss.logging.Logger;

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
		ATTRIBUTE,
		COLLECTION_ID,
		COLLECTION_ELEMENT,
		LIST_INDEX,
		MAP_KEY
	}

	private final Kind kind;
	private final MetadataBuildingContext buildingContext;

	private final ClassLoaderService classLoaderService;
	private final StrategySelector strategySelector;



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// in-flight info

	private String explicitBasicTypeName;
	private Map explicitLocalTypeParams;

	private Function<TypeConfiguration, JdbcTypeDescriptor> explicitSqlTypeAccess;
	private Function<TypeConfiguration, BasicJavaDescriptor> explicitJtdAccess;
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

	private Table table;
	private Ejb3Column[] columns;

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

	public void setColumns(Ejb3Column[] columns) {
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
		assert columns.length == 1;

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

		final Type explicitTypeAnn;
		if ( kind == Kind.MAP_KEY ) {
			assert isMap;
			final MapKeyType mapKeyTypeAnn = modelXProperty.getAnnotation( MapKeyType.class );
			if ( mapKeyTypeAnn != null ) {
				explicitTypeAnn = mapKeyTypeAnn.value();
			}
			else {
				explicitTypeAnn = null;
			}
		}
		else if ( kind == Kind.COLLECTION_ID ) {
			final CollectionId collectionIdAnn = modelXProperty.getAnnotation( CollectionId.class );
			assert collectionIdAnn != null;

			explicitTypeAnn = collectionIdAnn.type();
		}
		else {
			explicitTypeAnn = modelXProperty.getAnnotation( Type.class );
		}

		if ( explicitTypeAnn != null ) {
			setExplicitType( explicitTypeAnn );
		}
		else {
			switch ( kind ) {
				case ATTRIBUTE: {
					prepareBasicAttribute( declaringClassName, modelXProperty, modelPropertyTypeXClass );
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

	}

	private void prepareCollectionId(XProperty modelXProperty) {
		final CollectionId collectionIdAnn = modelXProperty.getAnnotation( CollectionId.class );
		if ( collectionIdAnn == null ) {
			throw new MappingException( "idbag mapping missing @CollectionId" );
		}

		explicitBasicTypeName = collectionIdAnn.type().type();
		implicitJavaTypeAccess = typeConfiguration -> null;

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

		implicitJavaTypeAccess = typeConfiguration -> implicitJavaType;

		final MapKeyType mapKeyTypeAnn = mapAttribute.getAnnotation( MapKeyType.class );
		if ( mapKeyTypeAnn != null ) {
			// See BasicTypeProducerRegistry registrations in StandardBasicTypes
			//
			// todo (6.0) : need to move those to MetadataBuildingContext
			//		I like the idea too of having TypeDefs fold into that
			throw new NotYetImplementedException( "see comment at throw site" );
		}

		final MapKeyTemporal mapKeyTemporalAnn = mapAttribute.getAnnotation( MapKeyTemporal.class );
		if ( mapKeyTemporalAnn != null ) {
			temporalPrecision = mapKeyTemporalAnn.value();
		}
		else {
			temporalPrecision = null;
		}

		if ( implicitJavaType.isEnum() ) {
			final MapKeyEnumerated enumeratedAnn = mapAttribute.getAnnotation( MapKeyEnumerated.class );
			if ( enumeratedAnn != null ) {
				enumType = enumeratedAnn.value();
				//noinspection ConstantConditions
				if ( enumType == null ) {
					// should never happen, but to be safe
					throw new IllegalStateException(
							"javax.persistence.EnumType was null on @javax.persistence.MapKeyEnumerated " +
									" associated with attribute " + mapAttribute.getDeclaringClass().getName() +
									'.' + mapAttribute.getName()
					);
				}
			}
		}
		else {
			enumType = null;
		}

		mapKeySupplementalDetails( mapAttribute );
	}

	private void prepareListIndex(XProperty listAttribute) {
		implicitJavaTypeAccess = typeConfiguration -> Integer.class;
	}

	private void prepareCollectionElement(XProperty attributeXProperty, XClass elementTypeXClass) {

		// todo (6.0) : @SqlType / @SqlTypeDescriptor

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
						"No javax.persistence.TemporalType defined for @javax.persistence.Temporal " +
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
							"javax.persistence.EnumType was null on @javax.persistence.Enumerated " +
									" associated with attribute " + attributeXProperty.getDeclaringClass().getName() +
									'.' + attributeXProperty.getName()
					);
				}
			}
		}
		else {
			enumType = null;
		}

		normalSupplementalDetails( attributeXProperty, buildingContext );
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
						"No javax.persistence.TemporalType defined for @javax.persistence.Temporal " +
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
							"javax.persistence.EnumType was null on @javax.persistence.Enumerated " +
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

	private void mapKeySupplementalDetails(XProperty attributeXProperty) {
		final MapKeyEnumerated mapKeyEnumeratedAnn = attributeXProperty.getAnnotation( MapKeyEnumerated.class );
		if ( mapKeyEnumeratedAnn != null ) {
			enumType = mapKeyEnumeratedAnn.value();
		}

		final MapKeyTemporal mapKeyTemporalAnn = attributeXProperty.getAnnotation( MapKeyTemporal.class );
		if ( mapKeyTemporalAnn != null ) {
			temporalPrecision = mapKeyTemporalAnn.value();
		}

		explicitSqlTypeAccess = typeConfiguration -> {
			final MapKeyJdbcType explicitDescriptorAnn = attributeXProperty.getAnnotation( MapKeyJdbcType.class );
			if ( explicitDescriptorAnn != null ) {
				final JdbcType explicitStdAnn = explicitDescriptorAnn.value();
				final Class<? extends JdbcTypeDescriptor> stdImplJavaType = explicitStdAnn.value();

				try {
					return stdImplJavaType.newInstance();
				}
				catch (InstantiationException | IllegalAccessException e) {
					throw new MappingException( "Could not instantiate explicit SqlTypeDescriptor - " + stdImplJavaType.getName(), e );
				}
			}

			final MapKeyJdbcTypeCode explicitCodeAnn = attributeXProperty.getAnnotation( MapKeyJdbcTypeCode.class );
			if ( explicitCodeAnn != null ) {
				final JdbcTypeCode explicitSqlTypeAnn = explicitCodeAnn.value();
				return typeConfiguration.getJdbcTypeDescriptorRegistry().getDescriptor( explicitSqlTypeAnn.value() );
			}

			return null;
		};

		explicitJtdAccess = typeConfiguration -> {
			final MapKeyJavaType mapKeyJtdAnn = attributeXProperty.getAnnotation( MapKeyJavaType.class );
			if ( mapKeyJtdAnn == null ) {
				return null;
			}

			final JavaType jtdAnn = mapKeyJtdAnn.value();
			final Class<? extends BasicJavaDescriptor<?>> jtdJavaType = jtdAnn.value();
			try {
				return jtdJavaType.newInstance();
			}
			catch (InstantiationException | IllegalAccessException e) {
				throw new MappingException( "Could not instantiate explicit JavaTypeDescriptor - " + jtdJavaType.getName(), e );
			}
		};
	}


	private void normalSupplementalDetails(
			XProperty attributeXProperty,
			MetadataBuildingContext buildingContext) {
		explicitSqlTypeAccess = typeConfiguration -> {
			final JdbcType explicitStdAnn = attributeXProperty.getAnnotation( JdbcType.class );
			if ( explicitStdAnn != null ) {
				final Class<? extends JdbcTypeDescriptor> stdImplJavaType = explicitStdAnn.value();

				try {
					return stdImplJavaType.newInstance();
				}
				catch (InstantiationException | IllegalAccessException e) {
					throw new MappingException( "Could not instantiate explicit SqlTypeDescriptor - " + stdImplJavaType.getName(), e );
				}
			}

			final JdbcTypeCode explicitSqlTypeAnn = attributeXProperty.getAnnotation( JdbcTypeCode.class );
			if ( explicitSqlTypeAnn != null ) {
				return typeConfiguration.getJdbcTypeDescriptorRegistry().getDescriptor( explicitSqlTypeAnn.value() );
			}

			return null;
		};

		explicitJtdAccess = typeConfiguration -> {
			final JavaType explicitJtdAnn = attributeXProperty.getAnnotation( JavaType.class );
			if ( explicitJtdAnn != null ) {
				final Class<? extends BasicJavaDescriptor<?>> jtdImplJavaType = explicitJtdAnn.value();

				try {
					return jtdImplJavaType.newInstance();
				}
				catch (InstantiationException | IllegalAccessException e) {
					throw new MappingException( "Could not instantiate explicit JavaTypeDescriptor - " + jtdImplJavaType.getName(), e );
				}
			}

			return null;
		};

		explicitMutabilityAccess = typeConfiguration -> {
			final Mutability mutabilityAnn = attributeXProperty.getAnnotation( Mutability.class );

			if ( mutabilityAnn != null ) {
				final Class<? extends MutabilityPlan<?>> planJavaType = mutabilityAnn.value();

				try {
					return planJavaType.newInstance();
				}
				catch (InstantiationException | IllegalAccessException e) {
					throw new MappingException( "Could not instantiate explicit MutabilityPlan - " + planJavaType.getName(), e );
				}
			}

			final Immutable immutableAnn = attributeXProperty.getAnnotation( Immutable.class );
			if ( immutableAnn != null ) {
				return ImmutableMutabilityPlan.instance();
			}

			// see if the value's type Class is annotated `@Immutable`
			final Class attributeType = ReflectHelper.getClass( implicitJavaTypeAccess.apply( typeConfiguration ) );
			if ( attributeType.isAnnotationPresent( Immutable.class ) ) {
				return ImmutableMutabilityPlan.instance();
			}

			// if the value is converted, see if the converter Class is annotated `@Immutable`
			if ( converterDescriptor != null ) {
				if ( converterDescriptor.getAttributeConverterClass().isAnnotationPresent( Immutable.class ) ) {
					return ImmutableMutabilityPlan.instance();
				}
			}

			return null;
		};

		final Enumerated enumeratedAnn = attributeXProperty.getAnnotation( Enumerated.class );
		if ( enumeratedAnn != null ) {
			enumType = enumeratedAnn.value();
		}

		final Temporal temporalAnn = attributeXProperty.getAnnotation( Temporal.class );
		if ( temporalAnn != null ) {
			temporalPrecision = temporalAnn.value();
		}
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

	public void setExplicitType(Type typeAnn) {
		setExplicitType( typeAnn.type() );
		this.explicitLocalTypeParams = extractTypeParams( typeAnn.parameters() );
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

	private void validate() {
		Ejb3Column.checkPropertyConsistency( columns, propertyName );
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

		if ( temporalPrecision != null ) {
			basicValue.setTemporalPrecision( temporalPrecision );
		}

		// todo (6.0) : explicit SqlTypeDescriptor / JDBC type-code
		// todo (6.0) : explicit mutability / immutable
		// todo (6.0) : explicit Comparator

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
							referencedEntityName, (Ejb3JoinColumn[]) columns, basicValue
					)
			);
		}
		else {
			for ( Ejb3Column column : columns ) {
				column.linkWithValue( basicValue );
			}
		}
	}

	public void fillSimpleValue() {
		LOG.debugf( "Starting `BasicValueBinder#fillSimpleValue` for %s", propertyName );

		basicValue.setExplicitTypeName( explicitBasicTypeName );
		basicValue.setExplicitTypeParams( explicitLocalTypeParams );
		// todo (6.0): Ideally we could check the type class like we did in 5.5 but that is unavailable at this point
		java.lang.reflect.Type type = implicitJavaTypeAccess == null ? null : implicitJavaTypeAccess.apply( getTypeConfiguration() );
		if ( xproperty != null && returnedClassName != null && ( !(type instanceof Class<?>) || !( (Class<?>) type ).isPrimitive() ) ) {
//		if ( typeClass != null && DynamicParameterizedType.class.isAssignableFrom( typeClass ) ) {
			final Map<String, Object> parameters = new HashMap<>();
			parameters.put( DynamicParameterizedType.IS_DYNAMIC, Boolean.toString( true ) );
			parameters.put( DynamicParameterizedType.RETURNED_CLASS, returnedClassName );
			parameters.put( DynamicParameterizedType.IS_PRIMARY_KEY, Boolean.toString( kind == Kind.MAP_KEY ) );

			parameters.put( DynamicParameterizedType.ENTITY, persistentClassName );
			parameters.put( DynamicParameterizedType.XPROPERTY, xproperty );
			parameters.put( DynamicParameterizedType.PROPERTY, xproperty.getName() );
			if ( accessType != null ) {
				parameters.put( DynamicParameterizedType.ACCESS_TYPE, accessType.getType() );
			}
			if ( explicitLocalTypeParams != null ) {
				parameters.putAll( explicitLocalTypeParams );
			}
			basicValue.setTypeParameters( (Map) parameters );
		}

		basicValue.setJpaAttributeConverterDescriptor( converterDescriptor );

		basicValue.setImplicitJavaTypeAccess( implicitJavaTypeAccess );
		basicValue.setExplicitJavaTypeAccess( explicitJtdAccess );
		basicValue.setExplicitMutabilityPlanAccess( explicitMutabilityAccess );
		basicValue.setExplicitSqlTypeAccess( explicitSqlTypeAccess );

		if ( enumType != null ) {
			basicValue.setEnumerationStyle( enumType );
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
	}
}
