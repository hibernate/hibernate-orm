package org.hibernate.ejb.test.metadata;

import javax.persistence.Entity;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Feline extends Animal {
	private String color;

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}
}
