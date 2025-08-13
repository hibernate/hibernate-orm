/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.domain.userguide;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

//tag::hql-examples-domain-model-example[]
@Entity
public class Account {
	@Id
	@GeneratedValue
	long id;

	@ManyToOne
	Person owner;

	@OneToMany(mappedBy = "account")
	List<Payment> payments = new ArrayList<>();

	//Getters and setters are omitted for brevity

	//end::hql-examples-domain-model-example[]
	public List<Payment> getPayments() {
		return payments;
	}

	public Person getOwner() {
		return owner;
	}

	public void setOwner(Person owner) {
		this.owner = owner;
	}

//tag::hql-examples-domain-model-example[]
}
//end::hql-examples-domain-model-example[]
