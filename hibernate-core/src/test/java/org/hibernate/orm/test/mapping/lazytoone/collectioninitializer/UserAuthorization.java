/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.lazytoone.collectioninitializer;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class UserAuthorization {
	@Id
	private Long id;

	@ManyToOne(optional = false)
	private User user;

	@ManyToOne(optional = false)
	private CostCenter costCenter;

	@Override
	public String toString() {
		return "UserAuthorization{" +
				"id='" + getId() + '\'' +
				'}';
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public CostCenter getCostCenter() {
		return costCenter;
	}

	public void setCostCenter(CostCenter costCenter) {
		this.costCenter = costCenter;
	}

}
