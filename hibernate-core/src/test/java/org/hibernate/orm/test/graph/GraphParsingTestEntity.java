/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.graph;

import java.util.Map;

import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@Entity
public class GraphParsingTestEntity {

	private String id;

	private String name;

	private String description;

	private GraphParsingTestEntity linkToOne;
	private GraphParsingTestEntity linkToOneLazy;

	private Map<GraphParsingTestEntity, GraphParsingTestEntity> map;

	@Id
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Basic
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@ManyToOne( fetch = FetchType.EAGER )
	public GraphParsingTestEntity getLinkToOne() {
		return linkToOne;
	}

	public void setLinkToOne(GraphParsingTestEntity linkToOne) {
		this.linkToOne = linkToOne;
	}

	@ManyToOne( fetch = FetchType.LAZY )
	public GraphParsingTestEntity getLinkToOneLazy() {
		return linkToOneLazy;
	}

	public void setLinkToOneLazy(GraphParsingTestEntity linkToOneLazy) {
		this.linkToOneLazy = linkToOneLazy;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	public Map<GraphParsingTestEntity, GraphParsingTestEntity> getMap() {
		return map;
	}

	public void setMap(Map<GraphParsingTestEntity, GraphParsingTestEntity> map) {
		this.map = map;
	}

	@Basic
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
