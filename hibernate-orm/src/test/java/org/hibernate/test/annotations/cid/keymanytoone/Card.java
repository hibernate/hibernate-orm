/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.cid.keymanytoone;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

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

	public Card(String id) {
		this();
		this.id = id;

	}

	Card() {
		fields = new HashSet<CardField>();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void addField(Card card, Key key) {
		fields.add(new CardField(card, key));
	}

	public Set<CardField> getFields() {
		return fields;
	}

	public void setFields(Set<CardField> fields) {
		this.fields = fields;
	}
}
