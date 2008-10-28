package org.hibernate.annotations.common.test.reflection.java.generics;

import java.io.Serializable;

/**
 * @author Davide Marchignoli
 * @author Paolo Perrotta
 */
public abstract class Grandpa<T, U> implements Serializable, Language<String> {

	Integer grandpaField;

	public T returnsGeneric() {
		return null;
	}

	// generic embedded value
	public Neighbour<U> getFriend() {
		return null;
	}
}
