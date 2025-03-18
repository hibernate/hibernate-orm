/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.collection.embeddable;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.envers.Audited;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@Audited
public class DarkCharacter implements Serializable {
	@Id
	private int id;

	@ElementCollection
	private Set<Name> names = new HashSet<Name>();

	private int kills;

	public DarkCharacter() {
	}

	public DarkCharacter(int id, int kills) {
		this.id = id;
		this.kills = kills;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof DarkCharacter) ) {
			return false;
		}

		DarkCharacter character = (DarkCharacter) o;

		if ( id != character.id ) {
			return false;
		}
		if ( kills != character.kills ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = id;
		result = 31 * result + kills;
		return result;
	}

	@Override
	public String toString() {
		return "DarkCharacter(id = " + id + ", kills = " + kills + ")";
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
}
