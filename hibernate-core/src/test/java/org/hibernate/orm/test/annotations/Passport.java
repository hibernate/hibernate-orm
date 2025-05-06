/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations;
import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

/**
 * International passport
 *
 * @author Emmanuel Bernard
 */
@Entity
public class Passport implements Serializable {

	private Long id;
	private String number;
	private Customer owner;

	@Id
	public Long getId() {
		return id;
	}

	@Column(name = "passport_number")
	public String getNumber() {
		return number;
	}

	@OneToOne(mappedBy = "passport")
	public Customer getOwner() {
		return owner;
	}

	public void setId(Long long1) {
		id = long1;
	}

	public void setNumber(String string) {
		number = string;
	}

	public void setOwner(Customer customer) {
		owner = customer;
	}

}
