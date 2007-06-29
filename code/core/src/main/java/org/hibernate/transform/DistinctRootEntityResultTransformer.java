//$Id: DistinctRootEntityResultTransformer.java 3890 2004-06-03 16:31:32Z steveebersole $
package org.hibernate.transform;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Gavin King
 */
public class DistinctRootEntityResultTransformer implements ResultTransformer {

	private static final Log log = LogFactory.getLog(DistinctRootEntityResultTransformer.class);

	static final class Identity {
		final Object entity;
		Identity(Object entity) {
			this.entity = entity;
		}
		public boolean equals(Object other) {
			Identity that = (Identity) other;
			return entity==that.entity;
		}
		public int hashCode() {
			return System.identityHashCode(entity);
		}
	}

	public Object transformTuple(Object[] tuple, String[] aliases) {
		return tuple[ tuple.length-1 ];
	}

	public List transformList(List list) {
		List result = new ArrayList( list.size() );
		Set distinct = new HashSet();
		for ( int i=0; i<list.size(); i++ ) {
			Object entity = list.get(i);
			if ( distinct.add( new Identity(entity) ) ) {
				result.add(entity);
			}
		}
		if ( log.isDebugEnabled() ) log.debug(
			"transformed: " +
			list.size() + " rows to: " +
			result.size() + " distinct results"
		);
		return result;
	}

}
