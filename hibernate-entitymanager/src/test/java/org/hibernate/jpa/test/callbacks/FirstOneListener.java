//$Id$
package org.hibernate.jpa.test.callbacks;
import javax.persistence.PreUpdate;

/**
 * @author Emmanuel Bernard
 */
public class FirstOneListener {
	@PreUpdate
	public void firstOne(CommunicationSystem object) {
		if ( !object.isFirst ) throw new IllegalStateException();
		object.isFirst = true;
		object.isLast = false;
		object.communication++;
	}
}
