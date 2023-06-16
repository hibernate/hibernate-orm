/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.collection;

import org.hibernate.QueryException;
import org.hibernate.Remove;
import org.hibernate.persister.entity.PropertyMapping;
import org.hibernate.type.Type;

/**
 * @author Gavin King
 *
 * @deprecated Replaced by {@link org.hibernate.metamodel.mapping.CollectionPart}
 */
@Deprecated(since = "6", forRemoval = true)
@Remove
public class ElementPropertyMapping implements PropertyMapping {

	private final String[] elementColumns;
	private final Type type;

	public ElementPropertyMapping(String[] elementColumns, Type type) {
		this.elementColumns = elementColumns;
		this.type = type;
	}

	public Type toType(String propertyName) throws QueryException {
		if ( propertyName==null || "id".equals(propertyName) ) {
			return type;
		}
		else {
			throw new QueryException("Cannot dereference scalar collection element: " + propertyName);
		}
	}

	/**
	 * Given a property path, return the corresponding column name(s).
	 */
	public String[] toColumns(String propertyName) {
		throw new UnsupportedOperationException( "References to collections must define an SQL alias" );
	}

	public Type getType() {
		return type;
	}

}
