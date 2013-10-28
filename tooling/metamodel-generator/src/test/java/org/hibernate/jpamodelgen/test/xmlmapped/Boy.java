/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpamodelgen.test.xmlmapped;

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



