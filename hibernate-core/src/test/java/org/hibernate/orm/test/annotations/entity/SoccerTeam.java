/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.entity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import org.hibernate.annotations.SQLRestriction;

@Entity
public class SoccerTeam {
	@Id
	@GeneratedValue
	private int id;

	String name;

	@OneToMany
	@SQLRestriction("activeLicense = true")
	private List<Doctor> physiologists = new ArrayList<Doctor>();

	@OneToMany(mappedBy="team",
		orphanRemoval=true,
		cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.DETACH})
	private Set<Player> players = new HashSet<Player>();

	@OneToOne(mappedBy="oneVoneTeam",
		orphanRemoval=true,
		cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.DETACH})
	private Player oneVonePlayer;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Set<Player> getPlayers() {
		return players;
	}

	public void addPlayer(Player val) {
		players.add(val);
		val.setTeam(this);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Player getOneVonePlayer() {
		return oneVonePlayer;
	}

	public void setOneVonePlayer(Player oneVonePlayer) {
		this.oneVonePlayer = oneVonePlayer;
	}

	public List<Doctor> getPhysiologists() {
		return physiologists;
	}

	public void setPhysiologists(List<Doctor> physiologists) {
		this.physiologists = physiologists;
	}



}
