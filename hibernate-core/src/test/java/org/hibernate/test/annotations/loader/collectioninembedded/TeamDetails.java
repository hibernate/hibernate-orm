/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.loader.collectioninembedded;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Loader;

@Embeddable
public class TeamDetails {

	private Set<Player> players = new HashSet<Player>();

	@OneToMany(targetEntity = Player.class, mappedBy = "team", fetch = FetchType.EAGER)
	@Fetch(FetchMode.SELECT)
	@Loader(namedQuery = "loadByTeam")
	public Set<Player> getPlayers() {
		return players;
	}

	public void setPlayers(Set<Player> players) {
		this.players = players;
	}
	
}
