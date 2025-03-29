/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.hhh14276.entity;

import java.io.Serializable;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "\"Scores\"")
@IdClass(ScoreId.class)
public class Score implements Serializable {

	@Id
	@Column(name = "game_id")
	private Integer gameId;

	@Id
	@Column(name = "is_home")
	private Boolean home;

	@Basic(optional = false)
	@Column(name = "roster_id")
	private Integer rosterId;

	@Basic
	@Column(name = "final_score")
	private Integer finalScore;

	public Score() {
	}

	public Integer getGameId() {
		return gameId;
	}

	public void setGameId(Integer gameId) {
		this.gameId = gameId;
	}

	public Boolean getHome() {
		return home;
	}

	public void setHome(Boolean home) {
		this.home = home;
	}

	public Integer getRosterId() {
		return rosterId;
	}

	public void setRosterId(Integer rosterId) {
		this.rosterId = rosterId;
	}

	public Integer getFinalScore() {
		return finalScore;
	}

	public void setFinalScore(Integer finalScore) {
		this.finalScore = finalScore;
	}
}
