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
