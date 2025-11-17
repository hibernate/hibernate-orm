/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.entity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;


@Entity
public class Player {

	private int id;
	private String name;
	private SoccerTeam team;

	// For the world cup of one versus one matches, we have
	// teams with one player (1v1 team).
	private SoccerTeam oneVoneTeam;

	@Id
	@GeneratedValue
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}

	@Column(name="name")
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	@ManyToOne
	public SoccerTeam getTeam() {
		return team;
	}
	public void setTeam(SoccerTeam team) {
		this.team = team;
	}

	@OneToOne
	public SoccerTeam getOneVoneTeam() {
		return oneVoneTeam;
	}

	public void setOneVoneTeam(SoccerTeam oneVoneTeam) {
		this.oneVoneTeam = oneVoneTeam;
	}
}
