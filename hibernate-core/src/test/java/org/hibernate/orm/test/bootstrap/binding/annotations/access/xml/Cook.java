/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.access.xml;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;


/**
 * @author Hardy Ferentschik
 */
@Entity
public class Cook {
	@Id
	@GeneratedValue
	private int id;

	private Knive favouriteKnife;

	public Knive getFavouriteKnife() {
		return favouriteKnife;
	}

	public void setFavouriteKnife(Knive favouriteKnife) {
		this.favouriteKnife = favouriteKnife;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
}
