//$Id: Paper.java 8048 2005-08-30 21:27:17Z epbernard $
package org.hibernate.test.stateless;

/**
 * @author Emmanuel Bernard
 */
public class Paper {
	private Integer id;
	private String color;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}
}
