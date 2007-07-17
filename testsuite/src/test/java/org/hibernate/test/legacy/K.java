//$Id: K.java 7203 2005-06-19 02:01:05Z oneovthafew $
package org.hibernate.test.legacy;

import java.util.Set;

/**
 * @author Gavin King
 */
public class K {
	private Long id;
	private Set is;
	void setIs(Set is) {
		this.is = is;
	}
	Set getIs() {
		return is;
	}
}
