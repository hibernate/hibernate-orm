/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.delete.keepreference;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Set;

/**
 * @author Richard Bizik
 */
@Entity
@SQLDelete(sql = "UPDATE deathstar SET deleted = true WHERE id = ?", keepReference = true)
@Where(clause = "deleted = false")
public class DeathStar extends BaseEntity {

	@NotNull
	@OneToOne(optional = false, cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private Vader vader;
	@OneToMany(cascade = CascadeType.MERGE, orphanRemoval = true, mappedBy = "deathStar",fetch = FetchType.EAGER)
	private Set<Trooper> troppers;
	@NotNull
	@OneToOne(optional = false, fetch = FetchType.EAGER)
	private Universe universe;

	public Vader getVader() {
		return vader;
	}

	public void setVader(Vader vader) {
		this.vader = vader;
	}

	public Set<Trooper> getTroppers() {
		return troppers;
	}

	public void setTroppers(Set<Trooper> troppers) {
		this.troppers = troppers;
	}

	public Universe getUniverse() {
		return universe;
	}

	public void setUniverse(Universe universe) {
		this.universe = universe;
	}
}
