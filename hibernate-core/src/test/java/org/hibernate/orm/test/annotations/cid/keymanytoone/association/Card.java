/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cid.keymanytoone.association;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/**
 * @author Andrea Boriero
 */
@Entity
public class Card {
	@Id
	private String id;

	@ManyToOne
	private CardField field;

	Card(){
	}

	public Card(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public CardField getField() {
		return field;
	}

	public void setField(CardField field) {
		this.field = field;
	}
}
