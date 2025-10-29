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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.hibernate.mapping.Component;
import org.hibernate.mapping.MetaAttribute;
import org.hibernate.mapping.Property;

import static org.hibernate.tool.internal.export.java.MetaAttributeConstants.EXTENDS;
import static org.hibernate.tool.internal.export.java.MetaAttributeConstants.IMPLEMENTS;

public class ComponentPOJOClass extends BasicPOJOClass {

	private final Component clazz;

	public ComponentPOJOClass(Component component, Cfg2JavaTool cfg) {
		super(component, cfg);
		this.clazz = component;
		init();
	}

	protected String getMappedClassName() {
		return clazz.getComponentClassName();
	}
	
	public String getExtends() {
		String extendz = "";

		if ( isInterface() ) {
			if ( clazz.getMetaAttribute( EXTENDS ) != null ) {
                extendz += getMetaAsString( EXTENDS, "," );
			}
		}
		else if ( clazz.getMetaAttribute( EXTENDS ) != null ) {
			extendz = getMetaAsString( EXTENDS, "," );
		}

		return extendz.isEmpty() ? null : extendz;
	}
	    
	public String getImplements() {
		List<String> interfaces = new ArrayList<String>();

		//	implement proxy, but NOT if the proxy is the class it self!
		// interfaces can't implement stuff
		if ( !isInterface() ) {
			MetaAttribute implementz = clazz.getMetaAttribute( IMPLEMENTS );
			if ( implementz != null ) {
                interfaces.addAll(implementz.getValues());
			}
			interfaces.add( Serializable.class.getName() ); // TODO: is this "nice" ? shouldn't it be a user choice ?
		}


		if (!interfaces.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			for ( Iterator<String> iter = interfaces.iterator(); iter.hasNext() ; ) {
				//sb.append(JavaTool.shortenType(iter.next().toString(), pc.getImports() ) );
				sb.append( iter.next() );
				if ( iter.hasNext() ) sb.append( "," );
			}
			return sb.toString();
		}
		else {
			return null;
		}
	}
	
	public Iterator<Property> getAllPropertiesIterator() {
		return clazz.getProperties().iterator();
	}

	public boolean isComponent() {
		return true;
	}
	
	public boolean hasIdentifierProperty() {
		return false;
	}

	public boolean needsAnnTableUniqueConstraints() {
		return false;
	}

	public String generateBasicAnnotation(Property property) {
		return "";
	}
	
	public String generateAnnIdGenerator() {
		return "";
	}

	public String generateAnnTableUniqueConstraint() {
		return "";
	}
	
	public Object getDecoratedObject() {
		return clazz;
	}
	
	public boolean isSubclass() {
		return false;
	}
	
	public List<Property> getPropertiesForFullConstructor() {
		List<Property> res = new ArrayList<Property>();
		
		Iterator<Property> iter = getAllPropertiesIterator();
		while(iter.hasNext()) {
			res.add(iter.next());
		}
		return res;
	}
	
	public List<Property> getPropertyClosureForFullConstructor() {
		return getPropertiesForFullConstructor();
	}
	
	public List<Property> getPropertyClosureForSuperclassFullConstructor() {
		return Collections.emptyList();
	}
	
	public List<Property> getPropertiesForMinimalConstructor() {
		List<Property> res = new ArrayList<Property>();
		Iterator<Property> iter = getAllPropertiesIterator();
		while(iter.hasNext()) {
			Property prop = (Property)iter.next();
			if(isRequiredInConstructor(prop)) {
				res.add(prop);
			}			
		}
		return res;
	}

	public List<Property> getPropertyClosureForMinimalConstructor() {
		return getPropertiesForMinimalConstructor();
	}

	public List<Property> getPropertyClosureForSuperclassMinimalConstructor() {
		return Collections.emptyList();
	}

	/* 
	 * @see org.hibernate.tool.hbm2x.pojo.POJOClass#getSuperClass()
	 */
	public POJOClass getSuperClass() {
		return null;
	}

	public String toString() {
		return "Component: " + (clazz==null?"<none>":clazz.getComponentClassName());
	}
	
	public Property getIdentifierProperty(){
		return null;
	}

   public boolean hasVersionProperty() {
	   return false;
   }
   
	/*
	 * @see org.hibernate.tool.hbm2x.pojo.POJOClass#getVersionProperty()
	 */
	public Property getVersionProperty()
	{
		return null;
	}
}
