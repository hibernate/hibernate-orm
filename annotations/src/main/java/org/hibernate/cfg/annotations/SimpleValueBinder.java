/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
import java.sql.Types;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import javax.persistence.Enumerated;
import javax.persistence.Lob;
import javax.persistence.MapKeyEnumerated;
import javax.persistence.MapKeyTemporal;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.cfg.BinderHelper;
import org.hibernate.cfg.Ejb3Column;
import org.hibernate.cfg.Ejb3JoinColumn;
import org.hibernate.cfg.ExtendedMappings;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.cfg.PkDrivenByDefaultMapsIdSecondPass;
import org.hibernate.cfg.SetSimpleValueTypeSecondPass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.type.CharacterArrayClobType;
import org.hibernate.type.EnumType;
import org.hibernate.type.PrimitiveCharacterArrayClobType;
import org.hibernate.type.SerializableToBlobType;
import org.hibernate.type.WrappedMaterializedBlobType;
import org.hibernate.util.StringHelper;

/**
 * @author Emmanuel Bernard
 */
public class SimpleValueBinder {
	private Logger log = LoggerFactory.getLogger( SimpleValueBinder.class );
	private String propertyName;
	private String returnedClassName;
	private Ejb3Column[] columns;
	private String persistentClassName;
	private String explicitType = "";
	private Properties typeParameters = new Properties();
	private ExtendedMappings mappings;
	private Table table;
	private SimpleValue simpleValue;
	private boolean isVersion;
	//is a Map key
	private boolean key;
	private String referencedEntityName;

	public void setReferencedEntityName(String referencedEntityName) {
		this.referencedEntityName = referencedEntityName;
	}

	public boolean isVersion() {
		return isVersion;
	}

	public void setVersion(boolean isVersion) {
		this.isVersion = isVersion;
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
		if ( returnedClass == null ) return; //we cannot guess anything
		XClass returnedClassOrElement = returnedClass;
		boolean isArray = false;
		if ( property.isArray() ) {
			returnedClassOrElement = property.getElementClass();
			isArray = true;
		}
		Properties typeParameters = this.typeParameters;
		typeParameters.clear();
		String type = BinderHelper.ANNOTATION_STRING_DEFAULT;
		if ( (!key && property.isAnnotationPresent( Temporal.class ) ) 
				|| (key && property.isAnnotationPresent( MapKeyTemporal.class ) )) {

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
				type = Hibernate.MATERIALIZED_CLOB.getName();
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
				type = Hibernate.MATERIALIZED_BLOB.getName();
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
	}

	private javax.persistence.EnumType getEnumType(XProperty property) {
		javax.persistence.EnumType enumType = null;
		if (key) {
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
		if (key) {
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
	
	//FIXME raise an assertion failure  if setExplicitType(String) and setExplicitType(Type) are use at the same time
	public void setExplicitType(Type typeAnn) {
		if ( typeAnn != null ) {
			explicitType = typeAnn.type();
			typeParameters.clear();
			for (Parameter param : typeAnn.parameters()) {
				typeParameters.setProperty( param.name(), param.value() );
			}
		}
	}

	public void setMappings(ExtendedMappings mappings) {
		this.mappings = mappings;
	}

	private void validate() {
		//TODO check necessary params
		Ejb3Column.checkPropertyConsistency( columns, propertyName );
	}

	public SimpleValue make() {
				
		validate();
		log.debug( "building SimpleValue for {}", propertyName );
		if ( table == null ) {
			table = columns[0].getTable();
		}
		simpleValue = new SimpleValue( table );

		linkWithValue();

		boolean isInSecondPass = mappings.isInSecondPass();
		SetSimpleValueTypeSecondPass secondPass = new SetSimpleValueTypeSecondPass(this);
		if (!isInSecondPass) {
			//Defer this to the second pass
			mappings.addSecondPass(secondPass);
		}
		else {
			//We are already in second pass
			fillSimpleValue();
		}
		return simpleValue;
	}

	public void linkWithValue() {
		if ( columns[0].isNameDeferred() && ! mappings.isInSecondPass() && referencedEntityName != null) {
			mappings.addSecondPass(
					new PkDrivenByDefaultMapsIdSecondPass( referencedEntityName, ( Ejb3JoinColumn[]) columns, simpleValue)
			);
		}
		else {
			for ( Ejb3Column column : columns) {
				column.linkWithValue( simpleValue );
			}
		}
	}

	public void fillSimpleValue() {
				
		log.debug( "setting SimpleValue typeName for {}", propertyName );
				
		String type = BinderHelper.isDefault( explicitType ) ? returnedClassName : explicitType;
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
		if ( persistentClassName != null ) {
			simpleValue.setTypeUsingReflection( persistentClassName, propertyName );
		}
		
		if ( !simpleValue.isTypeSpecified() && isVersion()) {
			simpleValue.setTypeName( "integer" );
		}
				
	}

	public void setKey(boolean key) {
		this.key = key;
	}
}
