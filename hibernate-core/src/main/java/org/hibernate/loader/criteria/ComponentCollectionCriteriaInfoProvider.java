/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Middleware LLC or third-party contributors as
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
 *
 */
package org.hibernate.loader.criteria;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.PropertyMapping;
import org.hibernate.type.ComponentType;
import org.hibernate.type.Type;

/**
 * @author David Mansfield
 */

class ComponentCollectionCriteriaInfoProvider implements CriteriaInfoProvider {
    QueryableCollection persister;
    Map /* <String,Type> */ subTypes = new HashMap /* <String,Type> */();

    ComponentCollectionCriteriaInfoProvider(QueryableCollection persister) {
	this.persister = persister;
	if (!persister.getElementType().isComponentType()) {
	    throw new IllegalArgumentException("persister for role "+persister.getRole()+" is not a collection-of-component");
	}

	ComponentType componentType = (ComponentType)persister.getElementType();
	String[] names = componentType.getPropertyNames();
	Type[] types = componentType.getSubtypes();

	for (int i = 0; i < names.length; i++) {
	    subTypes.put(names[i], types[i]);
	}

    }

    public String getName() {
	return persister.getRole();
    }

    public Serializable[] getSpaces() {
	return persister.getCollectionSpaces();
    }

    public PropertyMapping getPropertyMapping() {
	return (PropertyMapping)persister;
    }

    public Type getType(String relativePath) {
	// TODO: can a component have a nested component? then we may need to do something more here...
	if (relativePath.indexOf('.') >= 0) 
	    throw new IllegalArgumentException("dotted paths not handled (yet?!) for collection-of-component");

	Type type = (Type)subTypes.get(relativePath);
	
	if (type == null) 
	    throw new IllegalArgumentException("property "+relativePath+" not found in component of collection "+getName());
	
	return type;
    }
}
