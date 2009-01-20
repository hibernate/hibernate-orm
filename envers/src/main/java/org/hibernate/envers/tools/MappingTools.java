package org.hibernate.envers.tools;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class MappingTools {	
	/**
	 * @param componentName Name of the component, that is, name of the property in the entity that references the
	 * component.
	 * @return A prefix for properties in the given component.
	 */
	public static String createComponentPrefix(String componentName) {
		return componentName + "_";
	}
}
