package org.hibernate.test.legacy;

import java.io.Serializable;
import java.util.Comparator;

public class StringComparator implements Comparator, Serializable {

	public int compare(Object o1, Object o2) {
		return ( (String) o1 ).toLowerCase().compareTo( ( (String) o2 ).toLowerCase() );
	}

}
