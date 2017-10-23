/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.test.annotations.entity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;


@Entity
public class Player {

	private int id;
	private String name;
	private SoccerTeam team;

	// For the world cup of one versus one matches, we have
	// teams with one player (1v1 team).
	private SoccerTeam oneVoneTeam;

	@Id
	@GeneratedValue
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}

	@Column(name="name")
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	@ManyToOne
	public SoccerTeam getTeam() {
		return team;
	}
	public void setTeam(SoccerTeam team) {
		this.team = team;
	}

	@OneToOne
	public SoccerTeam getOneVoneTeam() {
		return oneVoneTeam;
	}

	public void setOneVoneTeam(SoccerTeam oneVoneTeam) {
		this.oneVoneTeam = oneVoneTeam;
	}
}
