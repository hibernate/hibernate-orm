/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.callbacks.jpa4;

import jakarta.persistence.PostInsert;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Steve Ebersole
 */
public class AnimalWatcher {
	public static List<Animal> creations = new ArrayList<>();

	@PostInsert
	public void postInsert(Animal animal) {
		creations.add(animal);
	}

	@PostInsert
	public void postInsert(Cat animal) {
		creations.add(animal);
	}

	@PostInsert
	public void postInsert(Dog animal) {
		creations.add(animal);
	}

}
