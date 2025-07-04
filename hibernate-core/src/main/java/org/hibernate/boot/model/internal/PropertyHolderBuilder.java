/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

import java.util.Map;

import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.PropertyData;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.PersistentClass;

/**
 * This factory is here to build a PropertyHolder and prevent .mapping interface adding
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
			MetadataBuildingContext context,
			Map<XClass, InheritanceState> inheritanceStatePerClass) {
		return new ClassPropertyHolder(
				persistentClass,
				clazzToProcess,
				entityBinder,
				context,
				inheritanceStatePerClass
		);
	}

	/**
	 * build a component property holder
	 *
	 * @param component component to wrap
	 * @param path	  component path
	 *
	 * @return PropertyHolder
	 */
	public static PropertyHolder buildPropertyHolder(
			Component component,
			String path,
			PropertyData inferredData,
			PropertyHolder parent,
			MetadataBuildingContext context,
			Map<XClass, InheritanceState> inheritanceStatePerClass) {
		return new ComponentPropertyHolder( component, path, inferredData, parent, context, inheritanceStatePerClass );
	}

	/**
	 * build a property holder on top of a collection
	 */
	public static CollectionPropertyHolder buildPropertyHolder(
			Collection collection,
			String path,
			XClass clazzToProcess,
			XProperty property,
			PropertyHolder parentPropertyHolder,
			MetadataBuildingContext context) {
		return new CollectionPropertyHolder(
				collection,
				path,
				clazzToProcess,
				property,
				parentPropertyHolder,
				context
		);
	}

	/**
	 * May only be called during the second pass phase.
	 * (The join must have already been set.)
	 */
	public static PropertyHolder buildPropertyHolder(
			PersistentClass persistentClass,
			Map<String, Join> joins,
			MetadataBuildingContext context,
			Map<XClass, InheritanceState> inheritanceStatePerClass) {
		return new ClassPropertyHolder( persistentClass, null, joins, context, inheritanceStatePerClass );
	}
}
