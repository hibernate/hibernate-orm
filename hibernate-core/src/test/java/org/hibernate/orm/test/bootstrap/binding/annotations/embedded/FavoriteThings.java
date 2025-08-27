/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.embedded;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Has collection of embeddable objects inside embeddable objects for testing HHH-4598
 */

@Entity
public class FavoriteThings {
	@Id
	int id;

	@Embedded
	InternetFavorites web;

	public InternetFavorites getWeb() {
		return web;
	}

	public void setWeb(InternetFavorites web) {
		this.web = web;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
}
