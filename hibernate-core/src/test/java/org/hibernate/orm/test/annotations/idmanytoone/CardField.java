/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.idmanytoone;
import java.io.Serializable;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class CardField {

	@Id
	private PrimaryKey primaryKey = new PrimaryKey();

	@ManyToOne
	private Card cardtmp;

	@Embeddable
	public static class PrimaryKey implements Serializable {

		@ManyToOne(optional = false)
		private Card card;

		@ManyToOne(optional = false)
		private CardKey key;

		public Card getCard() {
			return card;
		}

		public void setCard(Card card) {
			this.card = card;
		}

		public CardKey getKey() {
			return key;
		}

		public void setKey(CardKey key) {
			this.key = key;
		}
	}

	public Card getCardtmp() {
		return cardtmp;
	}

	public void setCardtmp(Card cardtmp) {
		this.cardtmp = cardtmp;
	}

	public PrimaryKey getPrimaryKey() {
		return primaryKey;
	}

	public void setPrimaryKey(PrimaryKey primaryKey) {
		this.primaryKey = primaryKey;
	}
}
