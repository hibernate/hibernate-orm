/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.onetomany;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

import org.hibernate.annotations.Immutable;

/**
 * @author Emmanuel Bernard
 */
@Entity
class City {
	private Integer id;
	private String name;
	private List<Street> streets;
	private List<Street> mainStreets;

	@Id
	@GeneratedValue
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

	@OneToMany(mappedBy = "city")
	@OrderBy("streetNameCopy, id")
	public synchronized List<Street> getStreets() {
		return streets;
	}

	public void setStreets(List<Street> streets) {
		this.streets = streets;
	}

	@OneToMany
	@JoinColumn(name = "mainstreetcity_id",
			foreignKey = @ForeignKey(name = "CITYSTR_FK"))
	@OrderBy
	@Immutable
	public List<Street> getMainStreets() {
		return mainStreets;
	}

	public void setMainStreets(List<Street> streets) {
		this.mainStreets = streets;
	}

	public void addMainStreet(Street street) {
		if ( mainStreets == null ) mainStreets = new ArrayList<>();
		mainStreets.add( street );
	}

}
