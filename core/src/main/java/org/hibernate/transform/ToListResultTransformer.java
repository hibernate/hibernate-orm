package org.hibernate.transform;

import java.util.Arrays;
import java.util.List;

public class ToListResultTransformer implements ResultTransformer {

	public static final ResultTransformer INSTANCE = new ToListResultTransformer();

	private ToListResultTransformer() {}
	
	public Object transformTuple(Object[] tuple, String[] aliases) {
		return Arrays.asList(tuple);
	}

	public List transformList(List collection) {
		return collection;
	}

}
