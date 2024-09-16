/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.hhh14343.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "\"Stats\"")
@IdClass(NestedStatId.class)
public class NestedStat implements Serializable
{
	private static final long serialVersionUID = 1L;

	@Id
	@Column
	private Integer period;

	@Id
	@ManyToOne(optional = false, fetch = FetchType.EAGER)
	@JoinColumn(name = "game_id", referencedColumnName = "game_id")
	@JoinColumn(name = "is_home", referencedColumnName = "is_home")
	@JoinColumn(name = "player_id", referencedColumnName = "player_id")
	private NestedPlayerStat playerStat;

	public NestedStat()
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

	public NestedPlayerStat getPlayerStat()
	{
		return playerStat;
	}

	public void setPlayerStat(NestedPlayerStat playerStat)
	{
		this.playerStat = playerStat;
	}
}
