package org.hibernate.annotations.common.test.reflection.java.generics;

import java.util.Set;

/**
 * @author Davide Marchignoli
 * @author Paolo Perrotta
 */
public class Neighbour<T> {

	public Set<T> embeddedProperty() {
		return null;
	}
}
