/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.factory.puUtil;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity(name = "NestedLegacyEntity")
@IdClass(NestedLegacyEntityPk.class)
public class NestedLegacyEntity {

	@Id
	@ManyToOne
	@JoinColumn(name = "legacyFk1", referencedColumnName = "primitivePk1")
	@JoinColumn(name = "legacyFk2", referencedColumnName = "primitivePk2")
	private LegacyEntity legacyEntity;

	@Id
	@ManyToOne
	@JoinColumn(name = "modernFk", referencedColumnName = "id")
	private ModernEntity modernEntity;

	public NestedLegacyEntity() {
	}

	public LegacyEntity getLegacyEntity() {
		return legacyEntity;
	}

	public void setLegacyEntity(LegacyEntity legacyEntity) {
		this.legacyEntity = legacyEntity;
	}

	public ModernEntity getModernEntity() {
		return modernEntity;
	}

	public void setModernEntity(ModernEntity modernEntity) {
		this.modernEntity = modernEntity;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		NestedLegacyEntity that = (NestedLegacyEntity) o;

		if (legacyEntity != null ? !legacyEntity.equals(that.legacyEntity) : that.legacyEntity != null) return false;
		return modernEntity != null ? modernEntity.equals(that.modernEntity) : that.modernEntity == null;

	}

	@Override
	public int hashCode() {
		int result = legacyEntity != null ? legacyEntity.hashCode() : 0;
		result = 31 * result + (modernEntity != null ? modernEntity.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "NestedLegacyEntity{" +
				"legacyEntity=" + legacyEntity +
				", modernEntity=" + modernEntity +
				'}';
	}
}
