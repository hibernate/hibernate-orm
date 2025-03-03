/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cid.keymanytoone.association;

import java.io.Serializable;
import jakarta.persistence.Embeddable;
import jakarta.persistence.ManyToOne;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
@Embeddable
public class PrimaryKey implements Serializable {
	@ManyToOne(optional = false)
	private Card card;

	@ManyToOne(optional = false)
	private Key key;

	public PrimaryKey(Card card, Key key) {
		this.card = card;
		this.key = key;
	}

	PrimaryKey() {
	}

	public Card getCard() {
		return card;
	}

	public void setCard(Card card) {
		this.card = card;
	}

	public Key getKey() {
		return key;
	}

	public void setKey(Key key) {
		this.key = key;
	}
}
