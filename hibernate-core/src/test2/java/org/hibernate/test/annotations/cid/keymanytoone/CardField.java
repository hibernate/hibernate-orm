/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.cid.keymanytoone;
import java.io.Serializable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
@Entity
public class CardField implements Serializable {

	@EmbeddedId
	private PrimaryKey primaryKey;

	CardField(Card card, Key key) {
		this.primaryKey = new PrimaryKey(card, key);
	}

	CardField() {
	}

	public PrimaryKey getPrimaryKey() {
		return primaryKey;
	}

	public void setPrimaryKey(PrimaryKey primaryKey) {
		this.primaryKey = primaryKey;
	}
}

