package org.hibernate.test.component.cascading.toone;


/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class PersonalInfo {
	private Address homeAddress = new Address();

	public PersonalInfo() {
	}

	public PersonalInfo(Address homeAddress) {
		this.homeAddress = homeAddress;
	}

	public Address getHomeAddress() {
		return homeAddress;
	}

	public void setHomeAddress(Address homeAddress) {
		this.homeAddress = homeAddress;
	}
}
