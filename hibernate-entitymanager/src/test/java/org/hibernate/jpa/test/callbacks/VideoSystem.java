//$Id$
package org.hibernate.jpa.test.callbacks;
import javax.persistence.EntityListeners;
import javax.persistence.ExcludeSuperclassListeners;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

/**
 * @author Emmanuel Bernard
 */
@ExcludeSuperclassListeners
@EntityListeners({FirstOneListener.class, IncreaseListener.class})
@MappedSuperclass
public class VideoSystem extends CommunicationSystem {
	public transient int counter = 0;
	@PreUpdate
	public void increase() {
		isFirst = false;
		isLast = false;
		communication++;
	}

	@PrePersist
	public void prepareEntity() {
		counter++;
	}
}
