/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.hhh14276.entity;

import java.io.Serializable;

public class ScoreId implements Serializable {

	private Integer gameId;

	private Boolean home;

	public ScoreId() {
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
}
