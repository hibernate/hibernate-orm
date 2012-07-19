/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.cfg.annotations;

import java.io.Serializable;
import java.lang.reflect.TypeVariable;
import java.sql.Types;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import javax.persistence.AttributeConverter;
import javax.persistence.Convert;
import javax.persistence.Converts;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.MapKeyEnumerated;
import javax.persistence.MapKeyTemporal;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.jboss.logging.Logger;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.annotations.common.util.ReflectHelper;
import org.hibernate.cfg.AttributeConverterDefinition;
import org.hibernate.cfg.BinderHelper;
import org.hibernate.cfg.Ejb3Column;
import org.hibernate.cfg.Ejb3JoinColumn;
import org.hibernate.cfg.Mappings;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.cfg.PkDrivenByDefaultMapsIdSecondPass;
import org.hibernate.cfg.SetSimpleValueTypeSecondPass;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.type.CharacterArrayClobType;
import org.hibernate.type.EnumType;
import org.hibernate.type.PrimitiveCharacterArrayClobType;
import org.hibernate.type.SerializableToBlobType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.WrappedMaterializedBlobType;

/**
 * @author Emmanuel Bernard
 */
public class SimpleValueBinder {
    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, SimpleValueBinder.class.getName());

	private String propertyName;
	private String returnedClassName;
	private Ejb3Column[] columns;
	private String persistentClassName;
	private String explicitType = "";
	private Properties typeParameters = new Properties();
	private Mappings mappings;
	private Table table;
	private SimpleValue simpleValue;
	private boolean isVersion;
	private String timeStampVersionType;
	//is a Map key
	private boolean key;
	private String referencedEntityName;

	private AttributeConverterDefinition attributeConverterDefinition;

	public void setReferencedEntityName(String referencedEntityName) {
		this.referencedEntityName = referencedEntityName;
	}

	public boolean isVersion() {
		return isVersion;
	}

	public void setVersion(boolean isVersion) {
		this.isVersion = isVersion;
	}

	public void setTimestampVersionType(String versionType) {
		this.timeStampVersionType = versionType;
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

	//TODO execute it lazily to be order safe

	public void setType(XProperty property, XClass returnedClass) {
		if ( returnedClass == null ) {
			return;
		} //we cannot guess anything
		XClass returnedClassOrElement = returnedClass;
		boolean isArray = false;
		if ( property.isArray() ) {
			returnedClassOrElement = property.getElementClass();
			isArray = true;
		}
		Properties typeParameters = this.typeParameters;
		typeParameters.clear();
		String type = BinderHelper.ANNOTATION_STRING_DEFAULT;
		if ( ( !key && property.isAnnotationPresent( Temporal.class ) )
				|| ( key && property.isAnnotationPresent( MapKeyTemporal.class ) ) ) {

			boolean isDate;
			if ( mappings.getReflectionManager().equals( returnedClassOrElement, Date.class ) ) {
				isDate = true;
			}
			else if ( mappings.getReflectionManager().equals( returnedClassOrElement, Calendar.class ) ) {
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
		}
		else if ( property.isAnnotationPresent( Lob.class ) ) {
			if ( mappings.getReflectionManager().equals( returnedClassOrElement, java.sql.Clob.class ) ) {
				type = "clob";
			}
			else if ( mappings.getReflectionManager().equals( returnedClassOrElement, java.sql.Blob.class ) ) {
				type = "blob";
			}
			else if ( mappings.getReflectionManager().equals( returnedClassOrElement, String.class ) ) {
				type = StandardBasicTypes.MATERIALIZED_CLOB.getName();
			}
			else if ( mappings.getReflectionManager().equals( returnedClassOrElement, Character.class ) && isArray ) {
				type = CharacterArrayClobType.class.getName();
			}
			else if ( mappings.getReflectionManager().equals( returnedClassOrElement, char.class ) && isArray ) {
				type = PrimitiveCharacterArrayClobType.class.getName();
			}
			else if ( mappings.getReflectionManager().equals( returnedClassOrElement, Byte.class ) && isArray ) {
				type = WrappedMaterializedBlobType.class.getName();
			}
			else if ( mappings.getReflectionManager().equals( returnedClassOrElement, byte.class ) && isArray ) {
				type = StandardBasicTypes.MATERIALIZED_BLOB.getName();
			}
			else if ( mappings.getReflectionManager()
					.toXClass( Serializable.class )
					.isAssignableFrom( returnedClassOrElement ) ) {
				type = SerializableToBlobType.class.getName();
				//typeParameters = new Properties();
				typeParameters.setProperty(
						SerializableToBlobType.CLASS_NAME,
						returnedClassOrElement.getName()
				);
			}
			else {
				type = "blob";
			}
		}
		//implicit type will check basic types and Serializable classes
		if ( columns == null ) {
			throw new AssertionFailure( "SimpleValueBinder.setColumns should be set before SimpleValueBinder.setType" );
		}
		if ( BinderHelper.ANNOTATION_STRING_DEFAULT.equals( type ) ) {
			if ( returnedClassOrElement.isEnum() ) {
				type = EnumType.class.getName();
				typeParameters = new Properties();
				typeParameters.setProperty( EnumType.ENUM, returnedClassOrElement.getName() );
				String schema = columns[0].getTable().getSchema();
				schema = schema == null ? "" : schema;
				String catalog = columns[0].getTable().getCatalog();
				catalog = catalog == null ? "" : catalog;
				typeParameters.setProperty( EnumType.SCHEMA, schema );
				typeParameters.setProperty( EnumType.CATALOG, catalog );
				typeParameters.setProperty( EnumType.TABLE, columns[0].getTable().getName() );
				typeParameters.setProperty( EnumType.COLUMN, columns[0].getName() );
				javax.persistence.EnumType enumType = getEnumType( property );
				if ( enumType != null ) {
					if ( javax.persistence.EnumType.ORDINAL.equals( enumType ) ) {
						typeParameters.setProperty( EnumType.TYPE, String.valueOf( Types.INTEGER ) );
					}
					else if ( javax.persistence.EnumType.STRING.equals( enumType ) ) {
						typeParameters.setProperty( EnumType.TYPE, String.valueOf( Types.VARCHAR ) );
					}
					else {
						throw new AssertionFailure( "Unknown EnumType: " + enumType );
					}
				}
			}
		}
		explicitType = type;
		this.typeParameters = typeParameters;
		Type annType = property.getAnnotation( Type.class );
		setExplicitType( annType );

		applyAttributeConverter( property );
	}

	private void applyAttributeConverter(XProperty property) {
		final boolean canBeConverted = ! property.isAnnotationPresent( Id.class )
				&& ! isVersion
				&& ! isAssociation()
				&& ! property.isAnnotationPresent( Temporal.class )
				&& ! property.isAnnotationPresent( Enumerated.class );

		if ( canBeConverted ) {
			// @Convert annotations take precedence
			final Convert convertAnnotation = locateConvertAnnotation( property );
			if ( convertAnnotation != null ) {
				if ( ! convertAnnotation.disableConversion() ) {
					attributeConverterDefinition = mappings.locateAttributeConverter( convertAnnotation.converter() );
				}
			}
			else {
				attributeConverterDefinition = locateAutoApplyAttributeConverter( property );
			}
		}
	}

	private AttributeConverterDefinition locateAutoApplyAttributeConverter(XProperty property) {
		final Class propertyType = mappings.getReflectionManager().toClass( property.getType() );
		for ( AttributeConverterDefinition attributeConverterDefinition : mappings.getAttributeConverters() ) {
			if ( areTypeMatch( attributeConverterDefinition.getEntityAttributeType(), propertyType ) ) {
				return attributeConverterDefinition;
			}
		}
		return null;
	}

	private boolean isAssociation() {
		// todo : this information is only known to caller(s), need to pass that information in somehow.
		// or, is this enough?
		return referencedEntityName != null;
	}

	@SuppressWarnings("unchecked")
	private Convert locateConvertAnnotation(XProperty property) {
		// first look locally on the property for @Convert
		Convert localConvertAnnotation = property.getAnnotation( Convert.class );
		if ( localConvertAnnotation != null ) {
			return localConvertAnnotation;
		}

		if ( persistentClassName == null ) {
			LOG.debug( "Persistent Class name not known during attempt to locate @Convert annotations" );
			return null;
		}

		final XClass owner;
		try {
			final Class ownerClass = ReflectHelper.classForName( persistentClassName );
			owner = mappings.getReflectionManager().classForName( persistentClassName, ownerClass  );
		}
		catch (ClassNotFoundException e) {
			throw new AnnotationException( "Unable to resolve Class reference during attempt to locate @Convert annotations" );
		}

		return lookForEntityDefinedConvertAnnotation( property, owner );
	}

	private Convert lookForEntityDefinedConvertAnnotation(XProperty property, XClass owner) {
		if ( owner == null ) {
			// we have hit the root of the entity hierarchy
			return null;
		}

		{
			Convert convertAnnotation = owner.getAnnotation( Convert.class );
			if ( convertAnnotation != null && isMatch( convertAnnotation, property ) ) {
				return convertAnnotation;
			}
		}

		{
			Converts convertsAnnotation = owner.getAnnotation( Converts.class );
			if ( convertsAnnotation != null ) {
				for ( Convert convertAnnotation : convertsAnnotation.value() ) {
					if ( isMatch( convertAnnotation, property ) ) {
						return convertAnnotation;
					}
				}
			}
		}

		// finally, look on superclass
		return lookForEntityDefinedConvertAnnotation( property, owner.getSuperclass() );
	}

	@SuppressWarnings("unchecked")
	private boolean isMatch(Convert convertAnnotation, XProperty property) {
		return property.getName().equals( convertAnnotation.attributeName() )
				&& isTypeMatch( convertAnnotation.converter(), property );
	}

	private boolean isTypeMatch(Class<? extends AttributeConverter> attributeConverterClass, XProperty property) {
		return areTypeMatch(
				extractEntityAttributeType( attributeConverterClass ),
				mappings.getReflectionManager().toClass( property.getType() )
		);
	}

	private Class extractEntityAttributeType(Class<? extends AttributeConverter> attributeConverterClass) {
		// this is duplicated in SimpleValue...
		final TypeVariable[] attributeConverterTypeInformation = attributeConverterClass.getTypeParameters();
		if ( attributeConverterTypeInformation == null || attributeConverterTypeInformation.length < 2 ) {
			throw new AnnotationException(
					"AttributeConverter [" + attributeConverterClass.getName()
							+ "] did not retain parameterized type information"
			);
		}

		if ( attributeConverterTypeInformation.length > 2 ) {
			LOG.debug(
					"AttributeConverter [" + attributeConverterClass.getName()
							+ "] specified more than 2 parameterized types"
			);
		}
		final Class entityAttributeJavaType = extractType( attributeConverterTypeInformation[0] );
		if ( entityAttributeJavaType == null ) {
			throw new AnnotationException(
					"Could not determine 'entity attribute' type from given AttributeConverter [" +
							attributeConverterClass.getName() + "]"
			);
		}
		return entityAttributeJavaType;
	}

	private Class extractType(TypeVariable typeVariable) {
		java.lang.reflect.Type[] boundTypes = typeVariable.getBounds();
		if ( boundTypes == null || boundTypes.length != 1 ) {
			return null;
		}

		return (Class) boundTypes[0];
	}

	private boolean areTypeMatch(Class converterDefinedType, Class propertyType) {
		if ( converterDefinedType == null ) {
			throw new AnnotationException( "AttributeConverter defined java type cannot be null" );
		}
		if ( propertyType == null ) {
			throw new AnnotationException( "Property defined java type cannot be null" );
		}

		return converterDefinedType.equals( propertyType )
				|| arePrimitiveWrapperEquivalents( converterDefinedType, propertyType );
	}

	private boolean arePrimitiveWrapperEquivalents(Class converterDefinedType, Class propertyType) {
		if ( converterDefinedType.isPrimitive() ) {
			return getWrapperEquivalent( converterDefinedType ).equals( propertyType );
		}
		else if ( propertyType.isPrimitive() ) {
			return getWrapperEquivalent( propertyType ).equals( converterDefinedType );
		}
		return false;
	}

	private static Class getWrapperEquivalent(Class primitive) {
		if ( ! primitive.isPrimitive() ) {
			throw new AssertionFailure( "Passed type for which to locate wrapper equivalent was not a primitive" );
		}

		if ( boolean.class.equals( primitive ) ) {
			return Boolean.class;
		}
		else if ( char.class.equals( primitive ) ) {
			return Character.class;
		}
		else if ( byte.class.equals( primitive ) ) {
			return Byte.class;
		}
		else if ( short.class.equals( primitive ) ) {
			return Short.class;
		}
		else if ( int.class.equals( primitive ) ) {
			return Integer.class;
		}
		else if ( long.class.equals( primitive ) ) {
			return Long.class;
		}
		else if ( float.class.equals( primitive ) ) {
			return Float.class;
		}
		else if ( double.class.equals( primitive ) ) {
			return Double.class;
		}

		throw new AssertionFailure( "Unexpected primitive type (VOID most likely) passed to getWrapperEquivalent" );
	}

	private javax.persistence.EnumType getEnumType(XProperty property) {
		javax.persistence.EnumType enumType = null;
		if ( key ) {
			MapKeyEnumerated enumAnn = property.getAnnotation( MapKeyEnumerated.class );
			if ( enumAnn != null ) {
				enumType = enumAnn.value();
			}
		}
		else {
			Enumerated enumAnn = property.getAnnotation( Enumerated.class );
			if ( enumAnn != null ) {
				enumType = enumAnn.value();
			}
		}
		return enumType;
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

	public void setMappings(Mappings mappings) {
		this.mappings = mappings;
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
		simpleValue = new SimpleValue( mappings, table );

		linkWithValue();

		boolean isInSecondPass = mappings.isInSecondPass();
		SetSimpleValueTypeSecondPass secondPass = new SetSimpleValueTypeSecondPass( this );
		if ( !isInSecondPass ) {
			//Defer this to the second pass
			mappings.addSecondPass( secondPass );
		}
		else {
			//We are already in second pass
			fillSimpleValue();
		}
		return simpleValue;
	}

	public void linkWithValue() {
		if ( columns[0].isNameDeferred() && !mappings.isInSecondPass() && referencedEntityName != null ) {
			mappings.addSecondPass(
					new PkDrivenByDefaultMapsIdSecondPass(
							referencedEntityName, ( Ejb3JoinColumn[] ) columns, simpleValue
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
		LOG.debugf( "Setting SimpleValue typeName for %s", propertyName );

		if ( attributeConverterDefinition != null ) {
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
			simpleValue.setJpaAttributeConverterDefinition( attributeConverterDefinition );
		}
		else {
			String type = BinderHelper.isEmptyAnnotationValue( explicitType ) ? returnedClassName : explicitType;
			org.hibernate.mapping.TypeDef typeDef = mappings.getTypeDef( type );
			if ( typeDef != null ) {
				type = typeDef.getTypeClass();
				simpleValue.setTypeParameters( typeDef.getParameters() );
			}
			if ( typeParameters != null && typeParameters.size() != 0 ) {
				//explicit type params takes precedence over type def params
				simpleValue.setTypeParameters( typeParameters );
			}
			simpleValue.setTypeName( type );
		}

		if ( persistentClassName != null || attributeConverterDefinition != null ) {
			simpleValue.setTypeUsingReflection( persistentClassName, propertyName );
		}

		if ( !simpleValue.isTypeSpecified() && isVersion() ) {
			simpleValue.setTypeName( "integer" );
		}

		// HHH-5205
		if ( timeStampVersionType != null ) {
			simpleValue.setTypeName( timeStampVersionType );
		}
	}

	public void setKey(boolean key) {
		this.key = key;
	}

}
