//$Id$
package org.hibernate.test.annotations.override;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Location {
	private String name;

	@Id
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
