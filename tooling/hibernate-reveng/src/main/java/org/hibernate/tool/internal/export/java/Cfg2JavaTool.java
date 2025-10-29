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

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.hibernate.boot.internal.NamedHqlQueryDefinitionImpl;
import org.hibernate.boot.query.NamedHqlQueryDefinition;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Array;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.MetaAttributable;
import org.hibernate.mapping.MetaAttribute;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Value;
import org.hibernate.tool.internal.export.hbm.Cfg2HbmTool;
import org.hibernate.tool.internal.reveng.util.EnhancedValue;
import org.hibernate.tool.internal.util.NameConverter;
import org.hibernate.tool.internal.util.StringUtil;
import org.hibernate.type.Type;
import org.hibernate.type.spi.TypeConfiguration;
import org.jboss.logging.Logger;

/**
 * Helper methods for javacode generation.
 * <p/>
 * 
 *
 * @author max
 */
public class Cfg2JavaTool {

	private static final Logger log = Logger.getLogger( Cfg2JavaTool.class );	
			
	public Cfg2JavaTool() {

	}

	public POJOClass getPOJOClass(Component comp) {		
		return new ComponentPOJOClass(comp, this);
	}
	
	public POJOClass getPOJOClass(PersistentClass comp) {		
		return new EntityPOJOClass(comp, this);
	}
	
	public String unqualify(String name) {
		return StringHelper.unqualify( name );
	}

	/**
	 * Returns all meta items as one large string.
	 *
	 */
	public String getMetaAsString(MetaAttributable pc, String attribute) {
		MetaAttribute c = pc.getMetaAttribute( attribute );

		return MetaAttributeHelper.getMetaAsString( c );
	}

	public boolean hasMetaAttribute(MetaAttributable pc, String attribute) {
		return pc.getMetaAttribute( attribute ) != null;
	}

	public String getMetaAsString(MetaAttributable pc, String attribute, String seperator) {
		return MetaAttributeHelper.getMetaAsString( pc.getMetaAttribute( attribute ), seperator );
	}

	public boolean getMetaAsBool(MetaAttributable ma, String attribute) {
		return getMetaAsBool( ma, attribute, false );
	}

	public boolean getMetaAsBool(MetaAttributable pc, String attribute, boolean defaultValue) {
		return MetaAttributeHelper.getMetaAsBool( pc.getMetaAttribute( attribute ), defaultValue );
	}



	/**
	 * Convert string into something that can be rendered nicely into a javadoc
	 * comment.
	 * Prefix each line with a star ('*').
	 */
	public String toJavaDoc(String string, int indent) {
		StringBuilder result = new StringBuilder();

		if ( string != null ) {
			String[] lines = StringUtil.split( string, "\n\r\f" );
			for ( int i = 0; i < lines.length ; i++ ) {
				String docline = " * " + lines[i];
				if ( i < lines.length - 1 ) docline += "\n";
				result.append( StringUtil.leftPad( docline, docline.length() + indent ) );
			}
		}

		return result.toString();
	}

	public String getClassModifiers(MetaAttributable pc) {
		String classModifiers = null;

		// Get scope (backwards compatibility)
		if ( pc.getMetaAttribute( "scope-class" ) != null ) {
			classModifiers = getMetaAsString( pc, "scope-class" ).trim();
		}

		// Get modifiers
		if ( pc.getMetaAttribute( "class-modifier" ) != null ) {
			classModifiers = getMetaAsString( pc, "class-modifier" ).trim();
		}
		return classModifiers == null ? "public" : classModifiers;
	}

	
	private String toName(Class<?> c) {

		if ( c.isArray() ) {
			Class<?> a = c.getComponentType();
			
			return a.getName() + "[]";
		}
		else {
			return c.getName();
		}
	}

	/**
	 * Returns the typename for a property, using generics if this is a Set type and useGenerics is set to true.
	 */
	public String getJavaTypeName(Property p, boolean useGenerics) {
		return getJavaTypeName(p, useGenerics, new NoopImportContext());
	}

	public String getJavaTypeName(Property p, boolean useGenerics, ImportContext importContext) {
		String overrideType = getMetaAsString( p, "property-type" );
		if ( !StringHelper.isEmpty( overrideType ) ) {
			String importType = importContext.importType(overrideType);			
			if ( useGenerics && !importType.contains("<")) {
				if ( p.getValue() instanceof Collection ) {
					String decl = getGenericCollectionDeclaration( (Collection) p.getValue(), importContext );
					return importType + decl;
				}
			}
			return importType;
		}
		else {
			String rawType = getRawTypeName( p, useGenerics, importContext );
			if(rawType==null) {
					throw new IllegalStateException("getJavaTypeName *must* return a value");				
			}
			return importContext.importType(rawType);
		}
	}
	
	private static final Map<String,String> PRIMITIVES = 
			new HashMap<String,String>();

	static {
		PRIMITIVES.put( "char", "Character" );
		PRIMITIVES.put( "byte", "Byte" );
		PRIMITIVES.put( "short", "Short" );
		PRIMITIVES.put( "int", "Integer" );
		PRIMITIVES.put( "long", "Long" );
		PRIMITIVES.put( "boolean", "Boolean" );
		PRIMITIVES.put( "float", "Float" );
		PRIMITIVES.put( "double", "Double" );
	}

	static public boolean isNonPrimitiveTypeName(String typeName) {
		return (!PRIMITIVES.containsKey( typeName ))
				&& new TypeConfiguration()
				.getBasicTypeRegistry()
				.getRegisteredType(typeName) != null;
	}

	private String getRawTypeName(Property p, boolean useGenerics, ImportContext importContext) {
		Value value = p.getValue();
		try {			
			
			if (value instanceof Array a) { // array has a string rep.inside.
                if ( a.isPrimitiveArray() ) {
					return toName( value.getType().getReturnedClass() );
				}
				else if (a.getElementClassName()!=null){
					return a.getElementClassName() + "[]";
				} else {
					return getJavaTypeName(a.getElement()) + "[]";
				}
			}

			if (value instanceof Component component) { // same for component.
                if(component.isDynamic()) return "java.util.Map";
				return component.getComponentClassName();
			}
			
			if ( useGenerics ) {
				if ( value instanceof Collection ) {
					String decl = getGenericCollectionDeclaration( (Collection) value, importContext );
					return getJavaTypeName(value) + decl;
				}
			}

			return getJavaTypeName( value);
		}
		catch (Exception e) {
			//e.printStackTrace();
			String msg = "Could not resolve type without exception for " + p + " Value: " + value;
			if ( value != null && value.isSimpleValue() ) {
				String typename = ( (SimpleValue) value ).getTypeName();
				log.warn( msg + ". Falling back to typename: " + typename );
				return typename;
			}
			else {
				throw new RuntimeException( msg, e );
			}
		}
	}

	public String getGenericCollectionDeclaration(Collection collection, ImportContext importContext) {
		Value element = collection.getElement();
		String elementType = importContext.importType(getJavaTypeName(element));
		String genericDecl = elementType;
		if(collection.isIndexed()) {
			IndexedCollection idxCol = (IndexedCollection) collection;
			if(!idxCol.isList()) {
				Value idxElement = idxCol.getIndex();
				String indexType = importContext.importType(getJavaTypeName(idxElement));
				genericDecl = indexType + "," + elementType;
			}
		}
        return "<" + genericDecl + ">";
	}
	
	public Properties getFilteredIdentifierGeneratorProperties(SimpleValue simpleValue) {
		Properties p = ((EnhancedValue)simpleValue).getIdentifierGeneratorProperties();
		return Cfg2HbmTool.getFilteredIdentifierGeneratorProperties(p, new Properties());
	}

	private String getJavaTypeName(Value value) {
		return (String) value.accept( new JavaTypeFromValueVisitor() );
	}

	public String asParameterList(Iterator<?> fields, boolean useGenerics, ImportContext ic) {
		StringBuilder buf = new StringBuilder();
		while ( fields.hasNext() ) {
			Property field = (Property)fields.next();
			buf.append( getJavaTypeName( field, useGenerics, ic ) )
					.append( " " )
					.append( keyWordCheck(field.getName()) );
			if ( fields.hasNext() ) {
				buf.append( ", " );
			}
		}
		return buf.toString();
	}

	/**
	 * @param fields iterator on Property elements.
	 * @return "name, number, ..." for a property list, usable for method calls.
	 *         <p/>
	 *         TODO: handle this in a template ?
	 */
	public String asArgumentList(Iterator<?> fields) {
		StringBuilder buf = new StringBuilder();
		while ( fields.hasNext() ) {
			Property field = (Property)fields.next();
			buf.append( keyWordCheck(field.getName()) );
			if ( fields.hasNext() ) {
				buf.append( ", " );
			}
		}
		return buf.toString();
	}

	/**
	 * @param clazz persistent class.
	 * @return "String name, int number, ..." for a property list, usable for method declarations.
	 *         <p/>
	 *         TODO: handle this in a template ?
	 */
	public String asNaturalIdParameterList(PersistentClass clazz) {
		StringBuilder buf = new StringBuilder();
		for (Property field : clazz.getRootClass().getProperties()) {
			if ( field.isNaturalIdentifier() ) {
				buf.append( getJavaTypeName( field, false ) ) 
						.append( " " )
						.append( field.getName() )
						.append( ", " );
			}
		}
		return buf.substring( 0, buf.length() - 2 );
	}

	public String asParameterList(List<Property> fields, boolean useGenerics, ImportContext ic) {
		return asParameterList( fields.iterator(), useGenerics, ic );
	}

	public String asArgumentList(List<Property> fields) {
		return asArgumentList( fields.iterator() );
	}
	
	public String asFinderArgumentList(Map<Object,Object> parameterTypes, ImportContext ctx) {
		StringBuilder buf = new StringBuilder();
		Iterator<Entry<Object,Object>> iter = parameterTypes.entrySet().iterator();
		while ( iter.hasNext() ) {
			Entry<Object,Object> entry = iter.next();
			String typename = null;
			Type type = null;
			if(entry.getValue() instanceof String) {
				try {
					type = new TypeConfiguration()
							.getBasicTypeRegistry()
							.getRegisteredType((String) entry.getValue());
				} catch(Throwable t) {
                    typename = (String) entry.getValue();
				}
			}
			
			if(type!=null) {
				typename = type.getReturnedClass().getName();
			}
			buf.append( ctx.importType( typename ))
					.append( " " )
					.append( entry.getKey() );
			if ( iter.hasNext() ) buf.append( ", " );
		}
		return buf.toString();
	}

	
	
	public boolean isPrimitive(String typeName) {
		return PRIMITIVES.containsKey(typeName);
	}
	
	public boolean isComponent(Property property) {
		return isComponent(property.getValue());
	}	
	
	public boolean isComponent(Value value) {
		return ( value instanceof Component );
	}	
	
	// TODO: should consult exporter/cfg2java tool for cached POJOEntities....or maybe not since they
	// have their own state...
	public Iterator<POJOClass> getPOJOIterator(
			final Iterator<PersistentClass> persistentClasses) {
		return new Iterator<POJOClass>() {		
			public POJOClass next() {
				return getPOJOClass((PersistentClass)persistentClasses.next());
			}		
			public boolean hasNext() {
				return persistentClasses.hasNext();
			}		
			public void remove() {
				persistentClasses.remove();
			}		
		};
	}


	public String simplePluralize(String str) {
		return NameConverter.simplePluralize(str);
	}
	
	public String keyWordCheck(String possibleKeyword) {
		if(NameConverter.isReservedJavaKeyword(possibleKeyword)) {
			possibleKeyword = possibleKeyword + "_";
		}
		return possibleKeyword;
	}	

	public boolean isArray(String typeName) {
		return typeName!=null && typeName.endsWith("[]");
	}
	
	public Map<?, ?> getParameterTypes(NamedHqlQueryDefinition<?> query) {
		Map<?, ?> result = null;
		try {
			Field field = NamedHqlQueryDefinitionImpl.class.getDeclaredField("parameterTypes");
			field.setAccessible(true);
			result = (Map<?, ?>)field.get(query);
			if (result == null) {
				result = new HashMap<>();
			}
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		return result;
	}
}
