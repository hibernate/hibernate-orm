/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.cid.keymanytoone;
import java.io.Serializable;
import javax.persistence.Embeddable;
import javax.persistence.ManyToOne;

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
