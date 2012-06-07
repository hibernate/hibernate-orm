//$Id$
package org.hibernate.test.annotations.tableperclass;
import javax.persistence.Entity;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Robot extends Machine {
	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
