package org.hibernate.test.ops;


/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class Person {
	private Long id;
	private String name;
	private Address address;
	private PersonalDetails details;

	public Person() {
	}

	public Person(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	public PersonalDetails getDetails() {
		return details;
	}

	public void setDetails(PersonalDetails details) {
		this.details = details;
	}
}
