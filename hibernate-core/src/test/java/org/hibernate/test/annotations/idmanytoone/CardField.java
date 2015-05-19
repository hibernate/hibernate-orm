/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.idmanytoone;
import java.io.Serializable;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

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

