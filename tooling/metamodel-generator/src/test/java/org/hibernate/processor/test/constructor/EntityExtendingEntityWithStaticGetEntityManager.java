package org.hibernate.processor.test.constructor;

import jakarta.persistence.Entity;

@Entity
public class EntityExtendingEntityWithStaticGetEntityManager extends EntityWithStaticGetEntityManager {
	private String otherName;

	public String getOtherName() {
		return otherName;
	}

	public void setOtherName(String otherName) {
		this.otherName = otherName;
	}
}
