/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.hibernate.mapping.PersistentClass;

public class DynamicMapInstantiator implements Instantiator {
	public static final String KEY = "$type$";

	private final String roleName;
	private Set<String> isInstanceEntityNames = new HashSet<>();

	public DynamicMapInstantiator() {
		this.roleName = null;
	}

	public DynamicMapInstantiator(PersistentClass mappingInfo) {
		this.roleName = mappingInfo.getEntityName();
		isInstanceEntityNames.add( roleName );
		if ( mappingInfo.hasSubclasses() ) {
			Iterator itr = mappingInfo.getSubclassClosureIterator();
			while ( itr.hasNext() ) {
				final PersistentClass subclassInfo = ( PersistentClass ) itr.next();
				isInstanceEntityNames.add( subclassInfo.getEntityName() );
			}
		}
	}

	public final Object instantiate(Serializable id) {
		return instantiate();
	}

	public final Object instantiate() {
		Map map = generateMap();
		if ( roleName != null ) {
			map.put( KEY, roleName );
		}
		return map;
	}

	public final boolean isInstance(Object object) {
		if ( object instanceof Map ) {
			if ( roleName == null ) {
				return true;
			}
			final String type = (String) ( (Map) object ).get( KEY );
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
