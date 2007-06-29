//$Id: Person.java 4316 2004-08-14 13:12:03Z oneovthafew $
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
