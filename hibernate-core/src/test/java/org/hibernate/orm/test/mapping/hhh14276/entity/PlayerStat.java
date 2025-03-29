/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.hhh14276.entity;

import java.io.Serializable;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "\"PlayerStats\"")
@IdClass(PlayerStatId.class)
public class PlayerStat implements Serializable {

	@Id
	@Column(name = "player_id")
	private Integer playerId;

	@Basic(optional = false)
	@Column(name = "jersey_nbr")
	private Integer jerseyNbr;

	@Id
	@ManyToOne(optional = false, fetch = FetchType.EAGER)
	@JoinColumn(name = "game_id", referencedColumnName = "game_id")
	@JoinColumn(name = "is_home", referencedColumnName = "is_home")
	private Score score;

	public PlayerStat() {
	}

	public Integer getGameId() {
		return score.getGameId();
	}

	public void setGameId(Integer gameId) {
		score.setGameId( gameId );
	}

	public Boolean getHome() {
		return score.getHome();
	}

	public void setHome(Boolean home) {
		score.setHome( home );
	}

	public Integer getPlayerId() {
		return playerId;
	}

	public void setPlayerId(Integer playerId) {
		this.playerId = playerId;
	}

	public Integer getJerseyNbr() {
		return jerseyNbr;
	}

	public void setJerseyNbr(Integer jerseyNbr) {
		this.jerseyNbr = jerseyNbr;
	}

	public Score getScore() {
		return score;
	}

	public void setScore(Score score) {
		this.score = score;
	}
}
