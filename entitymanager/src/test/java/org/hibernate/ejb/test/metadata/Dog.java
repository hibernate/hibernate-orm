package org.hibernate.ejb.test.metadata;

import javax.persistence.Entity;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Dog extends Animal {
	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
