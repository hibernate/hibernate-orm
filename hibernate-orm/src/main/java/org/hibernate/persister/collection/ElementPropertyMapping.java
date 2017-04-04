/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.collection;
import org.hibernate.MappingException;
import org.hibernate.QueryException;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.persister.entity.PropertyMapping;
import org.hibernate.type.Type;

/**
 * @author Gavin King
 */
public class ElementPropertyMapping implements PropertyMapping {

	private final String[] elementColumns;
	private final Type type;

	public ElementPropertyMapping(String[] elementColumns, Type type)
	throws MappingException {
		this.elementColumns = elementColumns;
		this.type = type;
	}

	public Type toType(String propertyName) throws QueryException {
		if ( propertyName==null || "id".equals(propertyName) ) {
			return type;
		}
		else {
			throw new QueryException("cannot dereference scalar collection element: " + propertyName);
		}
	}

	public String[] toColumns(String alias, String propertyName) throws QueryException {
		if (propertyName==null || "id".equals(propertyName) ) {
			return StringHelper.qualify( alias, elementColumns );
		}
		else {
			throw new QueryException("cannot dereference scalar collection element: " + propertyName);
		}
	}

	/**
	 * Given a property path, return the corresponding column name(s).
	 */
	public String[] toColumns(String propertyName) throws QueryException, UnsupportedOperationException {
		throw new UnsupportedOperationException( "References to collections must be define a SQL alias" );
	}

	public Type getType() {
		return type;
	}

}
