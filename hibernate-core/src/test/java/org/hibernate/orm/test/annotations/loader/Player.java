/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.loader;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Entity
public class Player {
	private Long id;
	private String name;
	private Team team;

	public Player() {
	}

	public Player(Long id) {
		this( id, "Player #" + id );
	}

	public Player(Long id, String name) {
		this.id = id;
		this.name = name;
	}

	public Player(Long id, Team team) {
		this( id, "Player #" + id, team );
	}

	public Player(Long id, String name, Team team) {
		this.id = id;
		this.name = name;
		this.team = team;
	}

	@Id
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne(targetEntity = Team.class)
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "team_id")
	public Team getTeam() {
		return team;
	}

	public void setTeam(Team team) {
		this.team = team;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
