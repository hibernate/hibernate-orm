/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.xmlmapped;

import java.util.List;

/**
 * @author Hardy Ferentschik
 */
public class Boy {
	private long id;

	private String name;

	private List<String> nickNames;

	private Superhero favoriteSuperhero;

	private List<Superhero> knowsHeroes;

	private List<Superhero> savedBy;

	public List<Superhero> getSavedBy() {
		return savedBy;
	}

	public void setSavedBy(List<Superhero> savedBy) {
		this.savedBy = savedBy;
	}

	public Superhero getFavoriteSuperhero() {
		return favoriteSuperhero;
	}

	public void setFavoriteSuperhero(Superhero favoriteSuperhero) {
		this.favoriteSuperhero = favoriteSuperhero;
	}

	public List<Superhero> getKnowsHeroes() {
		return knowsHeroes;
	}

	public void setKnowsHeroes(List<Superhero> knowsHeroes) {
		this.knowsHeroes = knowsHeroes;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getNickNames() {
		return nickNames;
	}

	public void setNickNames(List<String> nickNames) {
		this.nickNames = nickNames;
	}
}
