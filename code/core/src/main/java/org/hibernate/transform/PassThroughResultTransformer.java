//$Id: PassThroughResultTransformer.java 5685 2005-02-12 07:19:50Z steveebersole $
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
