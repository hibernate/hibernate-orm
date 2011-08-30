//$Id: Person.java 5686 2005-02-12 07:27:32Z steveebersole $
package org.hibernate.test.onetoone.joined;


/**
 * @author Gavin King
 */
public class Person extends Entity {
	public Address address;
	public Address mailingAddress;
}
