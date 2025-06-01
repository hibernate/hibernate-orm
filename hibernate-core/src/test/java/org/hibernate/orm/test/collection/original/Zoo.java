/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.original;
import java.util.ArrayList;
import java.util.List;

public class Zoo {
	long id;
	List<Animal> animals = new ArrayList<>();

	public long getId() {
		return id;
	}
	public void setId( long id ) {
		this.id = id;
	}
	public List<Animal> getAnimals() {
		return animals;
	}
	public void setAnimals(List<Animal> animals) {
		this.animals = animals;
	}


}
