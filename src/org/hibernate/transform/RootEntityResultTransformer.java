//$Id$
package org.hibernate.transform;

import java.util.List;

/**
 * @author Gavin King
 */
public class RootEntityResultTransformer implements ResultTransformer {

	public Object transformTuple(Object[] tuple, String[] aliases) {
		return tuple[ tuple.length-1 ];
	}

	public List transformList(List collection) {
		return collection;
	}

}
