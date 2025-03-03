/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.collectionelement.indexedCollection;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CollectionId;
import org.hibernate.annotations.CollectionIdJdbcTypeCode;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;

/**
 * @author Emmanuel Bernard
 */
@Entity
@GenericGenerator(name="increment", strategy = "increment")
public class Sale {
	@Id @GeneratedValue private Integer id;
	@ElementCollection
	@JoinTable(
		name = "contact",
		joinColumns = @JoinColumn(name = "n_key_person"))
	@CollectionId( column = @Column(name = "n_key_contact"), generator = "increment" )
	@CollectionIdJdbcTypeCode( Types.BIGINT )
	private List<Contact> contacts = new ArrayList<Contact>();

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public List<Contact> getContacts() {
		return contacts;
	}

	public void setContacts(List<Contact> contacts) {
		this.contacts = contacts;
	}
}
