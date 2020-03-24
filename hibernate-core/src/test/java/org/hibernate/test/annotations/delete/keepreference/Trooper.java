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
import javax.persistence.ManyToOne;

/**
 * @author Richard Bizik
 */
@Entity
@SQLDelete(sql = "UPDATE trooper SET deleted = true WHERE id = ?", keepReference = true)
@Where(clause = "deleted = false")
public class Trooper extends BaseEntity{
	private String code;
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	private DeathStar deathStar;

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public DeathStar getDeathStar() {
		return deathStar;
	}

	public void setDeathStar(DeathStar deathStar) {
		this.deathStar = deathStar;
	}
}
