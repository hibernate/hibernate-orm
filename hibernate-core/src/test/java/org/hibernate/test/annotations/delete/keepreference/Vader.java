/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.test.annotations.delete.keepreference;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToOne;

/**
 * @author Richard Bizik
 */
@Entity
@SQLDelete(sql = "UPDATE vader SET deleted = true WHERE id = ?", keepReference = true)
@Where(clause = "deleted = false")
public  class Vader extends BaseEntity{

	private String name = "Darth Vader";
	@OneToOne(optional = false, fetch = FetchType.LAZY, mappedBy = "vader")
	private DeathStar deathStar;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public DeathStar getDeathStar() {
		return deathStar;
	}

	public void setDeathStar(DeathStar deathStar) {
		this.deathStar = deathStar;
	}
}
