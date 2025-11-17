/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.pc;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/**
 * @author Vlad Mihalcea
 */
//tag::pc-cascade-domain-model-example[]
@Entity
public class Phone {

	@Id
	private Long id;

	@Column(name = "`number`")
	private String number;

	@ManyToOne(fetch = FetchType.LAZY)
	private Person owner;

	//Getters and setters are omitted for brevity
//end::pc-cascade-domain-model-example[]

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	public Person getOwner() {
		return owner;
	}

	public void setOwner(Person owner) {
		this.owner = owner;
	}
//tag::pc-cascade-domain-model-example[]
}
//end::pc-cascade-domain-model-example[]
