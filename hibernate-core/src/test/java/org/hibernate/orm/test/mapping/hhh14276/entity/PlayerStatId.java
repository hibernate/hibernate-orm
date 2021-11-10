/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.mapping.hhh14276.entity;

import java.io.Serializable;

public class PlayerStatId implements Serializable {

	private Integer playerId;

	// nested composite PK @IdClass: named like relationship in entity class
	private ScoreId score;

	public PlayerStatId() {
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

	public ScoreId getScoreId() {
		return score;
	}

	public void setScoreId(ScoreId scoreId) {
		this.score = scoreId;
	}
}
