/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.hhh14343.entity;

import java.io.Serializable;

public class NestedStatId implements Serializable
{
	private static final long serialVersionUID = 1L;

	private Integer period;

	private NestedPlayerStatId playerStat;

	public NestedStatId()
	{
	}

	public Integer getPeriod()
	{
		return period;
	}

	public void setPeriod(Integer period)
	{
		this.period = period;
	}

	public NestedPlayerStatId getPlayerStat()
	{
		return playerStat;
	}

	public void setPlayerStat(NestedPlayerStatId playerStat)
	{
		this.playerStat = playerStat;
	}
}
