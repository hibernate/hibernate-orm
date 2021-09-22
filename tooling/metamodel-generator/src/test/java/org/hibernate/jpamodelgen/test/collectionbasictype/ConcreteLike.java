package org.hibernate.jpamodelgen.test.collectionbasictype;

import jakarta.persistence.Entity;

@Entity(name = "ConcreteLike")
public class ConcreteLike extends Like<ConcreteLike.Target> {

	@Override
	public Reference<Target> getObject() {
		return new Reference<>();
	}

	public static class Target implements I1, I2 {
	}
}
