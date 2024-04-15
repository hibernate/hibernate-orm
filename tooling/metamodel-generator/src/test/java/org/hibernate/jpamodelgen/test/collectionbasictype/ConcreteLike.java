package org.hibernate.jpamodelgen.test.collectionbasictype;

import javax.persistence.Entity;

@Entity(name = "ConcreteLike")
public class ConcreteLike extends Like<ConcreteLike.Target> {

	@Override
	public Reference<Target> getObject() {
		return new Reference<>();
	}

	public static class Target implements Like.I1, Like.I2 {
	}
}
