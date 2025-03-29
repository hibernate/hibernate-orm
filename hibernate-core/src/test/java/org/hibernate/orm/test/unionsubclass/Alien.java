/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.unionsubclass;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Gavin King
 */
public class Alien extends Being {
	private String species;
	private Hive hive;
	private List hivemates = new ArrayList();
	/**
	 * @return Returns the species.
	 */
	public String getSpecies() {
		return species;
	}
	/**
	 * @param species The species to set.
	 */
	public void setSpecies(String species) {
		this.species = species;
	}
	public Hive getHive() {
		return hive;
	}
	public void setHive(Hive hive) {
		this.hive = hive;
	}
	public List getHivemates() {
		return hivemates;
	}
	public void setHivemates(List hivemates) {
		this.hivemates = hivemates;
	}
}
