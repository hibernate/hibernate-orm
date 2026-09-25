package org.hibernate.orm.test.onetoone.singletable;


/**
 * @author Gavin King
 */
public class Person extends Entity {
	public Address address;
	public Address mailingAddress;
}
