//$Id$
package org.hibernate.transform;

import java.util.List;

/**
 * @author max
 */
public class PassThroughResultTransformer implements ResultTransformer {

	public Object transformTuple(Object[] tuple, String[] aliases) {
		return tuple.length==1 ? tuple[0] : tuple;
	}

	public List transformList(List collection) {
		return collection;
	}

}
