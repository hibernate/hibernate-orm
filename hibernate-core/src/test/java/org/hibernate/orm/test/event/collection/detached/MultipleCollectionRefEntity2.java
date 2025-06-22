/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.event.collection.detached;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class MultipleCollectionRefEntity2 implements org.hibernate.orm.test.event.collection.Entity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID", length = 10)
	private Long id;

	@Column(name = "TEXT", length = 50, nullable = false)
	private String text;

	@ManyToOne
	@JoinColumn(name = "MCE_ID", nullable = false, insertable = false, updatable = false,
			foreignKey = @ForeignKey(name = "FK_RE2_MCE"))
	private MultipleCollectionEntity multipleCollectionEntity;

	@Column(name = "MCE_ID", insertable = false, updatable = false)
	private Long multipleCollectionEntityId;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public MultipleCollectionEntity getMultipleCollectionEntity() {
		return multipleCollectionEntity;
	}

	public void setMultipleCollectionEntity(
			MultipleCollectionEntity multipleCollectionEntity) {
		this.multipleCollectionEntity = multipleCollectionEntity;
	}

	public Long getMultipleCollectionEntityId() {
		return multipleCollectionEntityId;
	}

	public void setMultipleCollectionEntityId(Long multipleCollectionEntityId) {
		this.multipleCollectionEntityId = multipleCollectionEntityId;
	}

	@Override
	public String toString() {
		return "MultipleCollectionRefEntity2 [id=" + id + ", text=" + text
				+ ", multipleCollectionEntityId=" + multipleCollectionEntityId
				+ "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((text == null) ? 0 : text.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MultipleCollectionRefEntity2 other = (MultipleCollectionRefEntity2) obj;
		if (text == null) {
			if (other.text != null)
				return false;
		} else if (!text.equals(other.text))
			return false;
		return true;
	}


	public MultipleCollectionRefEntity2 deepCopy(MultipleCollectionEntity newRef) {
		MultipleCollectionRefEntity2 clone = new MultipleCollectionRefEntity2();
		clone.setText(this.text);
		clone.setId(this.id);
		clone.setMultipleCollectionEntity(newRef);
		clone.setMultipleCollectionEntityId(newRef.getId());
		return clone;
	}

}
