//$Id$
package org.hibernate.test.annotations;
import javax.persistence.Entity;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class IncorrectEntity {
	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
