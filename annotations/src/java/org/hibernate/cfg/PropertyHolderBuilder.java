//$Id$
package org.hibernate.cfg;

import java.util.Map;

import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.cfg.annotations.EntityBinder;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.PersistentClass;

/**
 * This factory is here ot build a PropertyHolder and prevent .mapping interface adding
 *
 * @author Emmanuel Bernard
 */
public final class PropertyHolderBuilder {
	private PropertyHolderBuilder() {
	}

	public static PropertyHolder buildPropertyHolder(
			XClass clazzToProcess,
			PersistentClass persistentClass,
			EntityBinder entityBinder,
			//Map<String, Join> joins,
			ExtendedMappings mappings
	) {
		return new ClassPropertyHolder( persistentClass, clazzToProcess, entityBinder, mappings );
	}

	/**
	 * build a component property holder
	 *
	 * @param component component to wrap
	 * @param path	  component path
	 * @param mappings
	 * @return PropertyHolder
	 */
	public static PropertyHolder buildPropertyHolder(
			Component component, String path, PropertyData inferredData, PropertyHolder parent,
			ExtendedMappings mappings
	) {
		return new ComponentPropertyHolder( component, path, inferredData, parent, mappings );
	}

	/**
	 * buid a property holder on top of a collection
	 */
	public static PropertyHolder buildPropertyHolder(
			Collection collection, String path, XClass clazzToProcess, XProperty property,
			PropertyHolder parentPropertyHolder, ExtendedMappings mappings
	) {
		return new CollectionPropertyHolder( collection, path, clazzToProcess, property, parentPropertyHolder, mappings );
	}

	/**
	 * must only be used on second level phases (<join> has to be settled already)
	 */
	public static PropertyHolder buildPropertyHolder(
			PersistentClass persistentClass, Map<String, Join> joins,
			ExtendedMappings mappings
	) {
		return new ClassPropertyHolder( persistentClass, null, joins, mappings );
	}
}
