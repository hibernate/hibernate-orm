// $Id:$
package org.hibernate.test.annotations.derivedidentities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author Hardy Ferentschik
 */
@Entity
public class Employee {
	@Id
	@GeneratedValue
	long id;

	String name;

	public Employee() {
	}

	public Employee( String name) {
		this.name = name;
	}

	public long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setId(long id) {
		this.id = id;
	}
}


