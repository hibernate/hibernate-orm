//$Id: Org.java 5686 2005-02-12 07:27:32Z steveebersole $
package org.hibernate.test.onetoone.singletable;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Gavin King
 */
public class Org extends Entity {
	public Set addresses = new HashSet();
}
