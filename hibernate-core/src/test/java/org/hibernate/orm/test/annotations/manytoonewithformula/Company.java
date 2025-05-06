/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.manytoonewithformula;

import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinFormula;

/**
 * @author Sharath Reddy
 */
@Entity
public class Company implements Serializable {

	private static final long serialVersionUID = 1L;
	private int id;
	private Person person;

	@Id @GeneratedValue
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}

	@ManyToOne
	@JoinColumnOrFormula(column=@JoinColumn(name="id", referencedColumnName="company_id", updatable=false, insertable=false))
	@JoinColumnOrFormula(formula=@JoinFormula(value="'T'", referencedColumnName="is_default"))
	public Person getDefaultContactPerson() {
		return person;
	}
	public void setDefaultContactPerson(Person person) {
		this.person = person;
	}

}
