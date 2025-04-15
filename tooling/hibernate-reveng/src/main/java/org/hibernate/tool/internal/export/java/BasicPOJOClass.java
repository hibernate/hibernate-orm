/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2010-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.internal.export.java;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Array;
import org.hibernate.mapping.Bag;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.IdentifierBag;
import org.hibernate.mapping.MetaAttributable;
import org.hibernate.mapping.MetaAttribute;
import org.hibernate.mapping.PrimitiveArray;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Set;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Value;
import org.hibernate.tool.internal.export.common.DefaultValueVisitor;
import org.hibernate.tool.internal.util.NameConverter;

/**
 * Abstract implementation of POJOClass. To be extended by ComponentPOJO and EntityPOJO
 * @author max
 * @author <a href="mailto:abhayani@jboss.org">Amit Bhayani</a>
 *
 */
abstract public class BasicPOJOClass implements POJOClass, MetaAttributeConstants {

	protected ImportContext importContext;
	protected MetaAttributable meta;
	protected final Cfg2JavaTool c2j;
	
	public BasicPOJOClass(MetaAttributable ma, Cfg2JavaTool c2j) {
		this.meta = ma;
		this.c2j = c2j;		
		
		if(this.meta==null) {
			throw new IllegalArgumentException("class Argument must be not null");
		}
		if(this.c2j==null) throw new IllegalArgumentException("c2j must be not null");
	}
	
	// called by subclasses
	protected void init() {
		importContext = new ImportContextImpl(getPackageName());
		
		MetaAttribute metaAttribute = meta.getMetaAttribute("extra-import");
		if(metaAttribute!=null) {
			Iterator<?> values = metaAttribute.getValues().iterator();
			while ( values.hasNext() ) {
				String element = (String) values.next();
				importContext.importType(element);				
			}
		}	
	}
	
	protected String getPackageDeclaration(String pkgName) {
		if (pkgName!=null && pkgName.trim().length()!=0 ) {
			return "package " + pkgName + ";";
		} 
		else {        
			return "// default package";
		}
	}

	public String getPackageDeclaration() {
		String pkgName = getPackageName();
		return getPackageDeclaration(pkgName);			
	}

	/** Return package name. Note: Does not handle inner classes */ 
	public String getPackageName() {
		String generatedClass = getGeneratedClassName();
		return StringHelper.qualifier(generatedClass.trim());
	}
	
	public String getShortName() {
		return qualifyInnerClass(StringHelper.unqualify(getMappedClassName()));
	}
	
	public String getQualifiedDeclarationName() {
		String generatedName = qualifyInnerClass(getGeneratedClassName());
		String qualifier = StringHelper.qualifier( getMappedClassName() );
		if ( !"".equals( qualifier ) ) {
			return qualifier + "." + generatedName;
		}
		else {
			return generatedName;
		}
	}
	
	/**
	 * @return unqualified classname for this class (can be changed by meta attribute "generated-class")
	 */
	public String getDeclarationName() {
		return qualifyInnerClass(StringHelper.unqualify( getGeneratedClassName() ));
	}
	
	protected String getGeneratedClassName()
	{
		String generatedClass = getMetaAsString(MetaAttributeConstants.GENERATED_CLASS).trim();
		if(StringHelper.isEmpty(generatedClass) ) {
			generatedClass = getMappedClassName();
		}
		if(generatedClass==null) return ""; // will occur for <dynamic-component>
		return generatedClass;
	}
	
	protected String qualifyInnerClass(String className)
	{
		return className.replace('$', '.');
	}
	
	protected abstract String getMappedClassName();

	public String getMetaAsString(String attribute) {
		MetaAttribute c = meta.getMetaAttribute( attribute );
		return MetaAttributeHelper.getMetaAsString( c );
	}

	public boolean hasMetaAttribute(String attribute) {
		return meta.getMetaAttribute( attribute ) != null;
	}

	public String getMetaAsString(String attribute, String seperator) {
		return MetaAttributeHelper.getMetaAsString( meta.getMetaAttribute( attribute ), seperator );
	}

	public boolean getMetaAsBool(String attribute) {
		return getMetaAsBool( attribute, false );
	}

	public boolean getMetaAsBool(String attribute, boolean defaultValue) {
		return MetaAttributeHelper.getMetaAsBool( meta.getMetaAttribute( attribute ), defaultValue );
	}

	public String getClassJavaDoc(String fallback, int indent) {
		MetaAttribute c = meta.getMetaAttribute( CLASS_DESCRIPTION );
		if ( c == null ) {
			return c2j.toJavaDoc( fallback, indent );
		}
		else {
			return c2j.toJavaDoc( getMetaAsString( CLASS_DESCRIPTION ), indent );
		}
	}
	
	public String getClassModifiers() {
		String classModifiers = null;

		// Get scope (backwards compatibility)
		if ( meta.getMetaAttribute( SCOPE_CLASS ) != null ) {
			classModifiers = getMetaAsString( SCOPE_CLASS ).trim();
		}

		// Get modifiers
		if ( meta.getMetaAttribute( CLASS_MODIFIER ) != null ) {
			classModifiers = getMetaAsString( CLASS_MODIFIER ).trim();
		}
		return classModifiers == null ? "public" : classModifiers;
	}

	public String getDeclarationType() {
		boolean isInterface = isInterface();
		if ( isInterface ) {
			return INTERFACE;
		}
		else {
			return "class";
		}
	}
	
	public boolean isInterface() {
		return getMetaAsBool( INTERFACE );
	}
	
	public String getExtendsDeclaration() {
		String extendz = getExtends();
		if ( extendz == null || extendz.trim().length() == 0 ) {
			return "";
		}
		else {
			return "extends " + extendz;
		}
	}

	public String getImplementsDeclaration() {
		String implementz = getImplements();
		if ( implementz == null || implementz.trim().length() == 0 ) {
			return "";
		}
		else {
			return "implements " + implementz;
		}
	}
	
	public String generateEquals(String thisName, String otherName, boolean useGenerics) {
		Iterator<Property> allPropertiesIterator = getEqualsHashCodePropertiesIterator();
		return generateEquals( thisName, otherName, allPropertiesIterator, useGenerics );
	}
	
	/** returns the properties that would be visible on this entity as a pojo. This does not return *all* properties since hibernate has certain properties that are only relevant in context of persistence. */ 
	public abstract Iterator<Property> getAllPropertiesIterator();

	protected String generateEquals(String thisName, String otherName, Iterator<Property> allPropertiesIterator, boolean useGenerics) {
		StringBuffer buf = new StringBuffer();
		while ( allPropertiesIterator.hasNext() ) {
			Property property = (Property) allPropertiesIterator.next();
				if ( buf.length() > 0 ) buf.append( "\n && " );
				String javaTypeName = c2j.getJavaTypeName( property, useGenerics, this );
				buf.append(
						internalgenerateEquals(
								javaTypeName, thisName + "." + getGetterSignature( property ) + "()",
								otherName + "." + getGetterSignature( property ) + "()")
				);			
		}

		if ( buf.length() == 0 ) {
			return "false";
		}
		else {
			return buf.toString();
		}
	}

	private boolean usePropertyInEquals(Property property) {
		boolean hasEqualsMetaAttribute = c2j.hasMetaAttribute(property, "use-in-equals");		
		boolean useInEquals = c2j.getMetaAsBool( property, "use-in-equals" );
		
		if(property.isNaturalIdentifier()) {
			if(hasEqualsMetaAttribute && !useInEquals) {
				return false;
			} else {
				return true;
			}
		} 
		
		return useInEquals;
	}

	private boolean useCompareTo(String javaTypeName) {
		// Fix for HBX-400
		if ("java.math.BigDecimal".equals(javaTypeName)) {
			return true;
		} else {
			return false;
		}
	} 


	private String internalgenerateEquals(String typeName, String lh, String rh) {
		if ( c2j.isPrimitive( typeName ) ) {
			return "(" + lh + "==" + rh + ")";
		}
		else {
			if(useCompareTo( typeName )) {
				return "( (" + lh + "==" + rh + ") || ( " + lh + "!=null && " + rh + "!=null && " + lh + ".compareTo(" + rh + ")==0 ) )";
			} else {
				if(typeName.endsWith("[]")) {
					return "( (" + lh + "==" + rh + ") || ( " + lh + "!=null && " + rh + "!=null && " + importType("java.util.Arrays") + ".equals(" + lh + ", " + rh + ") ) )";
				} else {
					return "( (" + lh + "==" + rh + ") || ( " + lh + "!=null && " + rh + "!=null && " + lh + ".equals(" + rh + ") ) )";
				}
			}

		}
	}

	public String getExtraClassCode() {
		return getMetaAsString( "class-code", "\n" );
	}
	
	private boolean needsEqualsHashCode(Iterator<?> iter) {
		while ( iter.hasNext() ) {
			Property element = (Property) iter.next();
			if ( usePropertyInEquals( element ) ) {
				return true;
			}
		}
		return false;
	}

	public boolean needsEqualsHashCode() {
		Iterator<Property> iter = getAllPropertiesIterator();
		return needsEqualsHashCode( iter );
	}

	public abstract String getExtends();
	
	public abstract String getImplements();

	
	public String importType(String fqcn) {
		return importContext.importType(fqcn);
	}
	
	public String generateImports() {
		return importContext.generateImports();
	}

	public String staticImport(String fqcn, String member) {
		return importContext.staticImport(fqcn, member);
	}
	
	public String generateBasicAnnotation(Property property) {
		StringBuffer annotations = new StringBuffer( "    " );
		if(property.getValue() instanceof SimpleValue) {
			if (hasVersionProperty())
				if (property.equals(getVersionProperty()))
						buildVersionAnnotation(annotations);
			String typeName = ((SimpleValue)property.getValue()).getTypeName();
			if("date".equals(typeName) || "java.sql.Date".equals(typeName)) {
				buildTemporalAnnotation( annotations, "DATE" );
			} else if ("timestamp".equals(typeName) || "java.sql.Timestamp".equals(typeName)) {
				buildTemporalAnnotation( annotations, "TIMESTAMP" );
			} else if ("time".equals(typeName) || "java.sql.Time".equals(typeName)) {
				buildTemporalAnnotation(annotations, "TIME");
			} //TODO: calendar etc. ?

						
		}
			
		return annotations.toString();
	}

	private StringBuffer buildTemporalAnnotation(StringBuffer annotations, String temporalTypeValue) {
		String temporal = importType("jakarta.persistence.Temporal");
		String temporalType = importType("jakarta.persistence.TemporalType");
		
		return annotations.append( "@" + temporal +"(" + temporalType + "." + temporalTypeValue + ")");
	}
	
	private StringBuffer buildVersionAnnotation(StringBuffer annotations) {
		String version = importType("jakarta.persistence.Version");
		
		return annotations.append( "@" + version );
	}
	
	public String generateAnnColumnAnnotation(Property property) {
		StringBuffer annotations = new StringBuffer( "    " );
		boolean insertable = property.isInsertable();
		boolean updatable = property.isUpdateable();
		if ( property.isComposite() ) {
			annotations.append( "@" + importType("jakarta.persistence.AttributeOverrides") +"( {" );
			Component component = (Component) property.getValue();
			Iterator<?> subElements = component.getProperties().iterator();
			buildRecursiveAttributeOverride( subElements, null, property, annotations );
			annotations.setLength( annotations.length() - 2 );
			annotations.append( " } )" );
		}
		else {
			if ( property.getColumnSpan() == 1 ) {
				Selectable selectable = property.getColumns().get(0);
				buildColumnAnnotation( selectable, annotations, insertable, updatable );				
			}
			else {
				annotations.append("@").append( importType("org.hibernate.annotations.Columns") ).append("( { " );
				for (Selectable selectable : property.getColumns()) {
					if ( selectable.isFormula() ) {
						//TODO formula in multicolumns not supported by annotations
						//annotations.append("/* TODO formula in multicolumns not supported by annotations */");
					}
					else {
						annotations.append( "\n        " );
						buildColumnAnnotation( selectable, annotations, insertable, updatable );
						annotations.append( ", " );
					}
				}
				annotations.setLength( annotations.length() - 2 );
				annotations.append( " } )" );
			}
		}
		return annotations.toString();
	}

	private void buildRecursiveAttributeOverride(Iterator<?> subElements, String path, Property property, StringBuffer annotations) {
		while ( subElements.hasNext() ) {
			Property subProperty = (Property) subElements.next();
			if ( subProperty.isComposite() ) {
				if ( path != null ) {
					path = path + ".";
				}
				else {
					path = "";
				}
				path = path + subProperty.getName();
				Component component = (Component) subProperty.getValue();
				buildRecursiveAttributeOverride( component.getProperties().iterator(), path, subProperty, annotations );
			}
			else {
				Selectable selectable = subProperty.getColumns().get(0);
				if ( selectable.isFormula() ) {
					//TODO formula in multicolumns not supported by annotations
				}
				else {
					annotations.append( "\n        " ).append("@")
							.append( importType("jakarta.persistence.AttributeOverride") ).append("(name=\"" );
					if ( path != null ) {
						annotations.append( path ).append( "." );
					}
					annotations.append( subProperty.getName() ).append( "\"" )
							.append( ", column=" );
					buildColumnAnnotation(
							selectable, annotations, subProperty.isInsertable(), subProperty.isUpdateable()
					);
					annotations.append( " ), " );
				}
			}
		}
	}

	private void buildColumnAnnotation(Selectable selectable, StringBuffer annotations, boolean insertable, boolean updatable) {
		if ( selectable.isFormula() ) {
			annotations.append("@").append( importType("org.hibernate.annotations.Formula") )
					.append("(value=\"" ).append( selectable.getText() ).append( "\")" );
		}
		else {
			Column column = (Column) selectable;
			annotations.append( "@" + importType("jakarta.persistence.Column") + "(name=\"" ).append( column.getName() ).append( "\"" );
			
			appendCommonColumnInfo( annotations, column, insertable, updatable );
			
			if (column.getPrecision() != null) {
				annotations.append( ", precision=" ).append( column.getPrecision() );
			}
			if (column.getScale() != null) { 
				annotations.append( ", scale=" ).append( column.getScale() );
			}
			else if (column.getLength() != null){ 
				annotations.append( ", length=" ).append( column.getLength() );
			}
			
					
					
			
			//TODO support secondary table
			annotations.append( ")" );
		}
	}

	protected void appendCommonColumnInfo(StringBuffer annotations, Column column, boolean insertable, boolean updatable) {
		if(column.isUnique()) {
				annotations.append( ", unique=" ).append( column.isUnique() );
		}
		if(!column.isNullable()) {
				annotations.append( ", nullable=" ).append( column.isNullable() );
		}
		
		if(!insertable) {
				annotations.append( ", insertable=" ).append( insertable );
		}
		
		if(!updatable) {
				annotations.append( ", updatable=" ).append( updatable );
		}
		
		String sqlType = column.getSqlType();
		if ( StringHelper.isNotEmpty( sqlType ) ) {
			annotations.append( ", columnDefinition=\"" ).append( sqlType ).append( "\"" );
		}
				
	}


	public Iterator<Property> getToStringPropertiesIterator() {
		Iterator<Property> iter = getAllPropertiesIterator();
		return getToStringPropertiesIterator( iter );
	}

	private Iterator<Property> getToStringPropertiesIterator(Iterator<Property> iter) {
		List<Property> properties = new ArrayList<Property>();

		while ( iter.hasNext() ) {
			Property element = (Property) iter.next();
			if ( c2j.getMetaAsBool( element, "use-in-tostring" ) ) {
				properties.add( element );
			}
		}

		return properties.iterator();
	}

	public Iterator<Property> getEqualsHashCodePropertiesIterator() {
		Iterator<Property> iter = getAllPropertiesIterator();
		return getEqualsHashCodePropertiesIterator(iter);
	}

	private Iterator<Property> getEqualsHashCodePropertiesIterator(Iterator<Property> iter) {
		List<Property> properties = new ArrayList<Property>();

		while ( iter.hasNext() ) {
			Property element = (Property) iter.next();
			if ( usePropertyInEquals(element) ) {
				properties.add( element );
			}
		}

		return properties.iterator();
	}

	public boolean needsToString() {
		Iterator<Property> iter = getAllPropertiesIterator();
		return needsToString( iter );
	}
	
	private boolean needsToString(Iterator<Property> iter) {
		while ( iter.hasNext() ) {
			Property element = (Property) iter.next();
			if ( c2j.getMetaAsBool( element, "use-in-tostring" ) ) {
				return true;
			}
		}
		return false;
	}

	public boolean hasMetaAttribute(MetaAttributable pc, String attribute) {
		return pc.getMetaAttribute( attribute ) != null;
	}

	public boolean getMetaAttribAsBool(MetaAttributable pc, String attribute, boolean defaultValue) {
		return MetaAttributeHelper.getMetaAsBool( pc.getMetaAttribute( attribute ), defaultValue );
	}
	
	public boolean hasFieldJavaDoc(Property property) {
		return property.getMetaAttribute("field-description")!=null;
	}
	
	public String getFieldJavaDoc(Property property, int indent) {
		MetaAttribute c = property.getMetaAttribute( "field-description" );
		if ( c == null ) {
			return c2j.toJavaDoc( "", indent );
		}
		else {
			return c2j.toJavaDoc( c2j.getMetaAsString( property, "field-description" ), indent );
		}
	}
	
	public String getFieldDescription(Property property){
		MetaAttribute c = property.getMetaAttribute( "field-description" );
		if ( c == null ) {
			return "";
		}
		else {
			return c2j.getMetaAsString( property, "field-description" );
		}		
	}

	/**
	 * Method getGetterSignature.
	 *
	 * @return String
	 */
	public String getGetterSignature(Property p) {
		String prefix = c2j.getJavaTypeName( p, false).equals( "boolean" ) ? "is" : "get";
		return prefix + beanCapitalize( p.getName() );
	}

	/**
	 * @param p
	 * @return foo -> Foo, FOo -> FOo
	 */
	public String getPropertyName(Property p) {
		return beanCapitalize( p.getName() );
	}


	// get the "opposite" collectionnae for a property. Currently a "hack" that just uses the same naming algorithm as in reveng, will fail on more general models!
	public String getCollectionNameFor(Property property) {
		String str = getPropertyName(property);
		return NameConverter.simplePluralize(str);
	}
	
	
	/**
	 * foo -> Foo
	 * FOo -> FOo
	 */
	static public String beanCapitalize(String fieldname) {
		if ( fieldname == null || fieldname.length() == 0 ) {
			return fieldname;
		}

		if ( fieldname.length() > 1 && Character.isUpperCase( fieldname.charAt( 1 ) ) ) {
			return fieldname;
		}
		char chars[] = fieldname.toCharArray();
		chars[0] = Character.toUpperCase( chars[0] );
		return new String( chars );
	}


	public boolean isComponent(Property property) {
		Value value = property.getValue();
		if ( value != null && value instanceof Component ) {
			return true;
		}
		else {
			return false;
		}
	}

	public String generateHashCode(Property property, String result, String thisName, boolean jdk5) {
		StringBuffer buf = new StringBuffer();
		if ( c2j.getMetaAsBool( property, "use-in-equals" ) ) {
			String javaTypeName = c2j.getJavaTypeName( property, jdk5, this );
			boolean isPrimitive = c2j.isPrimitive( javaTypeName );
			if ( isPrimitive ) {
				buf.append( result )
				.append( " = 37 * " )
				.append( result )
				.append( " + " );
				String thisValue = thisName + "." + getGetterSignature( property ) + "()";
				if("char".equals(javaTypeName)||"int".equals(javaTypeName)||"short".equals(javaTypeName)||"byte".equals(javaTypeName)) {
					buf.append( thisValue );
				} else if("boolean".equals(javaTypeName)) {
					buf.append("(" + thisValue + "?1:0)");
				} else {
					buf.append( "(int) ");
					buf.append( thisValue );
				}
				buf.append(";");
			}
			else {
				if(javaTypeName.endsWith("[]")) {
					if(jdk5) {
						buf.append( result )
						.append( " = 37 * " )
						.append( result )
						.append( " + " );
						buf.append( "( " )
						.append( getGetterSignature( property ) )
						.append( "() == null ? 0 : " + importType("java.util.Arrays") + ".hashCode(" )
						.append( thisName )
						.append( "." )
						.append( getGetterSignature( property ) )
						.append( "())" )
						.append( " )" )
						.append(";");						
					}
					else {
						buf.append(internalGenerateArrayHashcode(property, javaTypeName, result, thisName));
					}
				} else {
					buf.append( result )
					.append( " = 37 * " )
					.append( result )
					.append( " + " );
					buf.append( "( " )
					.append( getGetterSignature( property ) )
					.append( "() == null ? 0 : " )
					.append( thisName )
					.append( "." )
					.append( getGetterSignature( property ) )
					.append( "()" )
					.append( ".hashCode()" )
					.append( " )" )
					.append(";");
				}
			}
		}
		return buf.toString();
	}


	private String internalGenerateArrayHashcode(Property property, String javaTypeName, String result, String thisName)
	{
		StringBuffer buf = new StringBuffer();

		String propertyHashVarName = property.getName() + "Hashcode";
		String propertyArrayName = property.getName() + "Property";

//		int propertyHash = 0;
		buf.append( "int ")
		.append( propertyHashVarName )
		.append( " = 0;\n" );

//		type[] proterty = getProperty();
		buf.append( "         " )
		.append( javaTypeName )
		.append( " " )
		.append( propertyArrayName )
		.append( " = " )
		.append( thisName )
		.append( "." )
		.append( getGetterSignature( property ) )
		.append( "();\n");

//		if(property != null) {
		buf.append( "         if(" )
		.append( propertyArrayName )
		.append( " != null) {\n" );

//		propertyHash = 1;
		buf.append( "             " )
		.append( propertyHashVarName )
		.append( " = 1;\n" );

//		for (int i=0; i<property.length; i++)
		javaTypeName.replaceAll("\\[\\]", "");
		buf.append( "             for (int i=0; i<" )
		.append( propertyArrayName )
		.append( ".length; i++) {\n" );

		if(javaTypeName.startsWith("long")) {
//			int elementHash = (int)(propertyArray[i] ^ (propertyArray[i] >>> 32));
			buf.append( "                 int elementHash = (int)(" )
			.append( propertyArrayName )
			.append( "[i] ^ (" )
			.append( propertyArrayName )
			.append( "[i] >>> 32));\n" );

//			propertyHash = 37 * propertyHash + elementHash;
			buf.append( "                 " )
			.append( propertyHashVarName )
			.append( " = 37 * " )
			.append( propertyHashVarName )
			.append( " + elementHash;\n" );
		} else if(javaTypeName.startsWith("boolean")) {
//			propertyHash = 37 * propertyHash + (propertyArray[i] ? 1231 : 1237);
			buf.append( "                 " )
			.append( propertyHashVarName )
			.append( " = 37 * " )
			.append( propertyHashVarName )
			.append( " + (" )
			.append( propertyArrayName )
			.append( "[i] ? 1231 : 1237);\n" );
		} else if(javaTypeName.startsWith("float")) {
//			propertyHash = 37 * propertyHash + Float.floatToIntBits(propertyArray[i]);
			buf.append( "                 " )
			.append( propertyHashVarName )
			.append( " = 37 * " )
			.append( propertyHashVarName )
			.append( " + Float.floatToIntBits(" )
			.append( propertyArrayName )
			.append( "[i]);\n" );
		} else if(javaTypeName.startsWith("double")) {
//			long bits = Double.doubleToLongBits(propertyArray[i]);
			buf.append( "                 long bits = Double.doubleToLongBits(" )
			.append( propertyArrayName )
			.append( "[i]);\n" );

//			propertyHash = 37 * propertyHash + (int)(bits ^ (bits >>> 32));
			buf.append( "                 " )
			.append( propertyHashVarName )
			.append( " = 37 * " )
			.append( propertyHashVarName )
			.append( " + (int)(bits ^ (bits >>> 32));\n" );
		} else if(javaTypeName.startsWith("int")
				|| javaTypeName.startsWith("short")
				|| javaTypeName.startsWith("char")
				|| javaTypeName.startsWith("byte")) {
//			propertyHash = 37 * propertyHash + propertyArray[i];
			buf.append( "                 " )
			.append( propertyHashVarName )
			.append( " = 37 * " )
			.append( propertyHashVarName )
			.append( " + " )
			.append( propertyArrayName )
			.append( "[i];\n" );
		} else {// Object[]
//			propertyHash = 37 * propertyHash + propertyArray[i].hashCode();
			buf.append( "                 " )
			.append( propertyHashVarName )
			.append( " = 37 * " )
			.append( propertyHashVarName )
			.append( " + " )
			.append( propertyArrayName )
			.append( "[i].hashCode();\n" );
		}

		buf.append( "             }\n" );
		buf.append( "         }\n\n" );

//		result = 37 * result + arrayHashcode;
		buf.append( "         " )
		.append( result )
		.append( " = 37 * " )
		.append( result )
		.append( " + " )
		.append( propertyHashVarName )
		.append( ";\n" );

		return buf.toString();
	}


	public String getFieldModifiers(Property property) {
		return getModifiers( property, "scope-field", "private" );
	}

	public String getPropertyGetModifiers(Property property) {
		return getModifiers( property, "scope-get", "public" );
	}

	public String getPropertySetModifiers(Property property) {
		return getModifiers( property, "scope-set", "public" );
	}

	//TODO defaultModifiers
	private String getModifiers(Property property, String modifiername, String defaultModifiers) {
		MetaAttribute override = property.getMetaAttribute( modifiername );
		if ( override != null ) {
			return MetaAttributeHelper.getMetaAsString( override );
		}
		else {
			return defaultModifiers;
		}
	}

	protected boolean isRequiredInConstructor(Property field) {
		if(hasMetaAttribute(field, "default-value")) {
			return false;
		}
		if(field.getValue()!=null) {			
			if (!(field.isOptional() || field.getValue().isNullable()) && (field.getValueGeneratorCreator() == null )) {				
				return true;
			} else if (field.getValue() instanceof Component) {
				Component c = (Component) field.getValue();
				Iterator<?> it = c.getProperties().iterator();
				while ( it.hasNext() ) {
					Property prop = (Property) it.next();
					if(isRequiredInConstructor(prop)) {
						return true;
					}
				}
			}
		}
		
		return false;
	}

	public boolean needsMinimalConstructor() {
		List<Property> propClosure = getPropertyClosureForMinimalConstructor();
		if(propClosure.isEmpty()) return false; // minimal=default
		if(propClosure.equals(getPropertyClosureForFullConstructor())) return false; // minimal=full
		return true;
	}

	public boolean needsFullConstructor() {
		return !getPropertyClosureForFullConstructor().isEmpty();		
	}
	
	public String getJavaTypeName(Property p, boolean useGenerics) {
		return c2j.getJavaTypeName(p, useGenerics, this);
	}
	
	static private class DefaultInitializor {
		
		private final String type;
		private final boolean initToZero;
		
		public DefaultInitializor(String type, boolean initToZero) {
			this.type = type;
			this.initToZero = initToZero;					
		}
		
		public String getDefaultValue(String comparator, String genericDeclaration, ImportContext importContext) {
			StringBuffer val = new StringBuffer("new " + importContext.importType(type));
			if(genericDeclaration!=null) {
				val.append(genericDeclaration);
			}
			
			val.append("(");
			if(comparator!=null) {
				val.append("new ");
				val.append(importContext.importType(comparator));
				val.append("()");
				if(initToZero) val.append(",");
			}
			if(initToZero) {
				val.append("0");
			}
			val.append(")");
			return val.toString();
		}
		
	}
	
	static Map<String, DefaultInitializor> defaultInitializors = new HashMap<String, DefaultInitializor>();
	static {
		defaultInitializors.put("java.util.List", new DefaultInitializor("java.util.ArrayList", true));
		defaultInitializors.put("java.util.Map", new DefaultInitializor("java.util.HashMap", true));
		defaultInitializors.put("java.util.Set", new DefaultInitializor("java.util.HashSet",true));		
		defaultInitializors.put("java.util.SortedSet", new DefaultInitializor("java.util.TreeSet", false));
		defaultInitializors.put("java.util.SortedMap", new DefaultInitializor("java.util.TreeMap", false));
	}
	
	public boolean hasFieldInitializor(Property p, boolean useGenerics) {
		return getFieldInitialization(p, useGenerics)!=null;
	}
	
	public String getFieldInitialization(Property p, boolean useGenerics) {
		if(hasMetaAttribute(p, "default-value")) {
			return MetaAttributeHelper.getMetaAsString( p.getMetaAttribute( "default-value" ) );
		}
		if(c2j.getJavaTypeName(p, false)==null) {
			throw new IllegalArgumentException();
		} else if (p.getValue() instanceof Collection) {
			Collection col = (Collection) p.getValue();
			
			DefaultInitializor initialization = (DefaultInitializor) col.accept(new DefaultValueVisitor(true) {
			 
				public Object accept(Bag o) {
					return new DefaultInitializor("java.util.ArrayList", true);
				}
				
				public Object accept(org.hibernate.mapping.List o) {
					return new DefaultInitializor("java.util.ArrayList", true);
				}
				
				public Object accept(org.hibernate.mapping.Map o) {
					if(o.isSorted()) {
						return new DefaultInitializor("java.util.TreeMap", false);
					} else {
						return new DefaultInitializor("java.util.HashMap", true);
					}
				}
				
				public Object accept(IdentifierBag o) {
					return new DefaultInitializor("java.util.ArrayList", true);
				}
				
				public Object accept(Set o) {
					if(o.isSorted()) {
						return new DefaultInitializor("java.util.TreeSet", false);
					} else {
						return new DefaultInitializor("java.util.HashSet", true);
					}
				}
				
				
				public Object accept(PrimitiveArray o) {
					return null; // TODO: default init for arrays ?
				}
				
				public Object accept(Array o) {
					return null;// TODO: default init for arrays ?
				}
				
			});
						 
			if(initialization!=null) {
				String comparator = null;
				String decl = null;

				if(col.isSorted()) {
					comparator = col.getComparatorClassName();
				}

				if(useGenerics) {
					decl = c2j.getGenericCollectionDeclaration((Collection) p.getValue(), true, importContext);
				}
				return initialization.getDefaultValue(comparator, decl, this);
			} else {
				return null;
			}
		} else {
			return null;
		}
	}	
	
}
 
