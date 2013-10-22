/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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



