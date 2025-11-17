/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.exception;
import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/**
 * @author Hardy Ferentschik
 */
@Entity
@SuppressWarnings("serial")
public class Musician implements Serializable {
	private Integer id;

	private String name;

	private Music favouriteMusic;

	@Id @GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@ManyToOne
	public Music getFavouriteMusic() {
		return favouriteMusic;
	}

	public void setFavouriteMusic(Music favouriteMusic) {
		this.favouriteMusic = favouriteMusic;
	}
}
