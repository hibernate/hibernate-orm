/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.stateless;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

/**
 * @author stliu
 */
@Entity
@NamedQuery(name = "org.hibernate.orm.test.stateless.Contact.contacts", query = "from Contact")
public class Contact {
	@Id
	@GeneratedValue
	private Integer id;
	public Integer getId() {
		return id;
	}
	public void setId( Integer id ) {
		this.id = id;
	}
	public Org getOrg() {
		return org;
	}
	public void setOrg( Org org ) {
		this.org = org;
	}
	@ManyToOne(fetch = FetchType.EAGER)
	@Fetch(FetchMode.JOIN)
	private Org org;

}
