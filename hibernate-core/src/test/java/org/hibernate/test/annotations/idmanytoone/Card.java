/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.idmanytoone;
import java.io.Serializable;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Card {

	@Id
	private CardPrimaryKey primaryKey = new CardPrimaryKey();

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "primaryKey.card")
	private Set<CardField> fields;

	@ManyToOne
	private CardField mainCardField;

	@Embeddable
	public static class CardPrimaryKey implements Serializable {

		public CardPrimaryKey() {}

		@ManyToOne(optional = false)
		private Project project;

		public Project getProject() {
			return project;
		}

		public void setProject(Project project) {
			this.project = project;
		}

	}

	public Set<CardField> getFields() {
		return fields;
	}

	public void setFields(Set<CardField> fields) {
		this.fields = fields;
	}

	public CardPrimaryKey getPrimaryKey() {
		return primaryKey;
	}

	public void setPrimaryKey(CardPrimaryKey primaryKey) {
		this.primaryKey = primaryKey;
	}

	public CardField getMainCardField() {
		return mainCardField;
	}

	public void setMainCardField(CardField mainCardField) {
		this.mainCardField = mainCardField;
	}
}
