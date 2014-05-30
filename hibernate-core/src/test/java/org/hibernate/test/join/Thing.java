//$Id: $
package org.hibernate.test.join;


/**
 * @author Chris Jones
 */
public class Thing {

	private Long id;
	private String name;

	/**
	 * @return Returns the ID.
	 */
	public Long getId() {
		return id;
	}
	/**
	 * @param id The ID to set.
	 */
	public void setId(Long id) {
		this.id = id;
	}
	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}
}
