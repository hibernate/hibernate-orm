//$Id$
package org.hibernate.jpa.test.callbacks;
import javax.persistence.PreUpdate;

/**
 * @author Emmanuel Bernard
 */
public class IncreaseListener {
	@PreUpdate
	public void increate(CommunicationSystem object) {
		object.communication++;
		object.isFirst = false;
		object.isLast = false;
	}
}
