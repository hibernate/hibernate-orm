//$Id: $
package org.hibernate.ejb.test.inheritance;

import javax.persistence.Entity;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Strawberry extends Fruit {
	private Long size;

	public Long getSize() {
		return size;
	}

	public void setSize(Long size) {
		this.size = size;
	}
}
