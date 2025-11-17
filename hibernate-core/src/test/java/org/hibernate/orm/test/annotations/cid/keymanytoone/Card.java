/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cid.keymanytoone;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
@Entity
public class Card implements Serializable {
	@Id
	private String id;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "primaryKey.card")
	private Set<CardField> fields;

	String model;

	Card() {
		fields = new HashSet<>();
	}

	public Card(String id) {
		this();
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void addField(Card card, Key key) {
		fields.add(new CardField( card, key));
	}

	public Set<CardField> getFields() {
		return fields;
	}

	public void setFields(Set<CardField> fields) {
		this.fields = fields;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}
}
