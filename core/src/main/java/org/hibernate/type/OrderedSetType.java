//$Id: OrderedSetType.java 10100 2006-07-10 16:31:09Z steve.ebersole@jboss.com $
package org.hibernate.type;

import java.util.LinkedHashSet;

/**
 * A specialization of the set type, with (resultset-based) ordering.
 */
public class OrderedSetType extends SetType {

	/**
	 * Constructs a set type capable of creating ordered sets of the given
	 * role.
	 *
	 * @param role The collection role name.
	 * @param propertyRef The property ref name.
	 * @param isEmbeddedInXML Is this collection to embed itself in xml
	 */
	public OrderedSetType(String role, String propertyRef, boolean isEmbeddedInXML) {
		super( role, propertyRef, isEmbeddedInXML );
	}

	/**
	 * {@inheritDoc}
	 */
	public Object instantiate(int anticipatedSize) {
		return new LinkedHashSet( anticipatedSize );
	}

}
