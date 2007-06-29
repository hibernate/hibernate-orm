//$Id$
package org.hibernate.test.id;

/**
 * @author Emmanuel Bernard
 */
public class Car {
	private Long id;
	private String color;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}
}
