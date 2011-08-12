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
 *
 */
package org.hibernate.tuple;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.binding.EntityBinding;


public class DynamicMapInstantiator implements Instantiator {
	public static final String KEY = "$type$";

	private String entityName;
	private Set isInstanceEntityNames = new HashSet();

	public DynamicMapInstantiator() {
		this.entityName = null;
	}

	public DynamicMapInstantiator(PersistentClass mappingInfo) {
		this.entityName = mappingInfo.getEntityName();
		isInstanceEntityNames.add( entityName );
		if ( mappingInfo.hasSubclasses() ) {
			Iterator itr = mappingInfo.getSubclassClosureIterator();
			while ( itr.hasNext() ) {
				final PersistentClass subclassInfo = ( PersistentClass ) itr.next();
				isInstanceEntityNames.add( subclassInfo.getEntityName() );
			}
		}
	}

	public DynamicMapInstantiator(EntityBinding mappingInfo) {
		this.entityName = mappingInfo.getEntity().getName();
		isInstanceEntityNames.add( entityName );
		for ( EntityBinding subEntityBinding : mappingInfo.getPostOrderSubEntityBindingClosure() ) {
			isInstanceEntityNames.add( subEntityBinding.getEntity().getName() );
		}
	}

	public final Object instantiate(Serializable id) {
		return instantiate();
	}

	public final Object instantiate() {
		Map map = generateMap();
		if ( entityName!=null ) {
			map.put( KEY, entityName );
		}
		return map;
	}

	public final boolean isInstance(Object object) {
		if ( object instanceof Map ) {
			if ( entityName == null ) {
				return true;
			}
			String type = ( String ) ( ( Map ) object ).get( KEY );
			return type == null || isInstanceEntityNames.contains( type );
		}
		else {
			return false;
		}
	}

	protected Map generateMap() {
		return new HashMap();
	}
}