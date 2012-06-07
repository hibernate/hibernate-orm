//$Id: Part.java 5725 2005-02-14 12:10:15Z oneovthafew $
package org.hibernate.test.orphan;
import java.io.Serializable;

/**
 * @author Gavin King
 */
public class Part implements Serializable {
	private String name;
	private String description;
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
}
