package org.hibernate.transform;

import java.lang.reflect.Constructor;
import java.util.List;

import org.hibernate.QueryException;

public class AliasToBeanConstructorResultTransformer implements ResultTransformer {

	private Constructor constructor;

	public AliasToBeanConstructorResultTransformer(Constructor constructor) {
		this.constructor = constructor;
	}
	
	public Object transformTuple(Object[] tuple, String[] aliases) {
		try {
			return constructor.newInstance( tuple );
		}
		catch ( Exception e ) {
			throw new QueryException( 
					"could not instantiate: " + 
					constructor.getDeclaringClass().getName(), 
					e );
		}
	}

	public List transformList(List collection) {
		return collection;
	}

	
}
