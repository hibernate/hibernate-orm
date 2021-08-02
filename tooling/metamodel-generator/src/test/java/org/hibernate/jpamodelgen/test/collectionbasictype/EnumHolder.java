package org.hibernate.jpamodelgen.test.collectionbasictype;

import javax.persistence.Entity;

@Entity
public class EnumHolder {

	public <E extends Enum<E>> E getMyEnum() {
		return null;
	}
}
