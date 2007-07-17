//$Id: Person.java 11486 2007-05-08 21:57:24Z steve.ebersole@jboss.com $
package org.hibernate.test.sql.hand;

/**
 * @author Gavin King
 */
public class Person {
	private long id;
	private String name;

	public Person(String name) {
		this.name = name;
	}
	
	public Person() {}
	
/**
	 * @return Returns the id.
	 */
	public long getId() {
		return id;
	}
	/**
	 * @param id The id to set.
	 */
	public void setId(long id) {
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
