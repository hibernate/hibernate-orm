package org.hibernate.processor.test.collectionbasictype;

import jakarta.persistence.Entity;

@Entity(name = "ConcreteLike")
public class ConcreteLike extends Like<ConcreteLike.Target> {

	@Override
	public Reference<Target> getObject() {
		return new Reference<>();
	}

	public static class Target implements Like.I1, Like.I2 {
	}
}
