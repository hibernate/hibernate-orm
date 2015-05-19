/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;
import java.util.LinkedHashMap;

/**
 * A specialization of the map type, with (resultset-based) ordering.
 */
public class OrderedMapType extends MapType {

	/**
	 * Constructs a map type capable of creating ordered maps of the given
	 * role.
	 *
	 * @param role The collection role name.
	 * @param propertyRef The property ref name.
	 * @param isEmbeddedInXML Is this collection to embed itself in xml
	 *
	 * @deprecated Use {@link #OrderedMapType(TypeFactory.TypeScope, String, String)} instead.
	 *  instead.
	 * See Jira issue: <a href="https://hibernate.onjira.com/browse/HHH-7771">HHH-7771</a>
	 */
	@Deprecated
	public OrderedMapType(TypeFactory.TypeScope typeScope, String role, String propertyRef, boolean isEmbeddedInXML) {
		super( typeScope, role, propertyRef, isEmbeddedInXML );
	}

	public OrderedMapType(TypeFactory.TypeScope typeScope, String role, String propertyRef) {
		super( typeScope, role, propertyRef );
	}

	@Override
	public Object instantiate(int anticipatedSize) {
		return anticipatedSize > 0
				? new LinkedHashMap( anticipatedSize )
				: new LinkedHashMap();
	}

}
