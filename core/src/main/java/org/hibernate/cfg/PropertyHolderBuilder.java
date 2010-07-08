/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
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
			ExtendedMappings mappings,
			Map<XClass, InheritanceState> inheritanceStatePerClass
	) {
		return new ClassPropertyHolder( persistentClass, clazzToProcess, entityBinder, mappings, inheritanceStatePerClass );
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
			ExtendedMappings mappings,
			Map<XClass, InheritanceState> inheritanceStatePerClass
	) {
		return new ClassPropertyHolder( persistentClass, null, joins, mappings, inheritanceStatePerClass );
	}
}
