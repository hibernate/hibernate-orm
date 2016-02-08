/**
 * 
 */
package org.hibernate.envers.test.integration.query.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
@Entity
public class Address {

	@Id
	@GeneratedValue
	private Long id;

	private String street;
	private int number;

	public Address() {

	}

	public Address(String street, int number) {
		this.street = street;
		this.number = number;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public int getNumber() {
		return number;
	}

	public void setNumber(int number) {
		this.number = number;
	}

}
