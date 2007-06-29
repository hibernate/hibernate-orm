//$Id$
package org.hibernate.test.onetoone.singletable;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Gavin King
 */
public class Org extends Entity {
	public Set addresses = new HashSet();
}
