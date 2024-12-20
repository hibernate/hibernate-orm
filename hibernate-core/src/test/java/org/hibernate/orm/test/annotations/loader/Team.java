/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.loader;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityResult;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import jakarta.persistence.SqlResultSetMapping;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.SQLSelect;

@Entity
public class Team {
	private Long id;
	private Set<Player> players = new HashSet<Player>();

	@Id
	@GeneratedValue
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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
}
