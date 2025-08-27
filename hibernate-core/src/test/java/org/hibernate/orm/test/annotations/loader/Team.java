/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.loader;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityResult;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SqlResultSetMapping;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.SQLSelect;

import java.util.HashSet;
import java.util.Set;

@Entity
public class Team {
	private Long id;
	private String name;
	private Set<Player> players = new HashSet<>();

	public Team() {
	}

	public Team(Long id) {
		this( id, "Team #" + id );
	}

	public Team(Long id, String name) {
		this.id = id;
		this.name = name;
	}

	@Id
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@OneToMany(targetEntity = Player.class, mappedBy = "team", fetch = FetchType.EAGER)
	@Fetch(FetchMode.SELECT)
	@SQLSelect(sql = "select * from Player where team_id = ?1",
			resultSetMapping = @SqlResultSetMapping(name = "",
					entities = @EntityResult(entityClass = Player.class)))
	public Set<Player> getPlayers() {
		return players;
	}

	public void setPlayers(Set<Player> players) {
		this.players = players;
	}

	public void addPlayer(Player p) {
		players.add( p );
		p.setTeam( this );
	}
}
