/*
 * Created on 2004-12-01
 */
package org.hibernate.tool.internal.export.common;

import java.util.Iterator;
import java.util.Map;

import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.tool.hbm2x.pojo.ComponentPOJOClass;
import org.hibernate.tool.hbm2x.pojo.POJOClass;
import org.hibernate.tool.internal.export.pojo.Cfg2JavaTool;
import org.hibernate.tool.internal.export.pojo.POJOExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author max and david
 */
public class ConfigurationNavigator {

	private static final Logger log = LoggerFactory.getLogger(POJOExporter.class);
	
	/**
	 * @param clazz
	 */
	public static void collectComponents(Map<String, Component> components, PersistentClass clazz) {
		Iterator<Property> iter = new Cfg2JavaTool().getPOJOClass(clazz).getAllPropertiesIterator();
		collectComponents( components, iter );		
	}

	public static void collectComponents(Map<String, Component> components, POJOClass clazz) {
		Iterator<Property> iter = clazz.getAllPropertiesIterator();
		collectComponents( components, iter );		
	}
	
	private static void collectComponents(Map<String, Component> components, Iterator<Property> iter) {
		while(iter.hasNext()) {
			Property property = iter.next();
			if (!"embedded".equals(property.getPropertyAccessorName()) && // HBX-267, embedded property for <properties> should not be generated as component. 
				property.getValue() instanceof Component) {
				Component comp = (Component) property.getValue();
				addComponent( components, comp );			
			} 
			else if (property.getValue() instanceof Collection) {
				// compisite-element in collection
				Collection collection = (Collection) property.getValue();				
				if ( collection.getElement() instanceof Component) {
					Component comp = (Component) collection.getElement();				
					addComponent(components, comp);				
				}
			}
		}
	}

	private static void addComponent(Map<String, Component> components, Component comp) {
		if(!comp.isDynamic()) {
			Component existing = (Component) components.put(
					comp.getComponentClassName(), 
					comp);		
			if(existing!=null) {
				log.warn("Component " + existing.getComponentClassName() + " found more than once! Will only generate the last found.");
			}
		} else {
			log.debug("dynamic-component found. Ignoring it as a component, but will collect any embedded components.");
		}	
		collectComponents( 
				components, 
				new ComponentPOJOClass(comp, new Cfg2JavaTool()).getAllPropertiesIterator());		
	}
	
}
