/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.annotations;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
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
import org.hibernate.boot.model.TypeDefinition;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.AccessType;
import org.hibernate.cfg.BinderHelper;
import org.hibernate.cfg.Ejb3Column;
import org.hibernate.cfg.Ejb3JoinColumn;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.cfg.PkDrivenByDefaultMapsIdSecondPass;
import org.hibernate.cfg.SetSimpleValueTypeSecondPass;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.type.CharacterArrayClobType;
import org.hibernate.type.CharacterArrayNClobType;
import org.hibernate.type.CharacterNCharType;
import org.hibernate.type.EnumType;
import org.hibernate.type.PrimitiveCharacterArrayClobType;
import org.hibernate.type.PrimitiveCharacterArrayNClobType;
import org.hibernate.type.SerializableToBlobType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.StringNVarcharType;
import org.hibernate.type.WrappedMaterializedBlobType;
import org.hibernate.usertype.DynamicParameterizedType;

import org.jboss.logging.Logger;

/**
 * @author Emmanuel Bernard
 */
public class SimpleValueBinder {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, SimpleValueBinder.class.getName());

	private MetadataBuildingContext buildingContext;

	private String propertyName;
	private String returnedClassName;
	private Ejb3Column[] columns;
	private String persistentClassName;
	private String explicitType = "";
	private String defaultType = "";
	private Properties typeParameters = new Properties();
	private boolean isNationalized;
	private boolean isLob;

	private Table table;
	private SimpleValue simpleValue;
	private boolean isVersion;
	private String timeStampVersionType;
	//is a Map key
	private boolean key;
	private String referencedEntityName;
	private XProperty xproperty;
	private AccessType accessType;

	private ConverterDescriptor attributeConverterDescriptor;

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

	public void setType(XProperty property, XClass returnedClass, String declaringClassName, ConverterDescriptor attributeConverterDescriptor) {
		if ( returnedClass == null ) {
			// we cannot guess anything
			return;
		}

		XClass returnedClassOrElement = returnedClass;
		boolean isArray = false;
		if ( property.isArray() ) {
			returnedClassOrElement = property.getElementClass();
			isArray = true;
		}
		this.xproperty = property;
		Properties typeParameters = this.typeParameters;
		typeParameters.clear();
		String type = BinderHelper.ANNOTATION_STRING_DEFAULT;

		if ( getDialect().supportsNationalizedTypes() ) {
			isNationalized = property.isAnnotationPresent( Nationalized.class )
					|| buildingContext.getBuildingOptions().useNationalizedCharacterData();
		}

		Type annType = null;
		if ( (!key && property.isAnnotationPresent( Type.class ))
				|| (key && property.isAnnotationPresent( MapKeyType.class )) ) {
			if ( key ) {
				MapKeyType ann = property.getAnnotation( MapKeyType.class );
				annType = ann.value();
			}
			else {
				annType = property.getAnnotation( Type.class );
			}
		}

		if ( annType != null ) {
			setExplicitType( annType );
			type = explicitType;
		}
		else if ( ( !key && property.isAnnotationPresent( Temporal.class ) )
				|| ( key && property.isAnnotationPresent( MapKeyTemporal.class ) ) ) {

			boolean isDate;
			if ( buildingContext.getBootstrapContext().getReflectionManager().equals( returnedClassOrElement, Date.class ) ) {
				isDate = true;
			}
			else if ( buildingContext.getBootstrapContext().getReflectionManager().equals( returnedClassOrElement, Calendar.class ) ) {
				isDate = false;
			}
			else {
				throw new AnnotationException(
						"@Temporal should only be set on a java.util.Date or java.util.Calendar property: "
								+ StringHelper.qualify( persistentClassName, propertyName )
				);
			}
			final TemporalType temporalType = getTemporalType( property );
			switch ( temporalType ) {
				case DATE:
					type = isDate ? "date" : "calendar_date";
					break;
				case TIME:
					type = "time";
					if ( !isDate ) {
						throw new NotYetImplementedException(
								"Calendar cannot persist TIME only"
										+ StringHelper.qualify( persistentClassName, propertyName )
						);
					}
					break;
				case TIMESTAMP:
					type = isDate ? "timestamp" : "calendar";
					break;
				default:
					throw new AssertionFailure( "Unknown temporal type: " + temporalType );
			}
			explicitType = type;
		}
		else if ( !key && property.isAnnotationPresent( Lob.class ) ) {
			isLob = true;
			if ( buildingContext.getBootstrapContext().getReflectionManager().equals( returnedClassOrElement, java.sql.Clob.class ) ) {
				type = isNationalized
						? StandardBasicTypes.NCLOB.getName()
						: StandardBasicTypes.CLOB.getName();
			}
			else if ( buildingContext.getBootstrapContext().getReflectionManager().equals( returnedClassOrElement, java.sql.NClob.class ) ) {
				type = StandardBasicTypes.NCLOB.getName();
			}
			else if ( buildingContext.getBootstrapContext().getReflectionManager().equals( returnedClassOrElement, java.sql.Blob.class ) ) {
				type = "blob";
			}
			else if ( buildingContext.getBootstrapContext().getReflectionManager().equals( returnedClassOrElement, String.class ) ) {
				type = isNationalized
						? StandardBasicTypes.MATERIALIZED_NCLOB.getName()
						: StandardBasicTypes.MATERIALIZED_CLOB.getName();
			}
			else if ( buildingContext.getBootstrapContext().getReflectionManager().equals( returnedClassOrElement, Character.class ) && isArray ) {
				type = isNationalized
						? CharacterArrayNClobType.class.getName()
						: CharacterArrayClobType.class.getName();
			}
			else if ( buildingContext.getBootstrapContext().getReflectionManager().equals( returnedClassOrElement, char.class ) && isArray ) {
				type = isNationalized
						? PrimitiveCharacterArrayNClobType.class.getName()
						: PrimitiveCharacterArrayClobType.class.getName();
			}
			else if ( buildingContext.getBootstrapContext().getReflectionManager().equals( returnedClassOrElement, Byte.class ) && isArray ) {
				type = WrappedMaterializedBlobType.class.getName();
			}
			else if ( buildingContext.getBootstrapContext().getReflectionManager().equals( returnedClassOrElement, byte.class ) && isArray ) {
				type = StandardBasicTypes.MATERIALIZED_BLOB.getName();
			}
			else if ( buildingContext.getBootstrapContext().getReflectionManager()
					.toXClass( Serializable.class )
					.isAssignableFrom( returnedClassOrElement ) ) {
				type = SerializableToBlobType.class.getName();
				typeParameters.setProperty(
						SerializableToBlobType.CLASS_NAME,
						returnedClassOrElement.getName()
				);
			}
			else {
				type = "blob";
			}
			defaultType = type;
		}
		else if ( ( !key && property.isAnnotationPresent( Enumerated.class ) )
				|| ( key && property.isAnnotationPresent( MapKeyEnumerated.class ) ) ) {
			final Class attributeJavaType = buildingContext.getBootstrapContext().getReflectionManager().toClass( returnedClassOrElement );
			if ( !Enum.class.isAssignableFrom( attributeJavaType ) ) {
				throw new AnnotationException(
						String.format(
								"Attribute [%s.%s] was annotated as enumerated, but its java type is not an enum [%s]",
								declaringClassName,
								xproperty.getName(),
								attributeJavaType.getName()
						)
				);
			}
			type = EnumType.class.getName();
			explicitType = type;
		}
		else if ( isNationalized ) {
			if ( buildingContext.getBootstrapContext().getReflectionManager().equals( returnedClassOrElement, String.class ) ) {
				// nvarchar
				type = StringNVarcharType.INSTANCE.getName();
				explicitType = type;
			}
			else if ( buildingContext.getBootstrapContext().getReflectionManager().equals( returnedClassOrElement, Character.class ) ||
					buildingContext.getBootstrapContext().getReflectionManager().equals( returnedClassOrElement, char.class ) ) {
				if ( isArray ) {
					// nvarchar
					type = StringNVarcharType.INSTANCE.getName();
				}
				else {
					// nchar
					type = CharacterNCharType.INSTANCE.getName();
				}
				explicitType = type;
			}
		}

		// implicit type will check basic types and Serializable classes
		if ( columns == null ) {
			throw new AssertionFailure( "SimpleValueBinder.setColumns should be set before SimpleValueBinder.setType" );
		}

		if ( BinderHelper.ANNOTATION_STRING_DEFAULT.equals( type ) ) {
			if ( returnedClassOrElement.isEnum() ) {
				type = EnumType.class.getName();
			}
		}

		defaultType = BinderHelper.isEmptyAnnotationValue( type ) ? returnedClassName : type;
		this.typeParameters = typeParameters;

		applyAttributeConverter( property, attributeConverterDescriptor );
	}

	private Dialect getDialect() {
		return buildingContext.getBuildingOptions()
				.getServiceRegistry()
				.getService( JdbcServices.class )
				.getJdbcEnvironment()
				.getDialect();
	}

	private void applyAttributeConverter(XProperty property, ConverterDescriptor attributeConverterDescriptor) {
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

		this.attributeConverterDescriptor = attributeConverterDescriptor;
	}

	private boolean isAssociation() {
		// todo : this information is only known to caller(s), need to pass that information in somehow.
		// or, is this enough?
		return referencedEntityName != null;
	}

	private TemporalType getTemporalType(XProperty property) {
		if ( key ) {
			MapKeyTemporal ann = property.getAnnotation( MapKeyTemporal.class );
			return ann.value();
		}
		else {
			Temporal ann = property.getAnnotation( Temporal.class );
			return ann.value();
		}
	}

	public void setExplicitType(String explicitType) {
		this.explicitType = explicitType;
	}

	//FIXME raise an assertion failure  if setResolvedTypeMapping(String) and setResolvedTypeMapping(Type) are use at the same time

	public void setExplicitType(Type typeAnn) {
		if ( typeAnn != null ) {
			explicitType = typeAnn.type();
			typeParameters.clear();
			for ( Parameter param : typeAnn.parameters() ) {
				typeParameters.setProperty( param.name(), param.value() );
			}
		}
	}

	public void setBuildingContext(MetadataBuildingContext buildingContext) {
		this.buildingContext = buildingContext;
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
		simpleValue = new SimpleValue( buildingContext, table );
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

		if ( attributeConverterDescriptor != null ) {
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
					attributeConverterDescriptor,
					persistentClassName,
					propertyName
			);
			simpleValue.setJpaAttributeConverterDescriptor( attributeConverterDescriptor );
		}
		else {
			String type;
			TypeDefinition typeDef;

			if ( !BinderHelper.isEmptyAnnotationValue( explicitType ) ) {
				type = explicitType;
				typeDef = buildingContext.getMetadataCollector().getTypeDefinition( type );
			}
			else {
				// try implicit type
				TypeDefinition implicitTypeDef = buildingContext.getMetadataCollector().getTypeDefinition( returnedClassName );
				if ( implicitTypeDef != null ) {
					typeDef = implicitTypeDef;
					type = returnedClassName;
				}
				else {
					typeDef = buildingContext.getMetadataCollector().getTypeDefinition( defaultType );
					type = defaultType;
				}
			}

			if ( typeDef != null ) {
				type = typeDef.getTypeImplementorClass().getName();
				simpleValue.setTypeParameters( typeDef.getParametersAsProperties() );
			}
			if ( typeParameters != null && typeParameters.size() != 0 ) {
				//explicit type params takes precedence over type def params
				simpleValue.setTypeParameters( typeParameters );
			}
			simpleValue.setTypeName( type );
		}

		if ( persistentClassName != null || attributeConverterDescriptor != null ) {
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
				&& simpleValue.getMetadata().getTypeResolver().basic( simpleValue.getTypeName() ) == null ) {
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
}
