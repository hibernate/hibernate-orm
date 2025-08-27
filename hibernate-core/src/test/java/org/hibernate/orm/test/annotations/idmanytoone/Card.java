/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.idmanytoone;
import java.io.Serializable;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

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
