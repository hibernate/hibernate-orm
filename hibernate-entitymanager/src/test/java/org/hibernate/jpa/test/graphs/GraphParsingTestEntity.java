/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.jpa.test.graph;

import java.util.Map;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

@Entity
public class GraphParsingTestEntity {

	private String id;

	private String name;

	private String description;

	private GraphParsingTestEntity linkToOne;

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

	@ManyToOne
	public GraphParsingTestEntity getLinkToOne() {
		return linkToOne;
	}

	public void setLinkToOne(GraphParsingTestEntity linkToOne) {
		this.linkToOne = linkToOne;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@ElementCollection(targetClass = GraphParsingTestEntity.class)
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