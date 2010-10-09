//$Id$
package org.hibernate.ejb.test.xml;

import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.PreRemove;

/**
 * @author Emmanuel Bernard
 */
public class CounterListener {
	public static int insert;
	public static int update;
	public static int delete;

	@PrePersist
	public void increaseInsert(Object object) {
		insert++;
	}

	@PreUpdate
	public void increaseUpdate(Object object) {
		update++;
	}

	@PreRemove
	public void increaseDelete(Object object) {
		delete++;
	}

	public static void reset() {
		insert = 0;
		update = 0;
		delete = 0;
	}
}
