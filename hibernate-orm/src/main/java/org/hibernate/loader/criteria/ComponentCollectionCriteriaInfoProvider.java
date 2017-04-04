/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.criteria;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.PropertyMapping;
import org.hibernate.type.CompositeType;
import org.hibernate.type.Type;

/**
 * @author David Mansfield
 */

class ComponentCollectionCriteriaInfoProvider implements CriteriaInfoProvider {
	private final QueryableCollection persister;
	private final Map<String, Type> subTypes = new HashMap<String, Type>();

	ComponentCollectionCriteriaInfoProvider(QueryableCollection persister) {
		this.persister = persister;
		if ( !persister.getElementType().isComponentType() ) {
			throw new IllegalArgumentException( "persister for role " + persister.getRole() + " is not a collection-of-component" );
		}

		CompositeType componentType = (CompositeType) persister.getElementType();
		String[] names = componentType.getPropertyNames();
		Type[] types = componentType.getSubtypes();

		for ( int i = 0; i < names.length; i++ ) {
			subTypes.put( names[i], types[i] );
		}

	}

	@Override
	public String getName() {
		return persister.getRole();
	}

	@Override
	public Serializable[] getSpaces() {
		return persister.getCollectionSpaces();
	}

	@Override
	public PropertyMapping getPropertyMapping() {
		return persister;
	}

	@Override
	public Type getType(String relativePath) {
		// TODO: can a component have a nested component? then we may need to do something more here...
		if ( relativePath.indexOf( '.' ) >= 0 ) {
			throw new IllegalArgumentException( "dotted paths not handled (yet?!) for collection-of-component" );
		}
		Type type = subTypes.get( relativePath );
		if ( type == null ) {
			throw new IllegalArgumentException( "property " + relativePath + " not found in component of collection " + getName() );
		}
		return type;
	}
}
