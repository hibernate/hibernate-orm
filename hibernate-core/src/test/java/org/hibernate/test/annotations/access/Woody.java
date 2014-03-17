//$Id$
package org.hibernate.test.annotations.access;

import javax.persistence.MappedSuperclass;

import org.hibernate.annotations.AttributeAccessor;

/**
 * @author Emmanuel Bernard
 */
@MappedSuperclass
//@Access(AccessType.PROPERTY)
@AttributeAccessor("property")
public class Woody extends Thingy {
	private String color;
	private String name;
	public boolean isAlive; //shouldn't be persistent

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
