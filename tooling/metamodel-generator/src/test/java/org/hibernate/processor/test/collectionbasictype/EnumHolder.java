package org.hibernate.processor.test.collectionbasictype;

import jakarta.persistence.Entity;

@Entity
public class EnumHolder {

	public <E extends Enum<E>> E getMyEnum() {
		return null;
	}
}
