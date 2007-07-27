//$Id: OrderedMapType.java 10100 2006-07-10 16:31:09Z steve.ebersole@jboss.com $
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
	 */
	public OrderedMapType(String role, String propertyRef, boolean isEmbeddedInXML) {
		super( role, propertyRef, isEmbeddedInXML );
	}

	/**
	 * {@inheritDoc}
	 */
	public Object instantiate(int anticipatedSize) {
		return new LinkedHashMap( anticipatedSize );
	}

}
