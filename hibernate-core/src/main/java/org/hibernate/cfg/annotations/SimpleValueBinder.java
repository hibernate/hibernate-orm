/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.annotations;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.MapKeyEnumerated;
import javax.persistence.MapKeyTemporal;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.annotations.MapKeyType;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.common.reflection.ClassLoadingException;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.model.type.spi.BasicTypeProducer;
import org.hibernate.boot.model.type.spi.BasicTypeResolver;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.AttributeConverterDescriptor;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.AccessType;
import org.hibernate.cfg.BasicTypeResolverConvertibleSupport;
import org.hibernate.cfg.BasicTypeResolverSupport;
import org.hibernate.cfg.BinderHelper;
import org.hibernate.cfg.Ejb3Column;
import org.hibernate.cfg.Ejb3JoinColumn;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.cfg.PkDrivenByDefaultMapsIdSecondPass;
import org.hibernate.cfg.SetSimpleValueTypeSecondPass;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.type.converter.spi.AttributeConverterDefinition;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.BasicTypeParameters;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.DynamicParameterizedType;

import org.jboss.logging.Logger;

/**
 * @author Emmanuel Bernard
 */
public class SimpleValueBinder<T> {

	// todo (6.0) : In light of how we want to build Types (specifically BasicTypes) moving forward this Class should undergo major changes
	//		see the comments in #setType
	//		but as always the "design" of these classes make it unclear exactly how to change it properly.

	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class, SimpleValueBinder.class.getName() );

	public enum Kind {
		ATTRIBUTE,
		COLLECTION_ID,
		COLLECTION_ELEMENT,
		COLLECTION_INDEX
	}

	private final Kind kind;
	private final MetadataBuildingContext buildingContext;

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Property info
	//		todo (6.0) : ? why is this stuff here?  it is irrelevant for building a SimpleValue

	private XProperty xproperty;
	private AccessType accessType;


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// BasicType info

	private BasicJavaDescriptor<T> javaDescriptor;
	private SqlTypeDescriptor sqlTypeDescriptor;
	private AttributeConverterDescriptor converterDescriptor;
	private boolean isVersion;
	private boolean isNationalized;
	private boolean isLob;
	private javax.persistence.EnumType enumType;
	private TemporalType temporalPrecision;

	private BasicTypeResolver basicTypeResolver;
	private SimpleValue simpleValue;


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// mapping (database) data

	private Table table;
	private Ejb3Column[] columns;


	// todo (6.0) : investigate what this is used for.  it may not be needed
	private String referencedEntityName;


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// pretty sure none of these are needed any longer
	// todo (6.0) : verify, and remove if so
	private String propertyName;
	private String returnedClassName;
	private String persistentClassName;
	private String explicitType = "";
	private String defaultType = "";
	private Properties typeParameters = new Properties();


	public SimpleValueBinder(Kind kind, MetadataBuildingContext buildingContext) {
		assert kind != null;
		assert  buildingContext != null;

		this.kind = kind;
		this.buildingContext = buildingContext;
	}

	public void setReferencedEntityName(String referencedEntityName) {
		this.referencedEntityName = referencedEntityName;
	}

	public boolean isVersion() {
		return isVersion;
	}

	public void setVersion(boolean isVersion) {
		this.isVersion = isVersion;
		if ( isVersion && simpleValue != null ) {
			simpleValue.makeVersion();
		}
	}

	public void setTimestampVersionType(String versionType) {
		// todo (6.0) : change this to instead pass any indicated org.hibernate.tuple.ValueGenerator
		this.timeStampVersionType = versionType;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	public void setReturnedClassName(String returnedClassName) {
		this.returnedClassName = returnedClassName;

		if ( defaultType.length() == 0 ) {
			defaultType = returnedClassName;
		}
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

	//TODO execute it lazily to be order safe

	public void setType(
			XProperty navigableXProperty,
			XClass navigableXClass,
			String declaringClassName,
			AttributeConverterDescriptor converterDescriptor) {
		if ( navigableXClass == null ) {
			// we cannot guess anything
			return;
		}

		if ( columns == null ) {
			throw new AssertionFailure( "SimpleValueBinder.setColumns should be set beforeQuery SimpleValueBinder.setType" );
		}

		XClass returnedClassOrElement = navigableXClass;
		boolean isArray = false;
		if ( navigableXProperty.isArray() ) {
			returnedClassOrElement = navigableXProperty.getElementClass();
			isArray = true;
		}

		// If we get into this method we know that there is a Java type for the value
		//		and that it is safe to load on the app classloader.
		final Class navigableJavaType = resolveJavaType( returnedClassOrElement, buildingContext );
		if ( navigableJavaType == null ) {
			throw new IllegalStateException( "BasicType requires Java type" );
		}
		final BasicJavaDescriptor javaDescriptor = (BasicJavaDescriptor) buildingContext.getBootstrapContext()
				.getTypeConfiguration()
				.getJavaTypeDescriptorRegistry()
				.getDescriptor( navigableJavaType );

		this.xproperty = navigableXProperty;
		Properties typeParameters = this.typeParameters;
		typeParameters.clear();

		final boolean isMap = Map.class.isAssignableFrom(
				buildingContext.getBootstrapContext()
						.getReflectionManager()
						.toClass( returnedClassOrElement )
		);



		final boolean key = kind == Kind.COLLECTION_INDEX;

		if ( !key ) {
			isLob = navigableXProperty.isAnnotationPresent( Lob.class );
		}

		if ( getDialect().supportsNationalizedTypes() ) {
			isNationalized = navigableXProperty.isAnnotationPresent( Nationalized.class )
					|| buildingContext.getBuildingOptions().useNationalizedCharacterData();
		}

		applyAttributeConverter( navigableXProperty, converterDescriptor );

		Type explicitTypeAnn = null;
		if ( key ) {
			final MapKeyType mapKeyTypeAnn = navigableXProperty.getAnnotation( MapKeyType.class );
			if ( mapKeyTypeAnn != null ) {
				explicitTypeAnn = mapKeyTypeAnn.value();
			}
		}
		else {
			explicitTypeAnn = navigableXProperty.getAnnotation( Type.class );
		}

		if ( explicitTypeAnn != null ) {
			setExplicitType( explicitTypeAnn );
		}
		else {
			switch ( kind ) {
				case COLLECTION_ID: {
					basicTypeResolver = new BasicTypeResolverCollectionIdImpl(
							buildingContext,
							navigableXProperty
					);
					break;
				}
				case COLLECTION_INDEX: {
					if ( isMap ) {
						basicTypeResolver = new BasicTypeResolverMapKeyImpl(
								buildingContext,
								converterDescriptor,
								navigableXProperty,
								isLob,
								isNationalized
						);
					}
					else {
						basicTypeResolver = new BasicTypeResolverListIndexImpl( buildingContext, navigableXProperty );
					}
					break;
				}
				case COLLECTION_ELEMENT: {
					basicTypeResolver = new BasicTypeResolverCollectionElementImpl(
							buildingContext,
							converterDescriptor,
							navigableXProperty,
							isLob,
							isNationalized
					);
					break;
				}
				default: {
					assert kind == Kind.ATTRIBUTE;
					basicTypeResolver = new BasicTypeResolverAttributeImpl(
							buildingContext,
							converterDescriptor,
							navigableXProperty,
							isLob,
							isNationalized
					);
				}
			}
		}

		this.typeParameters = typeParameters;
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

	private void applyAttributeConverter(XProperty property, AttributeConverterDescriptor attributeConverterDescriptor) {
		if ( attributeConverterDescriptor == null ) {
			return;
		}

		LOG.debugf( "Starting applyAttributeConverter [%s:%s]", persistentClassName, property.getName() );

		if ( property.isAnnotationPresent( Id.class ) ) {
			LOG.debugf( "Skipping AttributeConverter checks for Id attribute [%s]", property.getName() );
			return;
		}

		if ( isVersion ) {
			LOG.debugf( "Skipping AttributeConverter checks for version attribute [%s]", property.getName() );
			return;
		}

		if ( !key && property.isAnnotationPresent( Temporal.class ) ) {
			LOG.debugf( "Skipping AttributeConverter checks for Temporal attribute [%s]", property.getName() );
			return;
		}
		if ( key && property.isAnnotationPresent( MapKeyTemporal.class ) ) {
			LOG.debugf( "Skipping AttributeConverter checks for map-key annotated as MapKeyTemporal [%s]", property.getName() );
			return;
		}

		if ( !key && property.isAnnotationPresent( Enumerated.class ) ) {
			LOG.debugf( "Skipping AttributeConverter checks for Enumerated attribute [%s]", property.getName() );
			return;
		}
		if ( key && property.isAnnotationPresent( MapKeyEnumerated.class ) ) {
			LOG.debugf( "Skipping AttributeConverter checks for map-key annotated as MapKeyEnumerated [%s]", property.getName() );
			return;
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
		this.explicitType = explicitType;
	}

	//FIXME raise an assertion failure  if setResolvedTypeMapping(String) and setResolvedTypeMapping(Type) are use at the same time

	public void setExplicitType(Type typeAnn) {
		basicTypeResolver = new BasicTypeResolverExplicitImpl( buildingContext, typeAnn );
	}

	private void validate() {
		//TODO check necessary params
		Ejb3Column.checkPropertyConsistency( columns, propertyName );
	}

	public SimpleValue make() {

		validate();
		LOG.debugf( "building SimpleValue for %s", propertyName );
		if ( table == null ) {
			table = columns[0].getTable();
		}
		simpleValue = new SimpleValue( buildingContext.getMetadataCollector(), table );
		if ( isVersion ) {
			simpleValue.makeVersion();
		}
		if ( isNationalized ) {
			simpleValue.makeNationalized();
		}
		if ( isLob ) {
			simpleValue.makeLob();
		}

		linkWithValue();

		boolean isInSecondPass = buildingContext.getMetadataCollector().isInSecondPass();
		if ( !isInSecondPass ) {
			//Defer this to the second pass
			buildingContext.getMetadataCollector().addSecondPass( new SetSimpleValueTypeSecondPass( this ) );
		}
		else {
			//We are already in second pass
			fillSimpleValue();
		}
		return simpleValue;
	}

	public void linkWithValue() {
		if ( columns[0].isNameDeferred() && !buildingContext.getMetadataCollector().isInSecondPass() && referencedEntityName != null ) {
			buildingContext.getMetadataCollector().addSecondPass(
					new PkDrivenByDefaultMapsIdSecondPass(
							referencedEntityName, (Ejb3JoinColumn[]) columns, simpleValue
					)
			);
		}
		else {
			for ( Ejb3Column column : columns ) {
				column.linkWithValue( simpleValue );
			}
		}
	}

	public void fillSimpleValue() {
		LOG.debugf( "Starting fillSimpleValue for %s", propertyName );

		final BasicTypeProducer producer;

		if ( converterDescriptor != null ) {
			if ( ! BinderHelper.isEmptyAnnotationValue( explicitType ) ) {
				throw new AnnotationException(
						String.format(
								"AttributeConverter and explicit Type cannot be applied to same attribute [%s.%s];" +
										"remove @Type or specify @Convert(disableConversion = true)",
								persistentClassName,
								propertyName
						)
				);
			}
			LOG.debugf(
					"Applying JPA AttributeConverter [%s] to [%s:%s]",
					converterDescriptor,
					persistentClassName,
					propertyName
			);

			producer = buildingContext.getBootstrapContext().getBasicTypeProducerRegistry().makeUnregisteredProducer();
			simpleValue.setJpaAttributeConverterDescriptor( converterDescriptor );
		}
		else {
			if ( !BinderHelper.isEmptyAnnotationValue( explicitType ) ) {
				producer = buildingContext.getBootstrapContext().getBasicTypeProducerRegistry().resolve( explicitType );
			}
			else {
				BasicTypeProducer test = buildingContext.getBootstrapContext().getBasicTypeProducerRegistry().resolve( returnedClassName );
				if ( test != null ) {
					producer = test;
				}
				else {
					test = buildingContext.getBootstrapContext().getBasicTypeProducerRegistry().resolve( defaultType );

					if ( test != null ) {
						producer = test;
					}
					else {
						producer = buildingContext.getBootstrapContext().getBasicTypeProducerRegistry().makeUnregisteredProducer();
					}
				}
			}
		}

		simpleValue.setBasicTypeResolver( producer );

		producer.injectBasicTypeSiteContext(
				new BasicTypeResolver() {
					@Override
					public BasicJavaDescriptor getJavaTypeDescriptor() {
						if ( persistentClassName != null || converterDescriptor != null ) {
							final Class javaType = ReflectHelper.reflectedPropertyClass(
									persistentClassName,
									propertyName,
									buildingContext.getBuildingOptions()
											.getServiceRegistry()
											.getService( ClassLoaderService.class )
							);
							return (BasicJavaDescriptor) buildingContext.getBootstrapContext()
									.getTypeConfiguration()
									.getJavaTypeDescriptorRegistry()
									.getDescriptor( javaType );
						}
						else if ( returnedClassName != null ) {
							final Class javaType = buildingContext.getBuildingOptions().getServiceRegistry().getService(
									ClassLoaderService.class ).classForName( returnedClassName );
							return (BasicJavaDescriptor) buildingContext.getBootstrapContext()
									.getTypeConfiguration()
									.getJavaTypeDescriptorRegistry()
									.getDescriptor( javaType );
						}
						return null;
					}

					@Override
					public SqlTypeDescriptor getSqlTypeDescriptor() {
						return null;
					}

					@Override
					public AttributeConverterDefinition getAttributeConverterDefinition() {
						return converterDescriptor;
					}

					@Override
					public MutabilityPlan getMutabilityPlan() {
						// todo : based on @Immutable?
						return null;
					}

					@Override
					public Comparator getComparator() {
						return null;
					}

					@Override
					public TemporalType getTemporalPrecision() {
						return temporalPrecision;
					}

					@Override
					public Map getLocalTypeParameters() {
						return typeParameters;
					}

					@Override
					public boolean isId() {
						return SimpleValueBinder.this.key;
					}

					@Override
					public boolean isVersion() {
						return SimpleValueBinder.this.isVersion();
					}

					@Override
					public boolean isNationalized() {
						return SimpleValueBinder.this.isNationalized;
					}

					@Override
					public boolean isLob() {
						return SimpleValueBinder.this.isLob;
					}

					@Override
					public javax.persistence.EnumType getEnumeratedType() {
						return enumType;
					}

					@Override
					public TypeConfiguration getTypeConfiguration() {
						return buildingContext.getMetadataCollector().getTypeConfiguration();
					}
				}
		);

		if ( persistentClassName != null || converterDescriptor != null ) {
			try {
				simpleValue.setTypeUsingReflection( persistentClassName, propertyName );
			}
			catch (Exception e) {
				throw new MappingException(
						String.format(
								Locale.ROOT,
								"Unable to determine basic type mapping via reflection [%s -> %s]",
								persistentClassName,
								propertyName
						),
						e
				);
			}
		}

		if ( !simpleValue.isTypeSpecified() && isVersion() ) {
			simpleValue.setTypeName( "integer" );
		}

		// HHH-5205
		if ( timeStampVersionType != null ) {
			simpleValue.setTypeName( timeStampVersionType );
		}
		
		if ( simpleValue.getTypeName() != null && simpleValue.getTypeName().length() > 0
				&& simpleValue.getBuildingContext().basicType( simpleValue.getTypeName() ) == null ) {
			try {
				Class typeClass = buildingContext.getBootstrapContext().getClassLoaderAccess().classForName( simpleValue.getTypeName() );

				if ( typeClass != null && DynamicParameterizedType.class.isAssignableFrom( typeClass ) ) {
					Properties parameters = simpleValue.getTypeParameters();
					if ( parameters == null ) {
						parameters = new Properties();
					}
					parameters.put( DynamicParameterizedType.IS_DYNAMIC, Boolean.toString( true ) );
					parameters.put( DynamicParameterizedType.RETURNED_CLASS, returnedClassName );
					parameters.put( DynamicParameterizedType.IS_PRIMARY_KEY, Boolean.toString( key ) );

					parameters.put( DynamicParameterizedType.ENTITY, persistentClassName );
					parameters.put( DynamicParameterizedType.XPROPERTY, xproperty );
					parameters.put( DynamicParameterizedType.PROPERTY, xproperty.getName() );
					parameters.put( DynamicParameterizedType.ACCESS_TYPE, accessType.getType() );
					simpleValue.setTypeParameters( parameters );
				}
			}
			catch (ClassLoadingException e) {
				throw new MappingException( "Could not determine type for: " + simpleValue.getTypeName(), e );
			}
		}

	}

	public void setKey(boolean key) {
		this.key = key;
	}

	public AccessType getAccessType() {
		return accessType;
	}

	public void setAccessType(AccessType accessType) {
		this.accessType = accessType;
	}


	private static class BasicTypeResolverExplicitImpl implements BasicTypeResolver {
		// todo (6.0) : ? shouldn't this be convertible as well?

		private final MetadataBuildingContext buildingContext;
		private final String name;
		private final HashMap<String,String> parameters;

		BasicTypeResolverExplicitImpl(
				MetadataBuildingContext buildingContext,
				Type typeAnn) {
			this.buildingContext = buildingContext;
			this.name = typeAnn.type();
			if ( typeAnn.parameters().length > 0 ) {
				this.parameters = new HashMap<>(  );
				for ( Parameter param : typeAnn.parameters() ) {
					this.parameters.put( param.name(), param.value() );
				}
			}
			else {
				parameters = Collections.emptyMap();
			}
		}

		@Override
		public <T> BasicType<T> resolveBasicType() {
			// Name could refer to:
			//		1) a registered TypeDef
			//		2) basic type "resolution key"

			// todo (6.0) : implement
			throw new NotYetImplementedException(  );
		}
	}

	private static BasicTypeResolver resolveMapKeyBasicTypeResolver(
			MetadataBuildingContext buildingContext,
			XProperty mapAttribute) {

	}

	private class BasicTypeResolverCollectionIdImpl extends BasicTypeResolverSupport {
		public BasicTypeResolverCollectionIdImpl(
				MetadataBuildingContext buildingContext,
				XProperty collectionAttributeDescriptor) {
			super( buildingContext );

			// todo (6.0) : ? not yet sure how to do that because this actually comes from the owner - SecondPass probably?

		}

	}

	private static class BasicTypeResolverMapKeyImpl
			extends BasicTypeResolverConvertibleSupport
			implements BasicTypeResolver, JdbcRecommendedSqlTypeMappingContext, BasicTypeParameters {
		private final XProperty mapAttribute;

		private final BasicJavaDescriptor javaDescriptor;
		private final TemporalType temporalPrecision;
		private final javax.persistence.EnumType enumType;
		private final boolean isLob;
		private final boolean isNationalized;

		public BasicTypeResolverMapKeyImpl(
				MetadataBuildingContext buildingContext,
				AttributeConverterDescriptor converterDescriptor,
				XProperty mapAttribute,
				boolean isLob,
				boolean isNationalized) {
			super( buildingContext, converterDescriptor );
			this.mapAttribute = mapAttribute;
			this.isLob = isLob;
			// todo (6.0) : need a @MapKeyNationalized annotation, then read that here
			this.isNationalized = isNationalized;


			final Class javaType = buildingContext.getBootstrapContext()
					.getReflectionManager()
					.toClass( mapAttribute.getMapKey() );
			javaDescriptor = (BasicJavaDescriptor) buildingContext.getBootstrapContext()
					.getTypeConfiguration()
					.getJavaTypeDescriptorRegistry()
					.getDescriptor( javaType );

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

			if ( javaType.isEnum() ) {
				final MapKeyEnumerated enumeratedAnn = mapAttribute.getAnnotation( MapKeyEnumerated.class );
				if ( enumeratedAnn == null ) {
					enumType = javax.persistence.EnumType.ORDINAL;
				}
				else {
					enumType = enumeratedAnn.value();
					if ( enumType == null ) {
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
		}

		@Override
		@SuppressWarnings("unchecked")
		public BasicJavaDescriptor getJavaTypeDescriptor() {
			return javaDescriptor;
		}

		@Override
		public SqlTypeDescriptor getSqlTypeDescriptor() {
			return null;
		}

		@Override
		public TemporalType getTemporalPrecision() {
			return temporalPrecision;
		}

		@Override
		public boolean isNationalized() {
			return isNationalized;
		}

		@Override
		public boolean isLob() {
			return isLob;
		}

		@Override
		public javax.persistence.EnumType getEnumeratedType() {
			return enumType;
		}
	}

	private static class BasicTypeResolverListIndexImpl
			extends BasicTypeResolverSupport<Integer>
			implements BasicTypeResolver, JdbcRecommendedSqlTypeMappingContext, BasicTypeParameters {
		private final MetadataBuildingContext buildingContext;
		private final XProperty listAttribute;

		public BasicTypeResolverListIndexImpl(
				MetadataBuildingContext buildingContext,
				XProperty listAttribute) {
			super( buildingContext );

			this.buildingContext = buildingContext;
			this.listAttribute = listAttribute;
		}

		@Override
		@SuppressWarnings("unchecked")
		public BasicJavaDescriptor<Integer> getJavaTypeDescriptor() {
			return (BasicJavaDescriptor) getTypeConfiguration().getJavaTypeDescriptorRegistry().getDescriptor( Integer.class );
		}

		@Override
		public SqlTypeDescriptor getSqlTypeDescriptor() {
			// ATM we have no way to specify the SQL type in annotations.
			//		I have proposed a @SqlType annotation, we shall see
			return null;
		}

		@Override
		public TypeConfiguration getTypeConfiguration() {
			return buildingContext.getBootstrapContext().getTypeConfiguration();
		}
	}

	private static class BasicTypeResolverAttributeImpl
			extends BasicTypeResolverConvertibleSupport
			implements BasicTypeResolver, JdbcRecommendedSqlTypeMappingContext, BasicTypeParameters {

		private final BasicJavaDescriptor javaDescriptor;
		private final TemporalType temporalPrecision;
		private final javax.persistence.EnumType enumType;
		private final boolean isLob;
		private final boolean isNationalized;

		@SuppressWarnings("unchecked")
		public BasicTypeResolverAttributeImpl(
				MetadataBuildingContext buildingContext,
				AttributeConverterDescriptor converterDescriptor,
				XProperty attributeDescriptor,
				boolean isLob,
				boolean isNationalized) {
			super( buildingContext, converterDescriptor );
			this.isLob = isLob;
			this.isNationalized = isNationalized;

			final Class javaType = buildingContext.getBootstrapContext()
					.getReflectionManager()
					.toClass( attributeDescriptor.getType() );
			javaDescriptor = (BasicJavaDescriptor) buildingContext.getBootstrapContext()
					.getTypeConfiguration()
					.getJavaTypeDescriptorRegistry()
					.getDescriptor( javaType );

			final Temporal temporalAnn = attributeDescriptor.getAnnotation( Temporal.class );
			if ( temporalAnn != null ) {
				temporalPrecision = temporalAnn.value();
				if ( temporalPrecision == null ) {
					throw new IllegalStateException(
							"No javax.persistence.TemporalType defined for @javax.persistence.Temporal " +
									"associated with attribute " + attributeDescriptor.getDeclaringClass().getName() +
									'.' + attributeDescriptor.getName()
					);
				}
			}
			else {
				temporalPrecision = null;
			}

			if ( javaType.isEnum() ) {
				final Enumerated enumeratedAnn = attributeDescriptor.getAnnotation( Enumerated.class );
				if ( enumeratedAnn == null ) {
					enumType = javax.persistence.EnumType.ORDINAL;
				}
				else {
					enumType = enumeratedAnn.value();
					if ( enumType == null ) {
						throw new IllegalStateException(
								"javax.persistence.EnumType was null on @javax.persistence.Enumerated " +
										" associated with attribute " + attributeDescriptor.getDeclaringClass().getName() +
										'.' + attributeDescriptor.getName()
						);
					}
				}
			}
			else {
				enumType = null;
			}
		}

		@Override
		public BasicJavaDescriptor getJavaTypeDescriptor() {
			return javaDescriptor;
		}

		@Override
		public SqlTypeDescriptor getSqlTypeDescriptor() {
			return null;
		}

		@Override
		public TemporalType getTemporalPrecision() {
			return temporalPrecision;
		}

		@Override
		public boolean isNationalized() {
			return isNationalized;
		}

		@Override
		public boolean isLob() {
			return isLob;
		}

		@Override
		public javax.persistence.EnumType getEnumeratedType() {
			return enumType;
		}
	}

	private class BasicTypeResolverCollectionElementImpl
			extends BasicTypeResolverConvertibleSupport {
		private final XProperty attributeDescriptor;
		private final boolean isLob;
		private final boolean isNationalized;

		public BasicTypeResolverCollectionElementImpl(
				MetadataBuildingContext buildingContext,
				AttributeConverterDescriptor converterDescriptor,
				XProperty attributeDescriptor,
				boolean isLob,
				boolean isNationalized) {
			super( buildingContext, converterDescriptor );

			this.attributeDescriptor = attributeDescriptor;
			this.isLob = isLob;
			this.isNationalized = isNationalized;

			final Class javaType = buildingContext.getBootstrapContext()
					.getReflectionManager()
					.toClass( attributeDescriptor.getType() );
			javaDescriptor = (BasicJavaDescriptor) buildingContext.getBootstrapContext()
					.getTypeConfiguration()
					.getJavaTypeDescriptorRegistry()
					.getDescriptor( javaType );

			final Temporal temporalAnn = attributeDescriptor.getAnnotation( Temporal.class );
			if ( temporalAnn != null ) {
				temporalPrecision = temporalAnn.value();
				if ( temporalPrecision == null ) {
					throw new IllegalStateException(
							"No javax.persistence.TemporalType defined for @javax.persistence.Temporal " +
									"associated with attribute " + attributeDescriptor.getDeclaringClass().getName() +
									'.' + attributeDescriptor.getName()
					);
				}
			}
			else {
				temporalPrecision = null;
			}

			if ( javaType.isEnum() ) {
				final Enumerated enumeratedAnn = attributeDescriptor.getAnnotation( Enumerated.class );
				if ( enumeratedAnn == null ) {
					enumType = javax.persistence.EnumType.ORDINAL;
				}
				else {
					enumType = enumeratedAnn.value();
					if ( enumType == null ) {
						throw new IllegalStateException(
								"javax.persistence.EnumType was null on @javax.persistence.Enumerated " +
										" associated with attribute " + attributeDescriptor.getDeclaringClass().getName() +
										'.' + attributeDescriptor.getName()
						);
					}
				}
			}
			else {
				enumType = null;
			}
		}

		@Override
		public BasicJavaDescriptor getJavaTypeDescriptor() {
			return javaDescriptor;
		}

		@Override
		public SqlTypeDescriptor getSqlTypeDescriptor() {
			return null;
		}

		@Override
		public TemporalType getTemporalPrecision() {
			return temporalPrecision;
		}

		@Override
		public boolean isNationalized() {
			return isNationalized;
		}

		@Override
		public boolean isLob() {
			return isLob;
		}

		@Override
		public javax.persistence.EnumType getEnumeratedType() {
			return enumType;
		}
	}
}
