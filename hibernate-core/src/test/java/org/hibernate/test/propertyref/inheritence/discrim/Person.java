//$Id: Person.java 6029 2005-03-06 16:34:16Z oneovthafew $
package org.hibernate.test.propertyref.inheritence.discrim;



/**
 * @author gavin
 */
public class Person {
	private Long id;
	private String name;
	private String personId;
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
	public String getPersonId() {
		return personId;
	}
	
	public void setPersonId(String personId) {
		this.personId = personId;
	}
	
}
