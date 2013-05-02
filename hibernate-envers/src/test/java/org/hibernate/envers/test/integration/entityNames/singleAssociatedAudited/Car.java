package org.hibernate.envers.test.integration.entityNames.singleAssociatedAudited;

import org.hibernate.envers.Audited;

/**
 * @author Hern&aacute;n Chanfreau
 */

@Audited
public class Car {

	private long id;

	private int number;

	private Person owner;

	private Person driver;

	public Car() {
	}

	public Car(int number, Person owner, Person driver) {
		this.number = number;
		this.owner = owner;
		this.driver = driver;
	}


	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Person getOwner() {
		return owner;
	}

	public void setOwner(Person owner) {
		this.owner = owner;
	}

	public int getNumber() {
		return number;
	}

	public void setNumber(int number) {
		this.number = number;
	}

	public Person getDriver() {
		return driver;
	}

	public void setDriver(Person driver) {
		this.driver = driver;
	}
}
