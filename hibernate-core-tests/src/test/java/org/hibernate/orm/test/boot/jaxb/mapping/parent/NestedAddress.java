package org.hibernate.orm.test.boot.jaxb.mapping.parent;

public class NestedAddress {
	private CompositeChild owner;
	private String street;
	private String city;

	public CompositeChild getOwner() {
		return owner;
	}

	public void setOwner(CompositeChild owner) {
		this.owner = owner;
	}

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}
}
