//$Id: OrderedSetType.java 10100 2006-07-10 16:31:09Z steve.ebersole@jboss.com $
package org.hibernate.type;

import org.hibernate.util.LinkedHashCollectionHelper;

public class OrderedSetType extends SetType {

	public OrderedSetType(String role, String propertyRef, boolean isEmbeddedInXML) {
		super( role, propertyRef, isEmbeddedInXML );
	}

	public Object instantiate(int anticipatedSize) {
		return LinkedHashCollectionHelper.createLinkedHashSet( anticipatedSize );
	}

}
