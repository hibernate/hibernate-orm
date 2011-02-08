//$Id: Child.java 4378 2004-08-19 10:12:11Z oneovthafew $
package org.hibernate.test.onetomany;


/**
 * @author gavin
 */
public class Child {
	private Long id;
	private String name;
	private Parent parent;
	/**
	 * @return Returns the id.
	 */
	public Long getId() {
		return id;
	}
	/**
	 * @param id The id to set.
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
	/**
	 * @return Returns the parent.
	 */
	public Parent getParent() {
		return parent;
	}
	/**
	 * @param parent The parent to set.
	 */
	public void setParent(Parent parent) {
		this.parent = parent;
	}
}
