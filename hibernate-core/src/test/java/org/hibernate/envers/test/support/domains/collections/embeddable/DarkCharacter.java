/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.collections.embeddable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.envers.Audited;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@Audited
public class DarkCharacter {
	@Id
	private int id;

	@ElementCollection
	private Set<Name> names = new HashSet<>();

	private int kills;

	public DarkCharacter() {
	}

	public DarkCharacter(int id, int kills) {
		this.id = id;
		this.kills = kills;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getKills() {
		return kills;
	}

	public void setKills(int kills) {
		this.kills = kills;
	}

	public Set<Name> getNames() {
		return names;
	}

	public void setNames(Set<Name> names) {
		this.names = names;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		DarkCharacter that = (DarkCharacter) o;
		return id == that.id &&
				kills == that.kills;
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, kills );
	}

	@Override
	public String toString() {
		return "DarkCharacter{" +
				"id=" + id +
				", kills=" + kills +
				'}';
	}
}
